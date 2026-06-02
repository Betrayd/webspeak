package net.betrayd.webspeak.webrtc.rtc_java.signaling;

import dev.onvoid.webrtc.PeerConnectionFactory;
import dev.onvoid.webrtc.RTCConfiguration;
import dev.onvoid.webrtc.RTCSdpType;
import net.betrayd.webspeak.webrtc.ice4j.RTCSignalingMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServerSentRTCConnection extends RTCConnection{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSentRTCConnection.class);

    public ServerSentRTCConnection(PeerConnectionFactory factory, RTCConfiguration config, String RTCIdentifier) {
        super(factory, config, RTCIdentifier);
    }

    @Override
    public HandledPeerConnectionObserver getPeerConnectionObserver() {
        return new HandledPeerConnectionObserver(this);
    }

    @Override
    public void receivedSessionDescription(RTCSignalingMessages.sessionDescription message) {
        RTCSdpType value = message.getSdpType();
        if(value != RTCSdpType.ANSWER){
            LOGGER.warn("Received non-answer session description from client: {}", "<TODO: add client>");
            return;
        }
        super.receivedSessionDescription(message);
    }

    public static class ServerSentPeerObserver extends HandledPeerConnectionObserver{

        public ServerSentPeerObserver(RTCConnection con) {
            super(con);
        }

        //make this call an event if it would be a fail to make it not
        /*@Override
        public void onConnectionChange(RTCPeerConnectionState state) {

        }*/
    }
}
