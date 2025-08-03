package net.betrayd.webspeak.v1.impl.relay;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import org.eclipse.jetty.util.NanoTime;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.WebSpeakPlayer;
import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.impl.ServerBackend;

//Don't bully me about the name. It's late I'm tired and just trying to follow the naming conventions
public class RelayServerBackend implements ServerBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelayServerBackend.class);

    private final WebSpeakServer webSpeakServer;
    private final String relayAddress;
    private WebSocketClient webSocketClient;

    private CoreRelay coreConnection;

    private final String serverID;
    private String privateRelayKey = null;

    public RelayServerBackend(WebSpeakServer webSpeakServer, String relayAddress, String serverID) {
        this.webSpeakServer = webSpeakServer;
        this.relayAddress = relayAddress;
        this.serverID = serverID;
    }

    public String getRelayAddress()
    {
        return this.relayAddress;
    }

    public String getServerID()
    {
        return this.serverID;
    }

    int tickTime = 20;

    public void tickBaseConnection()
    {
        if(coreConnection == null)
        {
            return;
        }
        if(tickTime > 20)
        {
            tickTime = 0;
            coreConnection.sendKeepAlive();
        }
        tickTime++;
    }

    /**
     * Initialize our base webSocket connection to let the server tell us what we
     * are identified as
     */
    private void createBaseConnection() {
        LOGGER.info("adress: "+relayAddress+"/host");
        URI serverURI = URI.create(relayAddress + "/host");

        coreConnection = new CoreRelay(this, serverID);

        try {
            webSocketClient.connect(coreConnection, serverURI);
        } catch (IOException e) {
            LOGGER.error(null, e);
        }
    }

    @Override
    public WebSpeakServer getServer() {
        return webSpeakServer;
    }

    @Override
    public void addPlayer(WebSpeakPlayer player) {
        URI serverURI = URI.create(relayAddress + "/addplayer?server=" + serverID + "&key="+privateRelayKey+"&id="+player.getSessionId());

        PlayerRelayConnection connection = new PlayerRelayConnection(webSpeakServer, player);

        try {
            webSocketClient.connect(connection, serverURI);
        } catch (IOException e) {
            LOGGER.error(null, e);
        }
    }

    @Override
    public void start(int port) throws Exception {
        if (webSocketClient != null) {
            throw new IllegalStateException("Server has already started.");
        }
        webSocketClient = new WebSocketClient();

        webSocketClient.start();

        createBaseConnection();
    }

    @Override
    public void stop() throws Exception {
        privateRelayKey = null;
        webSocketClient.stop();
    }

    @Override
    public boolean isRunning() {
        return webSocketClient != null && privateRelayKey != null && webSocketClient.isRunning();
    }

    @Override
    public int getPort() {
        return -1;
    }

    /*
     * Class for our core connection
     */
    // TODO: add keep alive packets
    public static class CoreRelay implements Session.Listener.AutoDemanding {
        private final RelayServerBackend backend;

        private Session session;
        private final String serverID;

        private CoreRelay(RelayServerBackend backend, String serverID) {
            this.backend = backend;
            this.serverID = serverID;
        }

        public void sendKeepAlive()
        {
            if(session == null)
            {
                return;
            }
            //LOGGER.info("Ah^ ah^ ah- ah-...");
            ByteBuffer buffer = ByteBuffer.allocate(8).putLong(NanoTime.now()).flip();
            session.sendPing(buffer, Callback.NOOP);
        }

        @Override
        public void onWebSocketOpen(Session session) {
            this.session = session;
            // TODO: add server idetifier here as payload
            session.sendText(serverID, Callback.NOOP);
        }

        // nothing fancy here just take the string and use it to tell our class what the
        // heck our ID is. No wrappers.
        @Override
        public void onWebSocketText(String message) {
            this.backend.privateRelayKey = message;
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            try {
                backend.stop();
            } catch (Exception e) {
                LOGGER.error("Our relay just disconnected. That should not have happened", e);
            }
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            // The WebSocket endpoint failed.

            // You may log the error.
            LOGGER.error("Failed to connect to the relay!", cause);
            try {
                backend.stop();
            } catch (Exception e) {
                LOGGER.error("Tried and failed to stop the server", e);
            }
        }

    }
}
