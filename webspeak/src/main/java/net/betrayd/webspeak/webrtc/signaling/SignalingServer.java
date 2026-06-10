package net.betrayd.webspeak.webrtc.signaling;

import net.betrayd.webspeak.webrtc.RTCConnection;

import java.util.function.Consumer;

public class SignalingServer {

    public final RTCConnection rtcConnection;
    public final SignalingChannel fallback;

    public final Consumer<RTCSignalingMessages.sessionDescription> handleSessionDescriptionConsumer;
    public final Consumer<RTCSignalingMessages.iceCandidate> handleIceCandidateConsumer;

    public SignalingChannel signalingChannel;

    public SignalingServer(RTCConnection rtcConnection, SignalingChannel fallback) {
        this.rtcConnection = rtcConnection;
        this.fallback = fallback;
        handleSessionDescriptionConsumer = this::handleSessionDescription;
        handleIceCandidateConsumer = this::handleIceCandidate;

        fallback.onReceivedSessionDescription().addListener(handleSessionDescriptionConsumer);
        fallback.onReceivedIceCandidate().addListener(handleIceCandidateConsumer);
    }

    public void setSignalingChannel(SignalingChannel signalingChannel) {
        if(this.signalingChannel != null) {
            this.signalingChannel.onReceivedSessionDescription().removeListener(handleSessionDescriptionConsumer);
            this.signalingChannel.onReceivedIceCandidate().removeListener(handleIceCandidateConsumer);
        }
        if(signalingChannel != null) {
            this.signalingChannel = signalingChannel;
            signalingChannel.onReceivedSessionDescription().addListener(handleSessionDescriptionConsumer);
            signalingChannel.onReceivedIceCandidate().addListener(handleIceCandidateConsumer);
        }
    }

    public void sendSessionDescription(RTCSignalingMessages.sessionDescription sessionDescription){
        if(signalingChannel != null){
            signalingChannel.sendDescription(sessionDescription);
        }else{
            fallback.sendDescription(sessionDescription);
        }
    }

    public void sendIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate){
        if(signalingChannel != null){
            signalingChannel.sendIceCandidate(iceCandidate);
        }else{
            fallback.sendIceCandidate(iceCandidate);
        }
    }

    private void handleSessionDescription(RTCSignalingMessages.sessionDescription sessionDescription){
        rtcConnection.receivedSessionDescription(sessionDescription);
    }

    private void handleIceCandidate(RTCSignalingMessages.iceCandidate iceCandidate){
        rtcConnection.receivedIceCandidate(iceCandidate);
    }
}
