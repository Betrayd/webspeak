package net.betrayd.webspeak.testapp;

import lombok.Getter;
import net.betrayd.webspeak.WebSpeakRelay;
import net.betrayd.webspeak.WebSpeakServer;
import net.betrayd.webspeak.event.Event;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * A wrapper around a WebSpeak server which handles ticking
 */
public class ServerContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak Server Container");

    private final Thread thread;

    @Getter @Nullable
    private WebSpeakServer server;

    private volatile boolean shutdownQueued;

    private final CompletableFuture<WebSpeakServer> startupFuture = new CompletableFuture<>();
    private final CompletableFuture<?> shutdownFuture = new CompletableFuture<>();

    private final Event.Invokable<Throwable> onError = Event.create();

    public ServerContainer() {
        this.thread = new Thread(this::runThread, "WebSpeak Server");
        thread.setDaemon(false);
    }

    public CompletableFuture<WebSpeakServer> start() {
        thread.start();
        return startupFuture;
    }

    public Event<Throwable> getOnError() {
        return onError;
    }

    public boolean isOnThread() {
        return Thread.currentThread().equals(thread);
    }

    public CompletableFuture<?> shutdown() {
        shutdownQueued = true;
        return shutdownFuture;
    }

    protected void runThread() {
        WebSpeakRelay.Config config = WebSpeakRelay.Config.builder()
                .connectionAddress(URI.create("ws://localhost:8080"))
                .build();

        LOGGER.info("Starting WebSpeak thread");
        WebSpeakRelay.openRelayConnection(config).whenComplete((backend, ex) -> {
            if (ex != null) {
                onError.invoke(ex);
                LOGGER.error("Error starting webspeak: ", ex);
                shutdownQueued = true;
                startupFuture.completeExceptionally(ex);
            } else {
                LOGGER.info("Established connection to relay");
                server = new WebSpeakServer(backend);
                server.getOnStop().addListener(v -> shutdownFuture.complete(null));

                startupFuture.complete(this.server);
            }
        });

        while (!shutdownQueued) {
            tick();
        }

        if (server != null) {
            LOGGER.info("Stopping the server...");
            server.stop().join();
            LOGGER.info("Server stopped");
        }
    }

    private void tick() {
        if (server != null) {
            server.tick();
        }

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            LOGGER.warn("Thread interrupted while sleeping between ticks");
        }
    }
}
