package net.betrayd.webspeak;

import lombok.Getter;

/**
 * A player that contains a connection, can obtain coordinates, etc.
 */
public abstract class WebSpeakPlayer {
    /**
     * The server the player belongs to.
     */
    @Getter
    private final WebSpeakServer server;

    /**
     * The public-facing player ID that identifies it across all clients.
     */
    @Getter
    private final String playerId;

    /**
     * The private session ID which the player's own client uses to connect.
     */
    @Getter
    private final String sessionId;

    public WebSpeakPlayer(WebSpeakServer server, String playerId, String sessionId) {
        this.server = server;
        this.playerId = playerId;
        this.sessionId = sessionId;
    }


}
