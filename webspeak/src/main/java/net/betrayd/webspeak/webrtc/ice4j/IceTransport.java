//Large portions of this class were translated from jitsi-VideoBridge
package net.betrayd.webspeak.webrtc.ice4j;

import net.betrayd.webspeak.event.Event;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.*;
import org.ice4j.util.Buffer;
import org.ice4j.util.BufferHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

public class IceTransport {
    public static final Logger LOGGER = LoggerFactory.getLogger(IceTransport.class);

    private final Agent iceAgent;
    private AtomicBoolean iceConnected = new AtomicBoolean(false);
    private Instant lastPingTime = Instant.EPOCH;
    @Nullable
    private Component iceComponent = null;

    private final Event.Invokable<IceConnectionState> iceConnectionStateChangedEvent = Event.create();
    private final Event.Invokable<Void> iceReadyEvent = Event.create();
    private final Event.Invokable<Buffer> packetReceivedEvent = Event.create();

    private final PropertyChangeListener iceStateChangedListener = this::iceStateChanged;
    private final PropertyChangeListener iceStreamChangedListener = this::iceStreamChanged;

    public IceTransport(){
        iceAgent = new Agent();
        iceAgent.addStateChangeListener(iceStateChangedListener);
    }

    /**
     * gets if this IceTransport is currently connected
     * @return connected
     */
    public boolean getIceConnected(){
        return iceConnected.get();
    }

    /**
     * gets the Instant that we last guarenteed a response from the connected RTC client
     * <p>This is useful for timeing out connections</p>
     * @return Last sucessful ping time
     */
    public Instant getLastPingTime(){
        return lastPingTime;
    }

    /**
     * Fired whenever the ICE connection state changes
     * <p>Not to be confused with onIceReady as they are fundamentally different</p>
     * @return the event attached to this object
     */
    public Event<IceConnectionState> onIceConnectionStateChanged(){
        return  iceConnectionStateChangedEvent;
    }

    /**
     * Fired when one valid candidate pair has been determined.
     * <p>In other words the connection is now open and data is sendable but the {@link IceTransport#onIceConnectionStateChanged} {@link IceConnectionState#CONNECTED} state still may not have fired yet.</p>
     * <p>We may not have the most optimal path yet</p>
     * @return
     */
    public Event<Void> oneIceReady(){
        return iceReadyEvent;
    }

    /**
     * Fired when this ice channel receives some kind of data.
     * <p>This data is fully garbage and really should only be used internally as we don't even know what webRTC stream this is attached to</p>
     * @return
     */
    public Event<Buffer> onPacketReceived(){
        return packetReceivedEvent;
    }

    @Nullable
    private IceMediaStream getCurrentIceMediaStream(){
        if(iceComponent == null){
            return null;
        }
        return iceComponent.getParentStream();
    }

    /**
     * Initializes the ice stream attached to this object and sets up listeners.
     * <p>This should be called before any other methods are ran on this object. Most likely right after creation.</p>
     * This is mainly an internal API
     * @throws IOException if an error occurs
     */
    public void init() throws IOException {
        IceMediaStream stream = iceAgent.createMediaStream("stream");
        stream.addPairChangeListener(iceStreamChangedListener);
        iceComponent = iceAgent.createComponent(stream, KeepAliveStrategy.SELECTED_ONLY, true);
        iceComponent.setBufferCallback(new BufferHandler() {
            @Override
            public void handleBuffer(@NotNull Buffer buffer) {
                packetReceivedEvent.invoke(buffer);
            }
        });
    }

