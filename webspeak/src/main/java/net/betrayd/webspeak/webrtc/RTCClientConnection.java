package net.betrayd.webspeak.webrtc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.MediaStreamTrack;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the RTC connection for a single client
 */
//TODO: add timeout when connecting that causes a fatal error to be called
public class RTCClientConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTCClientConnection.class);

    private final String sessionID;
    private final RTCPeerConnection peerConnection;
    private final ServerBackend serverBackend;

    private final Event.Invokable<String> joinEvent = Event.create();
    //TODO: make possible to get from implementation of library
    private final Event.Invokable<RTCErrorEvent> onError = Event.create();
    private final Event.Invokable<RTCDataChannelBuffer> onMessage = Event.create();

    private static final Gson GSON = new Gson();

    private final RTCDataChannel reliableDataChannel;
    private final RTCDataChannel unreliableDataChannel;

    int connect = 0;

    private Map<String, RTCRtpSender> audioList = new HashMap<>();

    //TODO: replace this with something abstracted for implementations
    public MediaStreamTrack micTrack = null;

    protected RTCClientConnection(String sessionID, PeerConnectionFactory factory, RTCConfiguration config, ServerBackend serverBackend){
        this.sessionID = sessionID;
        this.serverBackend = serverBackend;
        this.peerConnection = factory.createPeerConnection(config,
                new PeerConnectionObserver() {
                    @Override
                    public void onIceCandidate(RTCIceCandidate rtcIceCandidate) {
                        // Send the ICE candidate to the remote peer via your signaling channel
                        try {
                            RTCSignalingMessages.iceCandidate contents = new RTCSignalingMessages.iceCandidate(rtcIceCandidate.sdpMid, rtcIceCandidate.sdpMLineIndex, rtcIceCandidate.sdp);
                            serverBackend.sendMessage(RTCClientConnection.this.sessionID, RTCSignalingMessages.write(contents)).whenComplete((s, e)->{
                                if(e!=null){
                                    LOGGER.error("{} - Failed to send iceCandidates", RTCClientConnection.this.sessionID, e);
                                    onError.invoke(new RTCErrorEvent(e,true));
                                }
                            });
                        }
                        catch(Exception e){
                            LOGGER.error("{} - Failed to send iceCandidates", RTCClientConnection.this.sessionID, e);
                            onError.invoke(new RTCErrorEvent(e,true));
                        }
                    }

                    @Override
                    public void onTrack(RTCRtpTransceiver transceiver){
                        MediaStreamTrack track = transceiver.getReceiver().getTrack();
                        String kind = track.getKind();

                        if(kind.equals(MediaStreamTrack.AUDIO_TRACK_KIND)){
                            micTrack = track;
                        }
                    }

                    @Override public void onDataChannel(RTCDataChannel dc) {
                        System.out.println(dc.getId() + ": DataChannel received: " + dc.getLabel());
                    }
                }
        );

        serverBackend.getOnMessageReceived().addListener(messageEvent -> {
            if(messageEvent.sessionId().equals(this.sessionID)){
                this.onSignalingMessageReceived(messageEvent.message());
            }
        });

        RTCDataChannelInit reliableDataChannel = new RTCDataChannelInit();

        reliableDataChannel.id = 0;
        reliableDataChannel.priority = RTCPriorityType.HIGH;

        RTCDataChannelInit unreliableDataChannel = new RTCDataChannelInit();

        unreliableDataChannel.ordered = false;  // Messages will be delivered in order
        unreliableDataChannel.maxRetransmits = 0; // Don't retransmit
        unreliableDataChannel.id = 1;

        this.reliableDataChannel = peerConnection.createDataChannel("data-reliable", reliableDataChannel);
        this.unreliableDataChannel = peerConnection.createDataChannel("data-unreliable", unreliableDataChannel);

        this.reliableDataChannel.registerObserver(getObserver(this.reliableDataChannel));
        this.unreliableDataChannel.registerObserver(getObserver(this.unreliableDataChannel));

        init();
    }

    protected void init(){
        RTCOfferOptions options = new RTCOfferOptions();

        peerConnection.createOffer(options, new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                // Set local description
                peerConnection.setLocalDescription(description, new SetSessionDescriptionObserver() {
                    @Override
                    public void onSuccess() {
                        // Send the offer to the remote peer via your signaling channel
                        try {
                            RTCSignalingMessages.sessionDescription contents = new RTCSignalingMessages.sessionDescription(description.sdpType.ordinal(), description.sdp);
                            serverBackend.sendMessage(sessionID, RTCSignalingMessages.write(contents)).whenComplete((s, e)->{
                                if(e!=null){
                                    LOGGER.error("Failed to send sessionDescription", e);
                                    onError.invoke(new RTCErrorEvent(e,true));
                                }
                            });
                        }
                        catch(Exception e){
                            LOGGER.error("Failed to send sessionDescription", e);
                            onError.invoke(new RTCErrorEvent(e, true));
                        }
                    }

                    @Override
                    public void onFailure(String error) {
                        LOGGER.error("Failed to set local description: {}", error);
                        onError.invoke(new RTCErrorEvent(new RTCConnecctionError(error),true));
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                LOGGER.error("Failed to create offer: {}", error);
                onError.invoke(new RTCErrorEvent(new RTCConnecctionError(error), true));
            }
        });
    }

    protected void onSignalingMessageReceived(String message){
        handleSignalingMessage(message);
    }

    private void handleSignalingMessage(String payload) {
        JsonObject obj = GSON.fromJson(payload, JsonObject.class);

        String type = obj.get("type").getAsString();

        switch (type) {
            case RTCSignalingMessages.iceCandidate.TYPE ->
                    handleReceivedIceCandidate(GSON.fromJson(obj, RTCSignalingMessages.iceCandidate.class));
            case RTCSignalingMessages.sessionDescription.TYPE ->
                    handleReceivedSessionDescription(GSON.fromJson(obj, RTCSignalingMessages.sessionDescription.class));
            //case RTCSignalingMessages.C2SrequestRTC.TYPE ->
            //        handleRequestRTC();
            default -> {
                LOGGER.warn("{} - Unknown signaling message type: {}", sessionID, type);
                onError.invoke(new RTCErrorEvent(new RTCConnecctionError("Unknown signaling message type: "+ type), false));
            }
        }
    }

    /*private void handleRequestRTC(){
        init();
    }*/

    private void handleReceivedIceCandidate(RTCSignalingMessages.iceCandidate message){
        try{
            RTCIceCandidate candidate = new RTCIceCandidate(message.sdpMid(), message.sdpMLineIndex(), message.sdp());
            peerConnection.addIceCandidate(candidate);
        }
        catch (Throwable e){
            LOGGER.error("Failed to handle received ice candidates", e);
        }
    }

    private void handleReceivedSessionDescription(RTCSignalingMessages.sessionDescription message){
        RTCSdpType value = null;
        if(RTCSdpType.values().length < message.RTCSdpType()){
            LOGGER.warn("{} - Received bad RTCSdpType for session description", sessionID);
            onError.invoke(new RTCErrorEvent(new RTCConnecctionError("Received bad RTCSdpType for session description"),true));
            return;
        }
        value = RTCSdpType.values()[message.RTCSdpType()];
        RTCSessionDescription remoteDescription = new RTCSessionDescription(value, message.sdp());
        peerConnection.setRemoteDescription(remoteDescription, new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                LOGGER.info("{} - Remote description set successfully", sessionID);
            }

            @Override
            public void onFailure(String error) {
                LOGGER.error("{} - Failed to set remote description: {}", sessionID, error);
                onError.invoke(new RTCErrorEvent(new RTCConnecctionError("Failed to set remote description" + error), true));
            }
        });
    }

    private RTCDataChannelObserver getObserver(RTCDataChannel channel){
        return new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(long previousAmount) {
                // Called when the buffered amount changes
                //we currently do not care about buffered data since these are packet channels
            }

            @Override
            public void onStateChange() {
                // Called when the data channel state changes
                RTCDataChannelState state = channel.getState();

                // Handle different states
                switch (state) {
                    case CONNECTING:
                        LOGGER.info("Data channel with {} is being established", sessionID);
                        break;
                    case OPEN:
                        LOGGER.info("Data channel with {} is open and ready to use", sessionID);

                        connect++;
                        if(connect == 2){
                            joinEvent.invoke(sessionID);
                        }
                        break;
                    case CLOSING:
                        LOGGER.info("Data channel with {} is being closed", sessionID);
                        break;
                    case CLOSED:
                        LOGGER.info("Data channel with {} is closed", sessionID);
                        break;
                }
            }

            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                // Called when a message is received
                // IMPORTANT: The buffer data will be freed after this method returns,
                // so you must copy it if you need to use it asynchronously

                onMessage.invoke(buffer);
            }
        };
    }

    public void addTrack(MediaStreamTrack track, String sourceID){
         audioList.put(sourceID, peerConnection.addTrack(track, List.of(sourceID)));
    }

    public boolean removeTrack(String sourceID){
        if(audioList.containsKey(sourceID)){
            peerConnection.removeTrack(audioList.get(sourceID));
            return true;
        }
        return false;
    }

    //probably should make all of these sends completable futures since it would match with the relay system, but this is fine for now.
    private boolean send(RTCDataChannel channel, Object data){
        if (channel.getState() == RTCDataChannelState.OPEN) {
            if(data instanceof ByteBuffer binary){
                RTCDataChannelBuffer binaryChannelBuffer = new RTCDataChannelBuffer(binary, true);

                try {
                    channel.send(binaryChannelBuffer);
                } catch (Exception e) {
                    LOGGER.error("Failed to send binary data to session: {}", sessionID, e);
                }
                return true;
            }
            if(data instanceof String string){
                ByteBuffer textBuffer = ByteBuffer.wrap(string.getBytes(StandardCharsets.UTF_8));
                RTCDataChannelBuffer textChannelBuffer = new RTCDataChannelBuffer(textBuffer, false);
                try {
                    channel.send(textChannelBuffer);
                } catch (Exception e) {
                    LOGGER.error("Failed to send text data to session: {}", sessionID, e);
                }
                return true;
            }
        }
        return false;
    }

    public boolean sendBinaryReliable(ByteBuffer binary){
        return send(reliableDataChannel, binary);
    }

    public boolean sendBinaryUnreliable(ByteBuffer binary){
        return send(unreliableDataChannel, binary);
    }

    public boolean sendStringReliable(String string){
        return send(reliableDataChannel, string);
    }

    public boolean sendStringUnreliable(String string){
        return send(unreliableDataChannel, string);
    }

    //TODO: implement this to prevent memory leaks
    public void closeAndCleanup(){

    }

    private record RTCErrorEvent(Throwable error, boolean fatal){

    }

    public static class RTCConnecctionError extends RuntimeException{

        public RTCConnecctionError(String error) {
            super(error);
        }
    }
}