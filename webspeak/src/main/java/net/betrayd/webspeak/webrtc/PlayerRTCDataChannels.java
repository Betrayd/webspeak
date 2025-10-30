package net.betrayd.webspeak.webrtc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.onvoid.webrtc.*;
import lombok.Setter;
import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.signaling.RTCConnection;
import net.betrayd.webspeak.webrtc.signaling.RTCSignalingMessages;
import net.betrayd.webspeak.webrtc.signaling.ServerSentRTCConnection;
import net.betrayd.webspeak.webrtc.signaling.SignalingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Supplier;

public class PlayerRTCDataChannels extends ServerSentRTCConnection implements SignalingChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRTCDataChannels.class);
    private static final Gson GSON = new Gson();

    private final Event.Invokable<Throwable> onSignalingMessageError = Event.create();

    private final RTCDataChannel reliableDataChannel;
    private final RTCDataChannel unreliableDataChannel;

    private final Event.Invokable<String> onMessageStr = Event.create();
    private final Event.Invokable<ByteBuffer> onMessageBin = Event.create();

    private final Supplier<Collection<RTCConnection>> rtcConnections;

    /**
     * The name to be used in logs for this connection. Normally should be the players sessionID
     */
    private final String logName;

    public PlayerRTCDataChannels(PeerConnectionFactory factory, RTCConfiguration config, int RTCIdentifier, SignalingChannel signaling, Supplier<Collection<RTCConnection>> rtcConnections, String logName) {
        super(factory, config, RTCIdentifier, signaling);
        this.rtcConnections = rtcConnections;
        this.logName = logName;

        RTCDataChannelInit reliableDataChannel = new RTCDataChannelInit();

        reliableDataChannel.id = 0;
        reliableDataChannel.priority = RTCPriorityType.HIGH;

        RTCDataChannelInit unreliableDataChannel = new RTCDataChannelInit();

        unreliableDataChannel.ordered = false;  // Messages will be delivered in order
        unreliableDataChannel.maxRetransmits = 0; // Don't retransmit
        unreliableDataChannel.id = 1;

        this.reliableDataChannel = getPeerConnection().createDataChannel("data-reliable", reliableDataChannel);
        this.unreliableDataChannel = getPeerConnection().createDataChannel("data-unreliable", unreliableDataChannel);

        this.reliableDataChannel.registerObserver(getObserver(this.reliableDataChannel));
        this.unreliableDataChannel.registerObserver(getObserver(this.unreliableDataChannel));

        createOffer();
    }

    @Override
    public void sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate) {

    }

    @Override
    public void sendDescription(RTCSignalingMessages.sessionDescription offer) {

    }

    public Event<String> onMessageStr(){
        return onMessageStr;
    }

    public Event<ByteBuffer> onMessageBin(){
        return onMessageBin;
    }

    public void sendReliable(String message){

    }

    public void sendUnreliable(String message){

    }

    private RTCDataChannelObserver getObserver(RTCDataChannel channel) {
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
                        LOGGER.info("Data channel with {} is being established", logName);
                        break;
                    case OPEN:
                        LOGGER.info("Data channel with {} is open and ready to use", logName);
                        break;
                    case CLOSING:
                        LOGGER.info("Data channel with {} is being closed", logName);
                        break;
                    case CLOSED:
                        LOGGER.info("Data channel with {} is closed", logName);
                        break;
                }
            }

            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                // Called when a message is received
                // IMPORTANT: The buffer data will be freed after this method returns,
                // so you must copy it if you need to use it asynchronously

                if(buffer.binary){
                    onMessageBin.invoke(buffer.data);
                    return;
                }
                String str = StandardCharsets.UTF_8.decode(buffer.data).toString();

                JsonObject obj = GSON.fromJson(str, JsonObject.class);

                if(obj != null){
                    String type = obj.get("type").getAsString();
                    switch (type) {
                        case RTCSignalingMessages.iceCandidate.TYPE ->
                                handleReceivedIceCandidate(rtcConnections.get(), GSON.fromJson(obj, RTCSignalingMessages.iceCandidate.class));
                        case RTCSignalingMessages.sessionDescription.TYPE ->
                                handleReceivedSessionDescription(rtcConnections.get(), GSON.fromJson(obj, RTCSignalingMessages.sessionDescription.class));
                    }
                }
                onMessageStr.invoke(str);
            }
        };
    }
}
