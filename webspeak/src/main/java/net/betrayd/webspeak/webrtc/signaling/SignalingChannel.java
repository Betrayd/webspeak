package net.betrayd.webspeak.webrtc.signaling;

import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.RTCConnection;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

//TODO: add some way to detect signaling message errors from the RTCConnection in case they want to do something
public interface SignalingChannel {
    CompletableFuture<?> sendDescription(RTCSignalingMessages.sessionDescription offer);

    CompletableFuture<?> sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate);

    Event<RTCSignalingMessages.sessionDescription> onReceivedSessionDescription();

    Event<RTCSignalingMessages.iceCandidate> onReceivedIceCandidate();
}

