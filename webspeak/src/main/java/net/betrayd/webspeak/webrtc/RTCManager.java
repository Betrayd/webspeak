package net.betrayd.webspeak.webrtc;
import dev.onvoid.webrtc.*;
import net.betrayd.webspeak.ServerBackend;
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

    public RTCManager(RTCConfiguration config, ServerBackend serverBackend){
        this.config = config;
        this.serverBackend = serverBackend;
    }

    public RTCClientConnection createPeerConnection(String sessionID){
        return new RTCClientConnection(sessionID, factory, config, serverBackend);
    }
}
