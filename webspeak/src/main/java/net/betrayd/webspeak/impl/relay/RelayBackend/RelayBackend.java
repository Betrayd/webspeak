package net.betrayd.webspeak.impl.relay.RelayBackend;

import com.google.gson.JsonElement;
import lombok.Getter;
import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.event.WebSpeakEvent;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class RelayBackend implements ServerBackend, Session.Listener.AutoDemanding {

    private static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak Relay Connection");

    @Getter
    private final RelayConfig config;

    @Getter
    final WebSocketClient webSocketClient;

    @Getter @Nullable
    volatile Session session;

    @Nullable
    private volatile CompletableFuture<Session> startFuture;

    @Nullable
    private Function<? super String, ? extends WebSpeakPlayer> playerSupplier;

    private final Map<WebSpeakPlayer, ReplayPlayerConnection> connections = Collections.synchronizedMap(new WeakHashMap<>());

    private final WebSpeakEvent<Consumer<PlayerConnection>> playerConnectionEvent = WebSpeakEvent.createSimple();
    private final WebSpeakEvent<ServerStopEvent> serverStopEvent = WebSpeakEvent.createArrayBacked(
            listeners -> (code, reason) -> {
                for (var l : listeners) {
                    l.onServerStop(code, reason);
                }
            }
    );

    public RelayBackend(RelayConfig config, WebSocketClient webSocketClient) {
        this.config = config;
        this.webSocketClient = webSocketClient;
    }

    public RelayBackend(RelayConfig.RelayConfigBuilder config, WebSocketClient webSocketClient) {
        this(config.build(), webSocketClient);
    }

    @Override
    public CompletableFuture<Session> start() {
        if (isRunning() || startFuture != null)
            throw new IllegalStateException("Server is already running!");

        try {
            webSocketClient.connect(this, config.getRelayAddress());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        startFuture = new CompletableFuture<>();
        return startFuture;
    }

    @Override
    public CompletableFuture<?> stop() {
        var session = this.session;
        if (session == null) {
            throw new IllegalStateException("Server is not running!");
        }
        CompletableFuture<?> future = new CompletableFuture<>();
        session.close(StatusCode.NORMAL, "Closed by server", new Callback() {
            @Override
            public void succeed() {
                future.complete(null);
            }

            @Override
            public void fail(Throwable x) {
                future.completeExceptionally(x);
            }
        });
        return future;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        lastKeepalive = System.nanoTime() / 1000000L;
        this.session = session;
        var future = this.startFuture;
        if (future == null) {
            throw new IllegalStateException("Somehow, onWebSocketOpen was called before start()");
        }

        future.complete(session);
    }

    @Override
    public void onWebSocketText(String message) {
        String[] split = message.split(";", 2);
        String sessionId;
        String payload;

        if (split.length == 0) {
            return;
        } else if (split.length == 1) {
            sessionId = "";
            payload = split[0];
        } else {
            sessionId = split[0];
            payload = split[1];
        }

    }

    @Override
    public void onWebSocketError(Throwable cause) {
        var future = startFuture;
        if (future != null) {
            future.completeExceptionally(cause);
        }
        LOGGER.error("Error in relay websocket connection: ", cause);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        this.session = null;
        serverStopEvent.invoker().onServerStop(statusCode, reason);
    }


    @Override
    public void onStop(ServerStopEvent listener) {
        serverStopEvent.addListener(listener);
    }

    @Override
    public CompletableFuture<String> requestSessionID() {
        return null;
    }

    @Override
    public void onPlayerConnected(Consumer<PlayerConnection> listener) {
        playerConnectionEvent.addListener(listener);
    }

    public void handlePlayerConnection(String sessionId) {
        if (playerSupplier == null) {
            LOGGER.error("Player supplier has not been set; cannot connect player.");
            disconnectPlayer(sessionId, PlayerConnection.DisconnectReason.UNKNOWN);
            return;
        }
        WebSpeakPlayer player = playerSupplier.apply(sessionId);
        if (player == null) {
            LOGGER.warn("Tried to connect player with unknown session ID: {}", sessionId);
            disconnectPlayer(sessionId, PlayerConnection.DisconnectReason.UNKNOWN);
            return;
        }

        player.getServer().execute(() -> {
            if (player.isConnected()) {
                LOGGER.warn("Duplicate player connection: {}", sessionId);

                ReplayPlayerConnection playerConnection = new ReplayPlayerConnection(player);
                connections.put(player, playerConnection);
                player.setPlayerConnection(playerConnection);
            }
        });

    }

    public void disconnectPlayer(String sessionId, PlayerConnection.DisconnectReason reason) {
        // Send disconnect message
    }

    @Override
    public void setPlayerSupplier(@Nullable Function<? super String, ? extends WebSpeakPlayer> playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    @Override
    public boolean isRunning() {
        return session != null;
    }

    private long lastKeepalive;
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    @Override
    public void tick() {
        long time = System.nanoTime() / 1000000L;
        var session = this.session;
        if (session != null && time - lastKeepalive >= config.getKeepAliveInterval()) {
            session.sendPing(EMPTY_BUFFER, Callback.NOOP);
            lastKeepalive = time;
        }
    }


    class ReplayPlayerConnection implements PlayerConnection {

        final WebSpeakPlayer player;

        ReplayPlayerConnection(WebSpeakPlayer player) {
            this.player = player;
        }

        @Override
        public WebSpeakPlayer getPlayer() {
            return player;
        }

        @Override
        public void sendMessage(String message) {
            var ws = session;
            if (ws != null)
                ws.sendText(player.getSessionId() + ";" + message, Callback.NOOP);

        }

        @Override
        public void onReceiveMessage(Consumer<String> listener) {

        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void disconnect(DisconnectReason reason) {

        }

        @Override
        public void onDisconnected(Consumer<DisconnectReason> listener) {

        }
    }
}
