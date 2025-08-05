package net.betrayd.webspeak.impl.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.betrayd.webspeak.impl.msg.S2CMessage;
import net.betrayd.webspeak.impl.relay.RelayBackend.RelayMessages;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * A single websocket connection which emulates multiple connections.
 */
public class WSMultiConnection implements Session.Listener.AutoDemanding {

    private static final Logger LOGGER = LoggerFactory.getLogger(WSMultiConnection.class);

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
    public void sendRelayMessage(S2CMessage message, Callback callback) {
        var base = baseSession;
        if (base == null) {
            throw getNoBase();
        }
        String msg = ";" + RelayMessages.writeMessage(message);
        base.sendText(msg, callback);
    }

    @Override
    public void onWebSocketOpen(Session session) {
        this.baseSession = session;
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

        if (type.equals("addClient")) {
            handleAddClient(GSON.fromJson(message, RelayMessages.AddClientMessage.class));
        } else if (type.equals("closeClient")) {
            handleCloseClient(GSON.fromJson(message, RelayMessages.CloseClientMessage.class));
        } else {
            LOGGER.warn("Unknown relay message type; {}", type);
        }
    }

    public void handleAddClient(RelayMessages.AddClientMessage msg) {
        SimpleSession session = new SimpleSession(msg.sessionId());
        var listener = listenerFactory.apply(session, msg.sessionId());
        if (listeners.put(msg.sessionId(), listener) != null) {
            LOGGER.warn("Second client connected with duplicate session id: {}", msg.sessionId());
        }
    }

    public void handleCloseClient(RelayMessages.CloseClientMessage msg) {
        var listener = listeners.remove(msg.sessionId());
        if (listener != null) {
            listener.onWebSocketClose(msg.statusCode(), msg.reason());
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        for (var l : listeners.values()) {
            l.onWebSocketError(cause);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        for (var l : listeners.values()) {
            l.onWebSocketClose(statusCode, reason);
        }
    }

    private IllegalStateException getNoBase() {
        return new IllegalStateException("Base session is not connected!");
    }


    class SimpleSession implements SimpleWSSession {

        final String id;

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
            sendRelayMessage(new RelayMessages.CloseClientMessage(id, statusCode, reason), callback);
        }

        @Override
        public void disconnect() {
            sendRelayMessage(new RelayMessages.DisconnectClientMessage(id), Callback.NOOP);
            listeners.remove(id);
        }

        @Override
        public boolean isOpen() {
            var base = baseSession;
            return base != null && base.isOpen();
        }

        @Override
        public boolean isSecure() {
            var base = baseSession;
            return base != null && base.isSecure();
        }
    }
}
