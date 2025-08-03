package net.betrayd.webspeaktest;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.WebSpeakFlags;
import net.betrayd.webspeak.v1.WebSpeakServer;
import net.betrayd.webspeak.v1.util.PannerOptions;

public class WebSpeakTestServer implements Executor {
    public static final Logger LOGGER = LoggerFactory.getLogger("WebSpeak Server");

    private final Thread thread;
    private final int port;
    private final boolean relay;
    private WebSpeakServer webSpeakServer;

    private boolean shutdownQueued;
    private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();
    private final CompletableFuture<WebSpeakServer> startFuture = new CompletableFuture<>();
    private Queue<Runnable> queue = new ConcurrentLinkedDeque<>();

    private final Queue<PannerOptions.Partial> pannerUpdates = new ConcurrentLinkedDeque<>();

    public WebSpeakTestServer(int port, boolean relay) {
        this.port = port;
        this.relay = relay;
        thread = new Thread(this::runThread, "WebSpeak Server");
        thread.setDaemon(false);
        thread.start();
    }
    
    /**
     * Wait for the server to finish starting.
     * @return A future that completes once the server has started.
     */
    public CompletableFuture<WebSpeakServer> awaitStart() {
        return startFuture;
    }

    @Override
    public void execute(Runnable command) {
        queue.add(command);
    }

    public WebSpeakServer getWebSpeakServer() {
        return webSpeakServer;
    }

    public boolean hasStarted() {
        return webSpeakServer != null && webSpeakServer.isRunning();
    }

    public boolean isOnThread() {
        return Thread.currentThread().equals(thread);
    }

    public void assertOnThread() {
        if (!isOnThread())
            throw new IllegalStateException("WebSpeak server called from incorrect thread: " + Thread.currentThread().getName());
    }

    /**
     * Queue an update the panner options to be added on the next tick.
     * @param newOptions
     */
    public void updatePannerOptions(PannerOptions.Partial newOptions) {
        if (newOptions == null)
            return;
        pannerUpdates.add(newOptions);
    }

    protected void runThread() {
        webSpeakServer = new WebSpeakServer();
        webSpeakServer.setFlag(WebSpeakFlags.DEBUG_CONNECTION_REQUESTS, true);
        webSpeakServer.setFlag(WebSpeakFlags.DEBUG_KEEPALIVE, true);
        webSpeakServer.setFlag(WebSpeakFlags.DEBUG_CHANNEL_SWAPS, true);
        webSpeakServer.getPannerOptions().maxDistance = 5;

        try {
            if(this.relay) {
                //TODO: make this not hardcoded
                webSpeakServer.startRelay("ws://localhost:8080");
            }
            else {
                webSpeakServer.startJetty(port);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to start WebSpeak server.", e);
        }
        
        startFuture.complete(webSpeakServer);
        while (!shutdownQueued) {
            Runnable task;
            while ((task = queue.poll()) != null) {
                task.run();
            }

            tick();
            try {
                Thread.sleep(30);
            } catch (InterruptedException e) {
                LOGGER.warn("WebSpeak Server was inturrupted during sleep.");
            }
        }

        try {
            webSpeakServer.stop();
        } catch (Exception e) {
            LOGGER.error("Exception stopping WebSpeak server: ", e);
        }
        shutdownFuture.complete(null);
    }

    protected void tick() {

        boolean updatedPanner = false;
        PannerOptions.Partial update;
        while ((update = pannerUpdates.poll()) != null) {
            webSpeakServer.getPannerOptions().copyFrom(update);
            updatedPanner = true;
        }
        if (updatedPanner) {
            webSpeakServer.updatePannerOptions();
        }

        webSpeakServer.tick();
    }
    
    public boolean isShutdownQueued() {
        return shutdownQueued;
    }

    public void shutdown() {
        shutdownQueued = true;
    }

    public final CompletableFuture<Void> getShutdownFuture() {
        return shutdownFuture;
    }
    
    public final CompletableFuture<Void> shutdownAsync() {
        shutdown();
        return shutdownFuture;
    }
    
    public final void shutdownAndWait() throws InterruptedException {
        shutdown();
        thread.join();
    }

    public String getLocalConnectionUrl() {
        if (webSpeakServer == null) {
            return "";
        }

        return "http://localhost:" + port;
    }

    public String getWsConnectionUrl() {
        if (webSpeakServer == null) {
            return "";
        }

        return "ws://localhost:" + port;
    }
}
