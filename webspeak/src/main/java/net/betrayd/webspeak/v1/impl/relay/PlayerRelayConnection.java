package net.betrayd.webspeak.v1.impl.relay;

import java.nio.ByteBuffer;

import org.eclipse.jetty.util.NanoTime;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.PlayerConnection;
import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.impl.net.WebSpeakNet;
import net.betrayd.webspeak.v1.impl.net.WebSpeakNet.UnknownPacketException;

//TODO: test for memory leaks
public class PlayerRelayConnection implements PlayerConnection, Session.Listener.AutoDemanding {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerRelayConnection.class);

    private final WebSpeakServer server;
    private final WebSpeakPlayer player;

    private Session session;

    private boolean connected = false;
    private String remoteAdress = null;

    public PlayerRelayConnection(WebSpeakServer server, WebSpeakPlayer player) {
        this.server = server;
        this.player = player;
    }

    /*
     * while we wait for the player make sure we keep alive
     */
    int ticks = 0;
    public void tick()
    {
        if(!isConnected() && ticks > 20)
        {
            ticks = 0;
            sendKeepAlive();
        }
        ticks++;
    }

    private void sendKeepAlive()
    {
        if(session == null)
        {
            return;
        }
        //LOGGER.info("Ah^ ah^ ah- ah-...");
        ByteBuffer buffer = ByteBuffer.allocate(8).putLong(NanoTime.now()).flip();
        session.sendPing(buffer, Callback.NOOP);
    }

    // TODO: determine if we need a wrapper class for sessions to get all data we
    // would get in a real connection or if this is enough
    // TODO: LOW_PRIORITY: don't accept packets except connection packets if we are
    // not yet connected.
    /**
     * When the client has connected to the relay
     * 
     * @param connectionContext the context of the connection. Currently this is
     *                          hardcoded to the remote adress because that's all we
     *                          need. We may need a wrapper class in the future
     */
    public void clientConnected(String connectionContext) {
        this.connected = true;
        this.remoteAdress = connectionContext;
        player.getServer().onWebsocketConnected(this);
    }

    /**
     * When the client has disconnected from the relay
     */
    public void clientDisconnected(int statusCode, String reason) {
        connected = false;
        this.remoteAdress = null;
        server.onWebsocketDisconnected(this);
    }

    @Override
    public WebSpeakServer getServer() {
        return this.server;
    }

    @Override
    public WebSpeakPlayer getPlayer() {
        return this.player;
    }

    @Override
    public boolean isConnected() {
        return session != null && connected;
    }

    @Override
    public void sendText(String message) {
        session.sendText(message, Callback.NOOP);
    }

    @Override
    public void disconnect(int statusCode, String reason) {
        session.close(StatusCode.NORMAL, reason, Callback.NOOP);
    }

    @Override
    public String getRemoteAddress() {
        return remoteAdress;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        // The WebSocket endpoint has been opened.

        // Store the session to be able to send data to the remote peer.
        this.session = session;
        player.setConnection(this);
    }

    @Override
    public void onWebSocketText(String message) {
        try {
            WebSpeakNet.applyPacket(player, message);
        } catch (UnknownPacketException e) {
            session.close(StatusCode.BAD_PAYLOAD, "Unknown packet type: " + e.getPacketId(), Callback.NOOP);
            LOGGER.warn("{} sent unknown packet '{}'", player.getPlayerId(), e.getPacketId());
        } catch (Exception e) {
            session.close(StatusCode.SERVER_ERROR, e.getMessage(), Callback.NOOP);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        // The WebSocket endpoint failed.

        // You may log the error.
        LOGGER.error("Failed to connect to the relay!", cause);
    }
}
