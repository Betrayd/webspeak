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
     * Stop the server, and block until it has fully stopped.
     * @throws Exception If something goes wrong during shutdown.
     */
    void stop() throws Exception;

    /**
     * Register a listener for when a player connects to the server.
     * @param listener Connection listener
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
