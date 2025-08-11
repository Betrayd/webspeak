package net.betrayd.webspeak.impl.relay;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.betrayd.webspeak.ServerBackend;
import net.betrayd.webspeak.event.Event;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RelayBackend implements ServerBackend, Session.Listener.AutoDemanding {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelayBackend.class);

    private CompletableFuture<?> shutdownFuture;
    private @Nullable Session session;

    private final Event.Invokable<Session> onWsOpen = Event.create();
    private final Event.Invokable<String> onClientConnected = Event.create();
    private final Event.Invokable<ClientDisconnectEvent> onClientDisconnected = Event.create();
    private final Event.Invokable<MessageReceivedEvent> onMessageReceived = Event.create();
    private final Event.Invokable<Throwable> onError = Event.create();
    private final Event.Invokable<String> onClose = Event.create();

    private final Map<Integer, CompletableFuture<String>> sessionRequests = new ConcurrentHashMap<>();
    private final AtomicInteger nextIdRequest = new AtomicInteger();

    public Event<Session> getOnWsOpen() {
        return onWsOpen;
    }


    // TODO: PLEAASSEEE don't let this implementation stay. It's bad.
    @Override
    public synchronized CompletableFuture<?> stop() {
        if (session == null) {
            throw getNoSession();
        }
        if (shutdownFuture == null) {
            shutdownFuture = new CompletableFuture<>();

            session.close(1001, "Server shutting down", new Callback() {
                @Override
                public void succeed() {
                    shutdownFuture.complete(null);
                }

                @Override
                public void fail(Throwable x) {
                    shutdownFuture.completeExceptionally(x);
                }
            });
        }
        return shutdownFuture;
    }

    @Override
    public Event<String> getOnClientConnected() {
        return onClientConnected;
    }

    @Override
    public Event<ClientDisconnectEvent> getOnClientDisconnected() {
        return onClientDisconnected;
    }

    @Override
    public CompletableFuture<?> disconnectClient(String sessionId, String reason) {
        if (session == null)
            throw getNoSession();

        var message = new RelayMessages.S2RDisconnectClient(sessionId, 1000, reason);
        var future = new Callback.Completable();
        session.sendText(";" + RelayMessages.write(message), future);

        return future;
    }

    @Override
    public CompletableFuture<String> requestSessionId() {
        if (session == null)
            throw getNoSession();

        int request = nextIdRequest.getAndIncrement();

        CompletableFuture<String> future = new CompletableFuture<>();
        sessionRequests.put(request, future);

        String msg = RelayMessages.write(new RelayMessages.S2RGetSessionId(request));

        session.sendText(";" + msg, new Callback() {
            @Override
            public void fail(Throwable x) {
                future.completeExceptionally(x);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<?> releaseSessionId(String sessionId, String reason) {
        if (session == null)
            throw getNoSession();

        var message = new RelayMessages.S2RReleaseSessionId(sessionId, 1001, reason);
        var future = new Callback.Completable();
        session.sendText(";" + RelayMessages.write(message), future);

        return future;
    }

    @Override
    public Event<MessageReceivedEvent> getOnMessageReceived() {
        return onMessageReceived;
    }

    @Override
    public CompletableFuture<?> sendMessage(String sessionId, String message) {
        if (session == null)
            throw getNoSession();

        var future = new Callback.Completable();
        session.sendText(sessionId + ";" + message, future);

        return future;
    }

    @Override
    public Event<Throwable> getOnError() {
        return onError;
    }

    @Override
    public Event<String> getOnClose() {
        return onClose;
    }

    @Override
    public boolean isOpen() {
        return session != null && session.isOpen();
    }

    // WS listeners

    @Override
    public void onWebSocketOpen(Session session) {
        this.session = session;
        onWsOpen.invoke(session);
    }

    @Override
    public void onWebSocketPing(ByteBuffer payload) {
        if (session == null) {
            throw getNoSession();
        }

        session.sendPong(payload, Callback.NOOP);
    }

    private static final Gson GSON = new Gson();

    @Override
    public void onWebSocketText(String message) {
        String[] split = message.split(";", 2);

        String session;
        String payload;

        if (split.length == 0) {
            return;
        } else if (split.length == 1) {
            session = "";
            payload = split[0];
        } else {
            session = split[0];
            payload = split[1];
        }

        if (session.isBlank()) {
            handleRelayMessage(payload);
        } else {
            onMessageReceived.invoke(new MessageReceivedEvent(session, payload));
        }
    }

    private void handleRelayMessage(String payload) {
        JsonObject obj = GSON.fromJson(payload, JsonObject.class);

        String type = obj.get("type").getAsString();

        switch (type) {
            case RelayMessages.R2SAddedClient.TYPE ->
                    handleAddClient(GSON.fromJson(obj, RelayMessages.R2SAddedClient.class));
            case RelayMessages.R2SClosedClient.TYPE ->
                    handleCloseClient(GSON.fromJson(obj, RelayMessages.R2SClosedClient.class));
            case RelayMessages.R2SReturnSessionId.TYPE ->
                    handleReturnSessionId(GSON.fromJson(obj, RelayMessages.R2SReturnSessionId.class));
            default -> LOGGER.warn("Unknown relay message type: {}", type);
        }
    }

    private void handleAddClient(RelayMessages.R2SAddedClient msg) {
        onClientConnected.invoke(msg.id());
    }

    private void handleCloseClient(RelayMessages.R2SClosedClient msg) {
        onClientDisconnected.invoke(new ClientDisconnectEvent(msg.id(), DisconnectReason.CLIENT_DISCONNECT));
    }

    private void handleReturnSessionId(RelayMessages.R2SReturnSessionId msg) {
        CompletableFuture<String> future = sessionRequests.remove(msg.requestId());
        if (future == null) {
            LOGGER.warn("Received un-requested session ID: {} (request ID {})", msg.id(), msg.requestId());
            return;
        }
        future.complete(msg.id());
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        LOGGER.error("Error in relay websocket connection: ", cause);
        onError.invoke(cause);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        LOGGER.info("Relay websocket closed with status code {} ({})", statusCode, reason);
        onClose.invoke(reason);
    }

    private static IllegalStateException getNoSession() {
        return new IllegalStateException("WebSocket session is not connected!");
    }
}
