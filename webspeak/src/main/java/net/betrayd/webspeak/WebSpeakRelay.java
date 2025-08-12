package net.betrayd.webspeak;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.impl.relay.RelayBackend;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods involving the relay
 */
public final class WebSpeakRelay {

    private WebSpeakRelay() {};

    @Builder @Getter
    public static class Config {
        /**
         * The URL to attempt a relay connection to
         */
        @NonNull
        private URI connectionAddress;

        /**
         * When a session ID is requested, amount of time to wait before considering it timed-out.
         */
        @Builder.Default
        private long timeout = 10000;
    }


    public static CompletableFuture<ServerBackend> openRelayConnection(Config config, WebSocketClient wsClient) {
        RelayBackend backend = new RelayBackend(config);
        try {
            return wsClient.connect(backend, config.getConnectionAddress()).thenApply(s -> (ServerBackend) s)
                    .orTimeout(config.timeout, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public static CompletableFuture<ServerBackend> openRelayConnection(Config config) {
        return openRelayConnection(config, new WebSocketClient());
    }
}
