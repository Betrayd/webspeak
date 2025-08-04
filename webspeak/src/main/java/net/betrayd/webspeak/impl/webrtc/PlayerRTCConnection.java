package net.betrayd.webspeak.impl.webrtc;

import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.logging.Logging;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;

public class PlayerRTCConnection implements PeerConnectionObserver, Closeable {
    private static final PeerConnectionFactory FACTORY = new PeerConnectionFactory();

    static {
        Logging.addLogSink(Logging.Severity.VERBOSE, new Slf4jLogSink());
    }

    @Getter
    private RTCPeerConnection peerConnection;

    public PlayerRTCConnection() {
        RTCConfiguration config = new RTCConfiguration();

        // Add a STUN server to help with NAT traversal.
        RTCIceServer iceServer = new RTCIceServer();
        iceServer.urls.add("stun:stun.l.google.com:19302");
        config.iceServers.add(iceServer);

        peerConnection = FACTORY.createPeerConnection(config, this);
    }

    @Override
    public void onIceCandidate(RTCIceCandidate candidate) {

    }

    @Override
    public void close() {
        peerConnection.close();
    }
}