    /**
     * Starts actual connection processes with the other client.
     * <p>This may be called multiple times without stopping the instance
     * as that may be required to re-establish additional data channels</p>
     * @param data an object representing the session description data used to start the ice handshake,
     *             can be generated from the sdp in the session description
     */
    public void start(IceStartData data){
        if(iceComponent == null){
            LOGGER.error("start called before iceComponent initialized. Use init First.");
            return;
        }

        IceMediaStream stream = getCurrentIceMediaStream();
        stream.setRemoteUfrag(data.ufrag());
        stream.setRemotePassword(data.password());

        boolean running = IceProcessingState.RUNNING == iceAgent.getState();

        if(running && data.iceCandidates().isEmpty()){
            //already connected, no new Ice candidates, so just return
            return;
        }

        int addedCount = 0;
        for(IceCandidateParser.ParsedCandidateSDP parsedCandidate : data.iceCandidates()){
            if(addIceCandidate(parsedCandidate)) {
                addedCount++;
            }
        }

        if(running){
            if (addedCount == 0) {
                // Effectively: ICE is running but no remote candidates exist (all ignored)
            } else {
                iceComponent.updateRemoteCandidates();
            }
        }else if(addedCount != 0){
            if (iceComponent.getRemoteCandidateCount() > 0) {
                //There is a message in jitsi-videoBridge at this equivilent point. The message is as follows:
                    /*Once again, because the ICE Agent does not support adding
                    candidates after the connectivity establishment has been started
                    and because multiple [Session Descriptions] may be used to send
                    the whole set of transport candidates from the remote peer to the
                    local peer, do not really start the connectivity establishment
                    until we have at least one remote candidate per ICE Component.*/
                iceAgent.startConnectivityEstablishment();
            }
        }
        else if (stream.getRemoteUfrag() != null && stream.getRemotePassword() != null) {
            /*
            The last message doesn't make sense. isn't it basically just stating that the software can't do this????
            Isn't that just what I'm doing?
            Well, either way I'm not changing it right now because this pipeline both terrifies me, and I'm tired
            */
            iceAgent.startConnectivityEstablishment();
        }
    }

    /**
     * Frees the underlying ice agent, removes listeners.
     * may need to change this method for events being fired to work
     * for our design philosophy
     */
    public void stop(){
        iceAgent.removeStateChangeListener(iceStateChangedListener);
        if(iceComponent != null){
            getCurrentIceMediaStream().removePairStateChangeListener(iceStreamChangedListener);
        }
        iceAgent.free();
    }

    /**
     * adds the remote ice candidate to this object's iceAgent.
     * <p>Used in the handshake process for establishing a connection</p>
     * @param parsed
     */
    public boolean addIceCandidate(IceCandidateParser.ParsedCandidateSDP parsed) {
            if(iceComponent == null){
                LOGGER.error("addIceCandidate called before iceComponent initialized. Use init First.");
                return false;
            }

            //Eventually this should probably be a full parser, but for now it works well enough as we only can have one stream/component
            Component component = iceComponent;

            TransportAddress transportAddress = new TransportAddress(parsed.ip(), parsed.port(), Transport.UDP);

            RemoteCandidate remoteCandidate = new RemoteCandidate(transportAddress, component, CandidateType.parse(parsed.type()), parsed.foundation(), parsed.priority(), null);

            if(iceAgent.getState() == IceProcessingState.RUNNING){
                component.addRemoteCandidate(remoteCandidate);
            }else{
                component.addRemoteCandidate(remoteCandidate);
            }
            return true;
    }

    private void iceStateChanged(PropertyChangeEvent event) {
        IceProcessingState oldState = (IceProcessingState) event.getOldValue();
        IceProcessingState newState = (IceProcessingState) event.getNewValue();

        if(newState == IceProcessingState.COMPLETED){
            if(iceConnected.compareAndSet(false, true)){
                iceConnectionStateChangedEvent.invoke(IceConnectionState.CONNECTED);
            }
        } else if(oldState == IceProcessingState.RUNNING && newState == IceProcessingState.TERMINATED){
            iceConnectionStateChangedEvent.invoke(IceConnectionState.STOPPED);
        }
        else if(newState == IceProcessingState.FAILED){
            iceConnectionStateChangedEvent.invoke(IceConnectionState.FAILED);
        }
    }

    private void iceStreamChanged(PropertyChangeEvent event){
        if(IceMediaStream.PROPERTY_PAIR_VALIDATED.equals(event.getPropertyName())){
            iceReadyEvent.invoke(null);
        }
        else if (IceMediaStream.PROPERTY_PAIR_CONSENT_FRESHNESS_CHANGED.equals(event.getPropertyName())){
            lastPingTime = Instant.now();
        }
    }

    public static enum IceConnectionState {
        CONNECTED,
        STOPPED,
        FAILED
    }
}
