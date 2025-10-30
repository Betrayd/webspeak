package net.betrayd.webspeak.webrtc;
import dev.onvoid.webrtc.*;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.webrtc.signaling.BackendSignaling;
import net.betrayd.webspeak.webrtc.signaling.RTCConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A class for creating RTC connections to the clients
 */
public class RTCManagerCore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTCManagerCore.class);

    private final PeerConnectionFactory factory = new PeerConnectionFactory();

    private final RTCConfiguration config;
    private final ServerBackend serverBackend;
    private final WebSpeakServer server;

    public RTCManagerCore(RTCConfiguration config, ServerBackend serverBackend, WebSpeakServer server){
        this.config = config;
        this.serverBackend = serverBackend;
        this.server = server;

        serverBackend.getOnClientConnected().addListener((String sessionID) -> {
            WebSpeakPlayer player = server.getPlayers().get(sessionID);

            player.setSignaling(createRelaySignaling(player, serverBackend));
            player.setRtcConnection(createPeerConnection(player));
        });
    }

    public PlayerRTCDataChannels createPeerConnection(WebSpeakPlayer player){
        if(player.getSignaling() == null){
            return null;
        }
        return new PlayerRTCDataChannels(factory, config, RTCConnection.getNextIdentifier(), player.getSignaling(), List::of, player.getSessionId());
    }

    public BackendSignaling createRelaySignaling(WebSpeakPlayer player, ServerBackend backend){
        return new BackendSignaling(player.getSessionId(), backend, () -> {
            PlayerRTCDataChannels data = player.getRtcConnection();
            if(data != null){
                return List.of(data);
            }
            return  List.of();
        });
    }
}
