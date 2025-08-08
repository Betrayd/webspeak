package net.betrayd.webspeak.impl.relay;

import lombok.Getter;
import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.event.WebSpeakEvent;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class RelayBackend implements ServerBackend {

    @Getter
    private final WSMultiConnection connection = new WSMultiConnection(this::createListener);

    private Function<? super String, ? extends WebSpeakPlayer> playerSupplier = str -> null;

    private static final Logger LOGGER = LoggerFactory.getLogger(RelayBackend.class);

    private static final Map<Integer, CompletableFuture<String>> sessionIdRequests = new ConcurrentHashMap<>();
    private static final AtomicInteger nextIdRequest = new AtomicInteger();

    /**
     * Called when a player connects to the server.
     */
    private final WebSpeakEvent<Consumer<PlayerConnection>> onPlayerConnected = WebSpeakEvent.createSimple();
    /**
     * Called when a player disconnects from the server.
     */
    private final WebSpeakEvent<Consumer<PlayerConnection>> onPlayerDisconnected = WebSpeakEvent.createSimple();
    /**
     * Called after the server is stopped.
     */
    private final WebSpeakEvent<ServerStopEvent> onStop = WebSpeakEvent.createArrayBacked(
            listeners -> (statusCode, reason) -> {
                for (var l : listeners)
                    l.onServerStop(statusCode, reason);
            }
    );

    public RelayBackend() {
        connection.getOnReturnSessionId().addListener(this::onReturnSessionId);
        connection.getOnClose().addListener((statusCode, reason) -> {
            onStop.invoker().onServerStop(statusCode, reason);
        });

    }

    @Override
    public CompletableFuture<?> start() {
        return null;
    }

    //TODO: should this be returning the future in the same place as onStop when the server stops or just here? Should we call stop when the socket stops instead of just calling onStop? should onStop invoker be here instead?
    @Override
    public CompletableFuture<?> stop() {
        Callback.Completable future = new Callback.Completable();

        connection.close(1001, "Server shutdown", future);

        return future;
    }

    @Override
    public void onPlayerConnected(Consumer<PlayerConnection> listener) {
        onPlayerConnected.addListener(listener);
    }

    @Override
    public void onPlayerDisconnect(Consumer<PlayerConnection> listener){
        onPlayerDisconnected.addListener(listener);
    }

    @Override
    public void onStop(ServerStopEvent listener) {
        onStop.addListener(listener);
    }

    @Override
    public CompletableFuture<String> requestSessionID() {
        int request = nextIdRequest.getAndIncrement();

        CompletableFuture<String> future = new CompletableFuture<>();
        sessionIdRequests.put(request, future);


        String msg = RelayMessages.serializeRelayMessage(RelayMessages.S2RGetSessionId.TYPE,
                new RelayMessages.S2RGetSessionId(request));


        connection.sendRelayMessage(msg, new Callback() {
            @Override
            public void fail(Throwable x) {
                future.completeExceptionally(x);
            }
        });

        return future;
    }

    private void onReturnSessionId(int request, String id) {
        CompletableFuture<String> future = sessionIdRequests.remove(request);
        if (future == null) {
            LOGGER.warn("Received un-requested session ID: {}", id);
            return;
        }
        future.complete(id);
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void tick() {

    }

    @Override
    public void setPlayerSupplier(Function<? super String, ? extends WebSpeakPlayer> playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    public final SimpleWSSession.Listener createListener(SimpleWSSession session, String id) {
        return null;
    }

    static class ReplayPlayerConnection implements PlayerConnection, SimpleWSSession.Listener {
        final SimpleWSSession session;
        final String id;

        final WebSpeakEvent<Consumer<String>> onReceiveMessage = WebSpeakEvent.createSimple();
        final WebSpeakEvent<Consumer<DisconnectReason>> onDisconnect = WebSpeakEvent.createSimple();

        ReplayPlayerConnection(SimpleWSSession session, String id) {
            this.session = session;
            this.id = id;
        }

        @Override
        public void sendMessage(String message) {
            session.sendText(message, Callback.NOOP);
        }

        @Override
        public void onReceiveMessage(Consumer<String> listener) {
            onReceiveMessage.addListener(listener);
        }

        @Override
        public void onWebSocketText(String message) {
            onReceiveMessage.invoker().accept(message);
        }

        @Override
        public boolean isConnected() {
            return session.isOpen();
        }

        @Override
        public void disconnect(DisconnectReason reason) {
            session.close(StatusCode.NORMAL, reason.name(), Callback.NOOP);
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            onDisconnect.invoker().accept(DisconnectReason.CLIENT_DISCONNECT);
        }

        @Override
        public void onDisconnected(Consumer<DisconnectReason> listener) {
            onDisconnect.addListener(listener);
        }
    }
}
