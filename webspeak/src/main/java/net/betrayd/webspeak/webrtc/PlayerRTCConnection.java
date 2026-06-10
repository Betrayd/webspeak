package net.betrayd.webspeak.webrtc;

import net.betrayd.webspeak.webrtc.signaling.SignalingServer;

public class PlayerRTCConnection{

    private final SignalingServer signalingServer;
    private final RTCConnection rtcConnection;

    public PlayerRTCConnection(RTCConnection rtcConnection, SignalingServer signalingServer) {
        this.signalingServer = signalingServer;
        this.rtcConnection = rtcConnection;
    }
}
