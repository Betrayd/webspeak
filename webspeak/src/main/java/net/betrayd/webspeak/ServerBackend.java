package net.betrayd.webspeak;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A "backend type" that a webspeak server can use. This will handle
 * implementing Jetty and manage player connections.
 */
public interface ServerBackend {

    /**
     * Start the server.
     * @return A future that completes once a connection to the relay has been established.
     */
    CompletableFuture<?> start();

    /**
     * Stop the server.
     * @return A future that completes once the server has fully stopped.
     */
    CompletableFuture<?> stop();

    /**
     * Register a listener for when a player connects to the server.
     * @param listener Connection listener
     * @implNote Could get called on a thread that's <em>not</em> the server thread.
     */
    void onPlayerConnected(Consumer<PlayerConnection> listener);

    /**
     * Request a unique session ID from the relay.
     * @return A future that completes once the relay responds with a session ID.
     *
     */
    CompletableFuture<String> requestSessionID();

    boolean isRunning();
}
