package net.betrayd.webspeak.webrtc;
import dev.onvoid.webrtc.*;
import dev.onvoid.webrtc.media.audio.HeadlessAudioDeviceModule;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.WebSpeakServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class for creating RTC connections to the clients
 */
public class RTCManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTCManager.class);

    private final PeerConnectionFactory factory = new PeerConnectionFactory();

    private final RTCConfiguration config;
    private final ServerBackend serverBackend;
    private final WebSpeakServer server;

    public RTCManager(RTCConfiguration config, ServerBackend serverBackend, WebSpeakServer server){
        this.config = config;
        this.serverBackend = serverBackend;
        this.server = server;

        serverBackend.getOnClientConnected().addListener((String sessionID) -> {
            server.getPlayers().get(sessionID).setRtcConnection(createPeerConnection(sessionID));
        });
    }

    public RTCClientConnection createPeerConnection(String sessionID){
        return new RTCClientConnection(sessionID, factory, config, serverBackend);
    }
}
