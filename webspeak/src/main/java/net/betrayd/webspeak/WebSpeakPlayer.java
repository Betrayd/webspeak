package net.betrayd.webspeak;

import lombok.Getter;
import net.betrayd.webspeak.event.WebSpeakEvent;
import net.betrayd.webspeak.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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


    @Nullable @Getter
    private PlayerConnection playerConnection;

    public void setPlayerConnection(@Nullable PlayerConnection playerConnection) {
        if (playerConnection != null && playerConnection.getPlayer() != this) {
            throw new IllegalArgumentException("PlayerConnection belongs to the wrong player!");
        }
        this.playerConnection = playerConnection;
        if (playerConnection != null) {
            handlePlayerConnection(playerConnection);
        }
        server.getServerEvents().ON_PLAYER_CONNECTED.invoker().onPlayerConnected(this, playerConnection);
    }

    /**
     * Check if there is currently a client connected to this player.
     */
    public final boolean isConnected() {
        return playerConnection != null && playerConnection.isConnected();
    }

    public WebSpeakPlayer(WebSpeakServer server, String playerId, String sessionId) {
        this.server = server;
        this.playerId = playerId;
        this.sessionId = sessionId;
    }

    /**
     * Called when a client has connected to this player to set up packet listeners, etc.
     * @param playerConnection Player who connected.
     */
    protected void handlePlayerConnection(PlayerConnection playerConnection) {

    }

    /**
     * Get the global head position of this player.
     * @return Head position.
     */
    public abstract Vec3d getLocation();

    /**
     * Get the forward direction of this player.
     * @return Player forward vector
     */
    public abstract Vec3d getForward();

    /**
     * Get the up direction of this player.
     * @return Player up vector
     */
    public abstract Vec3d getUp();
}
