package net.betrayd.webspeak.v1.impl.jetty;

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.PlayerConnection;
import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.impl.net.S2CPacket;
import net.betrayd.webspeak.v1.impl.net.WebSpeakNet;
import net.betrayd.webspeak.v1.impl.net.WebSpeakNet.UnknownPacketException;
import net.betrayd.webspeak.v1.impl.util.NetUtils;

@WebSocket
public class PlayerWSConnection implements PlayerConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerWSConnection.class);

    private final WebSpeakServer server;

    private Session session;
    private WebSpeakPlayer player;

    private String sessionID;

    public PlayerWSConnection(WebSpeakServer server) {
        this.server = server;
    }

    public WebSpeakServer getServer() {
        return server;
    }

    public WebSpeakPlayer getPlayer() {
        return player;
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @OnWebSocketOpen
    public void onWebSocketOpen(Session session) {
        if (this.session != null) {
            throw new IllegalStateException("Session is already connected!");
        }

        this.session = session;
        this.sessionID = NetUtils.splitQueryString(session.getUpgradeRequest().getQueryString()).get("id");
        WebSpeakPlayer player = server.getPlayerBySessionID(sessionID);

        if (player == null) {
            session.close(StatusCode.BAD_PAYLOAD, "No player exists with session ID " + sessionID, Callback.NOOP);
            return;
        }

        if (player.isConnected()) {
            session.close(StatusCode.BAD_PAYLOAD, "Session " + sessionID + " already has a player connected!", Callback.NOOP);
        }
        this.player = player;
        player.setConnection(this);
        player.getServer().onWebsocketConnected(this);
    }
    
    @OnWebSocketMessage
    public void onWebSocketMessage(String message) {
        try {
            WebSpeakNet.applyPacket(player, message);
        } catch (UnknownPacketException e) {
            session.close(StatusCode.BAD_PAYLOAD, "Unknown packet type: " + e.getPacketId(), Callback.NOOP);
            LOGGER.warn("{} sent unknown packet '{}'", player.getPlayerId(), e.getPacketId());
        } catch (Exception e) {
            session.close(StatusCode.SERVER_ERROR, e.getMessage(), Callback.NOOP);
        }
    }

    @OnWebSocketClose
    public void onWsClose(int statusCode, String reason) {
        if (player != null && player.getConnection() == this) {
            player.setConnection(null);
            server.onWebsocketDisconnected(this);
        }
    }

    public void sendText(String message) {
        session.sendText(message, Callback.NOOP);
    }

    public <T> void sendPacket(S2CPacket<T> packet, T val) {
        sendText(WebSpeakNet.writePacket(packet, val));
    }

    public void disconnect(int statusCode, String reason) {
        session.close(statusCode, reason, Callback.NOOP);
    }

    public void disconnect(String reason) {
        session.close(StatusCode.NORMAL, reason, Callback.NOOP);
    }

    public String getRemoteAddress() {
        return session.getRemoteSocketAddress().toString();
    }
}
