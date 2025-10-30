package net.betrayd.webspeak.webrtc.signaling;

import net.betrayd.webspeak.event.Event;

import java.util.Collection;

//TODO: add some way to detect signaling message errors from the RTCConnection in case they want to do something
public interface SignalingChannel {
    void sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate);

    void sendDescription(RTCSignalingMessages.sessionDescription offer);

    default void handleReceivedIceCandidate(Collection<RTCConnection> con, RTCSignalingMessages.iceCandidate message){
        for(var connection : con){
            if(connection.getIdentifier() == message.getRTCIdentifier()){
                connection.receivedIceCandidate(message);
                return;
            }
        }
    }

    default void handleReceivedSessionDescription(Collection<RTCConnection> con, RTCSignalingMessages.sessionDescription message){
        for(var connection : con){
            if(connection.getIdentifier() == message.getRTCIdentifier()){
                connection.receivedSessionDescription(message);
                return;
            }
        }
    }
}
