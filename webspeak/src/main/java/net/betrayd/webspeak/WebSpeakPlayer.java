package net.betrayd.webspeak;

import lombok.Getter;
import net.betrayd.webspeak.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A player that contains a connection, can obtain coordinates, etc.
 */
public abstract class WebSpeakPlayer implements AudioSource3D {

    /**
     * The server the player belongs to.
     */
    @Getter
    private final WebSpeakServer<?> server;


    @Nullable @Getter
    private PlayerConnection playerConnection;

    public WebSpeakPlayer(WebSpeakServer<?> server) {
        this.server = server;
    }

    public String getSessionId() {
        String id = server.getSessionId(this);
        if (id == null) {
            throw new IllegalStateException("This player has been removed from its server!");
        }
        return id;
    }

    public String getAudioId() {
        String id = server.getAudioID(this);
        if (id == null) {
            throw new IllegalStateException("This player has been removed from its server!");
        }
        return id;
    }

    public void setPlayerConnection(@Nullable PlayerConnection playerConnection) {
        if (playerConnection == this.playerConnection)
            return;

        if (playerConnection != null && playerConnection.getPlayer() != this) {
            throw new IllegalArgumentException("PlayerConnection belongs to the wrong player!");
        }
        var oldConnection = this.playerConnection;
        this.playerConnection = playerConnection;
        if (playerConnection != null) {
            handlePlayerConnection(playerConnection);
        }
        server.getServerEvents().ON_PLAYER_CONNECTED.invoker().onPlayerConnected(this, playerConnection);

        // Failsafe to ensure no duplicate connections
        if (oldConnection != null) {
            oldConnection.disconnect(PlayerConnection.DisconnectReason.UNKNOWN);
        }
    }

    /**
     * Disconnect the client from the server.
     *
     * @param reason Reason for disconnection.
     * @return If there was a client connected.
     * @implNote It's possible for the player to be disconnected without this being called.
     * If subclasses need to perform extra logic, override {@link #handlePlayerDisconnection} instead.
     */
    public boolean disconnect(PlayerConnection.DisconnectReason reason) {
        if (!isConnected())
            return false;

        assert playerConnection != null; // isConnected should check this
        playerConnection.disconnect(reason);
        return true;
    }

    /**
     * Check if there is currently a client connected to this player.
     */
    public final boolean isConnected() {
        return playerConnection != null && playerConnection.isConnected();
    }

    /**
     * Called when a client has connected to this player to set up packet listeners, etc.
     * @param playerConnection Player who connected.
     */
    protected void handlePlayerConnection(PlayerConnection playerConnection) {
        playerConnection.onDisconnected(reason -> handlePlayerDisconnection(playerConnection, reason));
    }

    /**
     * Called whenever a player client has disconnected from the server.
     *
     * @param connection Disconnected client.
     * @param reason     The reason for disconnection.
     * @implNote It's possible for this to be called on a connection that is not active.
     * Subclasses should compare it with {@link #getPlayerConnection()} first.
     */
    protected void handlePlayerDisconnection(PlayerConnection connection, PlayerConnection.DisconnectReason reason) {
        if (connection == this.playerConnection) {
            setPlayerConnection(null);
        }
        server.getServerEvents().ON_PLAYER_DISCONNECTED
                .invoker().onPlayerDisconnected(this, playerConnection, reason);
    }

    /**
     * Called when the player is added to the server.
     * @param sessionId The assigned session ID.
     * @param playerId The assigned player ID.
     */
    public void onPlayerAdded(String sessionId, String playerId) {

    }

    /**
     * Called when the player is being removed from the server.
     */
    public void onPlayerRemove() {
        if (playerConnection != null && playerConnection.isConnected()) {
            playerConnection.disconnect(PlayerConnection.DisconnectReason.PLAYER_REMOVED);
        }
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
