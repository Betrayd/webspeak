package net.betrayd.webspeak;

import net.betrayd.webspeak.event.Event;

import java.util.concurrent.CompletableFuture;

/**
 * Responsible for managing out-of-band communication between webspeak and client browsers
 */
public interface ServerBackend {

    /**
     * Stop the server.
     *
     * @return A future that completes once the server has fully stopped.
     */
    CompletableFuture<?> close();

    /**
     * Called when a client browser connects to the server.
     */
    Event<String> getOnClientConnected();

    record ClientDisconnectEvent(String sessionId, int statusCode, String reason) {
    }

    /**
     * Called when a client browser disconnects from the server.
     */
    Event<ClientDisconnectEvent> getOnClientDisconnected();

    /**
     * Disconnect a client without releasing its session ID.
     *
     * @param sessionId  ID of the client to disconnect.
     * @param statusCode Websocket disconnect status code to send.
     * @param reason     Explanation to display to the user.
     * @return A future that completes once the message has been sent.
     */
    CompletableFuture<?> disconnectClient(String sessionId, int statusCode, String reason);

    /**
     * Request a unique session ID from the relay.
     * This session ID can be forwarded to players, and browsers who attempt to connect with this session ID will be sent to this server.
     *
     * @return A future that completes once the relay responds with a session ID.
     */
    CompletableFuture<String> requestSessionId();

    /**
     * ticks the backend, in order to do events like send keep alives, etc.
     */
    void tick();

    /**
     * Indicate to the relay that a session ID is no-longer in use. Also disconnects any connected client.
     *
     * @param sessionId The session ID to release.
     * @param reason    Explanation to display to the user.
     * @return A future that completes once the message has been sent.
     */
    CompletableFuture<?> releaseSessionId(String sessionId, String reason);

    record MessageReceivedEvent(String sessionId, String message) {
    }

    /**
     * Called whenever a message is received from a client.
     */
    Event<MessageReceivedEvent> getOnMessageReceived();

    /**
     * Send a message a client.
     *
     * @param sessionId Session ID of the client.
     * @param message   Message payload.
     * @return A future that completes once the message has been sent.
     */
    CompletableFuture<?> sendMessage(String sessionId, String message);

    /**
     * Called when an internal error has occurred, causing the session to close.
     */
    Event<Throwable> getOnError();

    /**
     * Called when the internal connection has closed.
     */
    Event<String> getOnClose();

    /**
     * Check if the connection to the relay is currently open.
     */
    boolean isOpen();
}
