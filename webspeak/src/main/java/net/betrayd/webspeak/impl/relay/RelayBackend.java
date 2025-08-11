package net.betrayd.webspeak.impl.relay;

import lombok.Getter;
import net.betrayd.webspeak.PlayerConnection;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.WebSpeakPlayer;
import net.betrayd.webspeak.event.WebSpeakEvent;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.WebSocketClient;
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

    private final WebSocketClient webSocketClient = new WebSocketClient();

    /**
     * Called when a player connects to the server.
     */
    private final WebSpeakEvent<Consumer<PlayerConnection>> onPlayerConnected = WebSpeakEvent.createSimple();
    /**
     * Called when a player disconnects from the server.
     */
    private final WebSpeakEvent<PlayerDisconnectEvent> onPlayerDisconnected = WebSpeakEvent.createArrayBacked(
            listeners -> (reason, connection) -> {
                for (var l : listeners)
                    l.onDisconnect(reason, connection);
            }
    );
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
        CompletableFuture<String> future = new CompletableFuture<>();
        try{
            webSocketClient.start();
            webSocketClient.connect(connection, );
        }
        catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<?> stop() {
        Callback.Completable future = new Callback.Completable();

        var base = connection.getBaseSession();
        if (base == null) {
            future.fail(new IllegalStateException("Base session is not connected!"));
            return future;
        }
        base.close(1001, "Server shutdown", future);

        return future;
    }

    @Override
    public void onPlayerConnected(Consumer<PlayerConnection> listener) {
        onPlayerConnected.addListener(listener);
    }

    //TODO: should this also return reason?
    @Override
    public void onPlayerDisconnect(PlayerDisconnectEvent listener){
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
        if(connection.getBaseSession() == null){
            return false;
        }
        return connection.getBaseSession().isOpen();
    }

    @Override
    public void tick() {

    }

    @Override
    public void setPlayerSupplier(Function<? super String, ? extends WebSpeakPlayer> playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    public final SimpleWSSession.Listener createListener(SimpleWSSession session, String id) {
        RelayPlayerConnection playerConnection = new RelayPlayerConnection(session, id);
        onPlayerConnected.invoker().accept(playerConnection);
        playerConnection.onDisconnected((reason) -> {onPlayerDisconnected.invoker().onDisconnect(reason, playerConnection);});
        return playerConnection;
    }

    static class RelayPlayerConnection implements PlayerConnection, SimpleWSSession.Listener {
        final SimpleWSSession session;
        final String id;

        final WebSpeakEvent<Consumer<String>> onReceiveMessage = WebSpeakEvent.createSimple();
        final WebSpeakEvent<Consumer<DisconnectReason>> onDisconnect = WebSpeakEvent.createSimple();

        RelayPlayerConnection(SimpleWSSession session, String id) {
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
