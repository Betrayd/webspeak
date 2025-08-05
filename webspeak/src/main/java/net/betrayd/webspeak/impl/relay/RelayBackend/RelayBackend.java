package net.betrayd.webspeak.impl.relay.RelayBackend;

import lombok.Getter;
import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.event.WebSpeakEvent;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RelayBackend implements ServerBackend, Session.Listener.AutoDemanding {

    @Getter
    private final RelayConfig config;

    @Getter
    private final WebSocketClient webSocketClient;

    @Getter @Nullable
    private volatile Session session;

    @Nullable
    private volatile CompletableFuture<Session> startFuture;

    private final WebSpeakEvent<Consumer<PlayerConnection>> PLAYER_CONNECTION_EVENT = WebSpeakEvent.createSimple();
    private final WebSpeakEvent<ServerStopEvent> STOP_EVENT = WebSpeakEvent.createArrayBacked(
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
    public void onWebSocketError(Throwable cause) {
        var future = startFuture;
        if (future != null) {
            future.completeExceptionally(cause);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        this.session = null;
        STOP_EVENT.invoker().onServerStop(statusCode, reason);
    }

    @Override
    public void onPlayerConnected(Consumer<PlayerConnection> listener) {
        PLAYER_CONNECTION_EVENT.addListener(listener);
    }

    @Override
    public void onStop(ServerStopEvent listener) {
        STOP_EVENT.addListener(listener);
    }

    @Override
    public CompletableFuture<String> requestSessionID() {
        return null;
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
}
