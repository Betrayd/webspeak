package net.betrayd.webspeak.v1;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A channel that a player can be in. Players will only be considered for scope
 * with other players in the same channel.
 * 
 * @apiNote The channel must be added to the server via
 *          {@link WebSpeakServer#addChannel} before any of its players will be
 *          considered for scope.
 */
public class WebSpeakChannel {

    private final String name;

    /**
     * Keep track fo the server we currently belong to.
     */
    private WeakReference<WebSpeakServer> associatedServer;

    public WebSpeakChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    // If the player is no longer being tracked by the server, no reason to keep it here.
    private final Set<WebSpeakPlayer> players = Collections.newSetFromMap(new WeakHashMap<>());

    public final Set<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    /**
     * Called when a player is added to the channel.
     * @param player Player that was added.
     * @return If the player was not already in the channel.
     * @apiNote Should only be called by the player's <code>setChannel</code> function.
     */
    protected synchronized boolean onAddPlayer(WebSpeakPlayer player) {
        WebSpeakServer server = getAssociatedServer();
        if (server != null && server != player.getServer()) {
            throw new IllegalArgumentException("Player belongs to the wrong server!");
        }
        return players.add(player);
    }
    
    /**
     * Called when a player is removed from the channel.
     * @param player Player that was removed.
     * @return If the player was in the channel.
     * @apiNote Should only be called by the player's <code>setChannel</code> function.
     */
    protected synchronized boolean onRemovePlayer(WebSpeakPlayer player) {
        if (players.remove(player)) {
            // Manually remove all players from scope
            // because the scope checker won't check them any more.
            player.getServer().kickScopes(player);
            if (players.isEmpty()) {
                associatedServer = null;
            }
            return true;
        }
        return false;
    }

    /**
     * Remove all players from this channel, transfering them to another channel.
     * @param other Channel to transfer players to.
     */
    public synchronized void clear(WebSpeakChannel other) {
        if (other == this)
            return;
        // Copy player list to avoid concurrent modification
        for (var player : List.copyOf(players)) {
            player.setChannel(other);
        }
    }

    /**
     * Get the server that this channel is associated with. A channel is considered
     * associated with a server when one or more players in the channel belong to
     * said server. If a channel is associated with a server, only players that
     * belong to that server may join.
     * 
     * @return The associated server. <code>null</code> if there is none.
     */
    public WebSpeakServer getAssociatedServer() {
        return associatedServer != null ? associatedServer.get() : null;
    }

    @Override
    public String toString() {
        return "WebSpeak Channel (" + name + ")";
    }


}
