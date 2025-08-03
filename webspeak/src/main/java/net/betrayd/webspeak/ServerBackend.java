package net.betrayd.webspeak;

import java.util.function.Consumer;

/**
 * A "backend type" that a webspeak server can use. This will handle
 * implementing Jetty and manage player connections.
 */
public interface ServerBackend {
    /**
     * Start the server, and block until it has started.
     * @throws Exception If something goes wrong during startup.
     */
    void start() throws Exception;

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

    boolean isRunning();
}
