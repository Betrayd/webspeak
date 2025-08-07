package net.betrayd.webspeak.impl.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.betrayd.webspeak.event.WebSpeakEvent;
import net.betrayd.webspeak.impl.relay.RelayMessages;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A single websocket connection which emulates multiple connections.
 */
public class WSMultiConnection implements Session.Listener.AutoDemanding {

    private static final Logger LOGGER = LoggerFactory.getLogger(WSMultiConnection.class);

    /**
     * Called when the primary websocket connection has been established.
     */
    @Getter
    private final WebSpeakEvent<Consumer<Session>> onOpen = WebSpeakEvent.createSimple();

    public interface OnCloseEvent {
        void onClose(int statusCode, String reason);
    }

    /**
     * Called when the primary websocket connection has closed.
     */
    @Getter
    private final WebSpeakEvent<OnCloseEvent> onClose = WebSpeakEvent.createArrayBacked(
            listeners -> (code, reason) -> {
                for (var l : listeners)
                    l.onClose(code, reason);
            }
    );

    public interface OnReturnSessionId {
        void onReturnSessionId(int requestId, String id);
    }

    /**
     * Called when a returnSessionId message is received.
     */
    @Getter
    private final WebSpeakEvent<OnReturnSessionId> onReturnSessionId = WebSpeakEvent.createArrayBacked(
            listeners -> (req, id) -> {
                for (var l : listeners)
                    l.onReturnSessionId(req, id);
            }
    );

    private final BiFunction<SimpleWSSession, String, SimpleWSSession.Listener> listenerFactory;

    /**
     * The "real" websocket connection to the server.
     */
    @Getter
    @Nullable
    private volatile Session baseSession;

    private final Map<String, SimpleWSSession.Listener> listeners = new ConcurrentHashMap<>();

    public WSMultiConnection(BiFunction<SimpleWSSession, String, SimpleWSSession.Listener> listenerFactory) {
        this.listenerFactory = listenerFactory;
    }


    /**
     * Send a message intended to be handled by the relay.
     * @param message Message to send
     * @param callback callback to notify when the send operation is complete
     */
    public void sendRelayMessage(String message, Callback callback) {
        var base = baseSession;
        if (base == null) {
            throw getNoBase();
        }
        String msg = ";" + message;
        base.sendText(msg, callback);
    }

    @Override
    public void onWebSocketOpen(Session session) {
        this.baseSession = session;
        onOpen.invoker().accept(session);
    }

    @Override
    public void onWebSocketPing(ByteBuffer payload) {
        var base = baseSession;
        if (base == null)
            throw getNoBase();

        base.sendPong(payload, Callback.NOOP);
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
            return;
        }

        var listener = listeners.get(session);
        if (listener != null) {
            listener.onWebSocketText(payload);
        } else {
            LOGGER.warn("Received message for non-existent session ID ({}): {}", session, payload);
        }
    }

    protected void handleRelayMessage(String payload) {
        JsonObject message = GSON.fromJson(payload, JsonObject.class);
        String type = message.get("type").getAsString();

        if (type.equals(RelayMessages.R2SAddedClient.TYPE)) {
            handleAddClient(GSON.fromJson(message, RelayMessages.R2SAddedClient.class));
        } else if (type.equals(RelayMessages.R2SClosedClient.TYPE)) {
            handleCloseClient(GSON.fromJson(message, RelayMessages.R2SClosedClient.class));
        } else if (type.equals(RelayMessages.R2SReturnSessionId.TYPE)) {
            handleReturnSessionId(GSON.fromJson(message, RelayMessages.R2SReturnSessionId.class));
        } else {
            LOGGER.warn("Unknown relay message type: {}", type);
        }
    }

    private void handleAddClient(RelayMessages.R2SAddedClient msg) {
        SimpleSession session = new SimpleSession(msg.id());
        var listener = listenerFactory.apply(session, msg.id());
        if (listener == null) {
            LOGGER.warn("Cannot create listener for session id: {}", msg.id());
        } else if (listeners.put(msg.id(), listener) != null) {
            LOGGER.warn("Second client connected with duplicate session id: {}", msg.id());
        }
    }

    private void handleCloseClient(RelayMessages.R2SClosedClient msg) {
        var listener = listeners.remove(msg.id());
        if (listener != null) {
            listener.onWebSocketClose(msg.statusCode(), msg.reason());
        }
    }

    private void handleReturnSessionId(RelayMessages.R2SReturnSessionId msg) {
        onReturnSessionId.invoker().onReturnSessionId(msg.requestId(), msg.id());
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        for (var l : listeners.values()) {
            l.onWebSocketError(cause);
        }
        LOGGER.error("Error in websocket connection: ", cause);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        for (var l : listeners.values()) {
            l.onWebSocketClose(statusCode, reason);
        }
        onClose.invoker().onClose(statusCode, reason);
    }

    private IllegalStateException getNoBase() {
        return new IllegalStateException("Base session is not connected!");
    }


    class SimpleSession implements SimpleWSSession {

        final String id;

        volatile boolean isClosed;

        SimpleSession(String id) {
            this.id = id;
        }

        @Override
        public void sendText(String text, Callback callback) {
            var base = baseSession;
            if (base != null) {
                base.sendText(id + ";" + text, callback);
            } else {
                throw getNoBase();
            }
        }

        @Override
        public void close(int statusCode, String reason, Callback callback) {
            String msg = RelayMessages.serializeRelayMessage(RelayMessages.S2RDisconnectClient.TYPE,
                    new RelayMessages.S2RDisconnectClient(id, statusCode, reason));
            sendRelayMessage(msg, callback);
            listeners.remove(id);
            isClosed = true;
        }

        @Override
        public void disconnect() {
            String msg = RelayMessages.serializeRelayMessage(RelayMessages.S2RDisconnectClient.TYPE,
                    new RelayMessages.S2RDisconnectClient(id, -1, ""));
            sendRelayMessage(msg, Callback.NOOP);
            listeners.remove(id);
            isClosed = true;
        }

        @Override
        public boolean isOpen() {
            var base = baseSession;
            return !isClosed && base != null && base.isOpen();
        }

        @Override
        public boolean isSecure() {
            var base = baseSession;
            return base != null && base.isSecure();
        }
    }
}
