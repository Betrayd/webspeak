package net.betrayd.webspeak.webrtc.rtc_java.signaling;

import net.betrayd.webspeak.webrtc.ice4j.RTCSignalingMessages;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

//TODO: add some way to detect signaling message errors from the RTCConnection in case they want to do something
public interface SignalingChannel {
    CompletableFuture<?> sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate);

    CompletableFuture<?> sendDescription(RTCSignalingMessages.sessionDescription offer);

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
