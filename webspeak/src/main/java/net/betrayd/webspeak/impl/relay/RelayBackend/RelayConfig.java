package net.betrayd.webspeak.impl.relay.RelayBackend;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.net.URI;

@Builder
@Getter
public class RelayConfig {
    @NonNull
    private URI relayAddress;

    /**
     * The number of milliseconds between each ping sent
     */
    @Builder.Default
    private long keepAliveInterval = 20000;
}
