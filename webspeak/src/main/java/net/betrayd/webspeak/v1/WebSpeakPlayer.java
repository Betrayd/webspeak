package net.betrayd.webspeak.v1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.impl.net.packets.SetAudioModifierS2CPacket;
import net.betrayd.webspeak.v1.impl.relay.PlayerRelayConnection;
import net.betrayd.webspeak.v1.impl.util.URIComponent;
import net.betrayd.webspeak.v1.util.AudioModifier;
import net.betrayd.webspeak.v1.util.WSPlayerListEntry;
import net.betrayd.webspeak.v1.util.WebSpeakVector;

public abstract class WebSpeakPlayer {

    private final WebSpeakServer server;
    private final String playerId;
    private final String sessionId;
    
    protected final Logger LOGGER;

    PlayerConnection connection;
    
    public WebSpeakPlayer(WebSpeakServer server, String playerId, String sessionId) {
        LOGGER = LoggerFactory.getLogger("WebSpeak Player (" + playerId + ")");

        this.server = server;
        this.playerId = playerId;
        this.sessionId = sessionId;
    }

    /**
     * Get the session ID used in the URL when a websocket connection is made.
     * @return Session ID.
     */
    public final String getPlayerId() {
        return playerId;
    }

    /**
     * Get the public-facing ID used for this player.
     * @return Public-facing ID
     */
    public final String getSessionId() {
        return sessionId;
    }

    public final WebSpeakServer getServer() {
        return server;
    }

    private WSPlayerListEntry playerListEntry = new WSPlayerListEntry("Default Name", "");
    // Initialize to true so we send updates on our next tick.
    private boolean isPlayerListDirty = true;

    public WSPlayerListEntry getPlayerListEntry() {
        return playerListEntry;
    }

    public void setPlayerListEntry(WSPlayerListEntry playerListEntry) {
        if (!playerListEntry.isValid()) {
            throw new IllegalArgumentException(
                    "Player list entry may not contain null values. (" + playerListEntry + ")");
        }
        
        this.playerListEntry = playerListEntry;
        this.isPlayerListDirty = true;
    }

    private AudioModifier defaultAudioModifier = AudioModifier.DEFAULT;

    /**
     * Get the audio modifier that other players will use on this player when
     * there's no additional modifiers.
     * 
     * @return Default audio modifier.
     */
    public AudioModifier getDefaultAudioModifier() {
        return defaultAudioModifier;
    }

    /**
     * Set the audio modifier that other players will use on this player when
     * there's no additional modifiers.
     * 
     * @param defaultAudioModifier Default audio modifier.
     */
    public void setDefaultAudioModifier(AudioModifier defaultAudioModifier) {
        if (defaultAudioModifier == null) {
            defaultAudioModifier = AudioModifier.DEFAULT;
        }
        if (this.defaultAudioModifier.equals(defaultAudioModifier)) {
            return;
        }
        this.defaultAudioModifier = defaultAudioModifier;
        for (var player : server.getPlayersInScope(this)) {
            player.invalidateAudioModifier(this);
        }
    }

    private WebSpeakChannel channel = null;

    private final List<WebSpeakGroup> groups = new ArrayList<>();

    /**
     * Return an unmodifiable list of groups the player is in. Groups with a greater
     * index take a higher priority over groups with a lesser index.
     * 
     * @return Unmodifiable list.
     */
    public List<WebSpeakGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public boolean isInGroup(WebSpeakGroup group) {
        return groups.contains(group);
    }

    /**
     * Add this player to a group.
     * @param group Group to add.
     * @return If the player was not already in the group.
     */
    public synchronized boolean addGroup(WebSpeakGroup group) {
        if (groups.contains(group)) {
            return false;
        }
        groups.add(group);
        onAddGroup(group);
        // Call this here because onAddGroup may need to be called without invalidating modifiers.
        invalidateAudioModifiers(group.getAudioModifiedPlayers());
        return true;
    }
    
    /**
     * Add this player to a group at a specified index.
     * @param group Group to add.
     * @param index Index to add at. Higher indexes take priority over lower indexes.
     * @return If the player was not already in the group.
     */
    public synchronized boolean addGroup(WebSpeakGroup group, int index) {
        if (groups.contains(group)) {
            return false;
        }
        groups.add(index, group);
        onAddGroup(group);
        invalidateAudioModifiers(group.getAudioModifiedPlayers());
        return true;
    }

    /**
     * Add a this player to a collection of groups.
     * @param groups Groups to add.
     */
    public synchronized void addGroups(Collection<? extends WebSpeakGroup> groups) {
        Set<WebSpeakGroup> groupSet = new HashSet<>(this.groups);
        Set<WebSpeakPlayer> invalidPlayers = new HashSet<>();
        for (var group : groups) {
            // Easy way to check for duplicates
            if (groupSet.add(group)) {
                this.groups.add(group);
                onAddGroup(group);
                invalidPlayers.addAll(group.getAudioModifiedPlayers());
            }
        }
        invalidateAudioModifiers(invalidPlayers);
    }

    /**
     * Remove this player from a group.
     * @param group Group to remove from.
     * @return If the player was in the group.
     */
    public synchronized boolean removeGroup(WebSpeakGroup group) {
        Collection<WebSpeakPlayer> invalidPlayers = null;
        boolean success = false;
        if (groups.remove(group)) {
            // This is called before onRemoveGroup so, in case the group has an audio
            // modifier on itself, this player gets included in that.
            invalidPlayers = group.getAudioModifiedPlayers();
            onRemoveGroup(group);
            success = true;
        }
        if (success) {
            invalidateAudioModifiers(invalidPlayers);
        }
        return success;
    }

    /**
     * Remove this player from a collection of groups.
     * @param groups Groups to remove from.
     * @return If the player was in any of the groups.
     */
    public synchronized boolean removeGroups(Collection<? extends WebSpeakGroup> groups) {
        Set<WebSpeakPlayer> invalidPlayers = new HashSet<>();
        // Checks make sure this will never duplicate.
        boolean success = false;
        var iterator = this.groups.iterator();
        while (iterator.hasNext()) {
            var group = iterator.next();
            if (groups.contains(group)) {
                iterator.remove();
                invalidPlayers.addAll(group.getAudioModifiedPlayers());
                onRemoveGroup(group);
                success = true;
            }
        }

        invalidateAudioModifiers(invalidPlayers);
        return success;
    }

    /**
     * Remove this player from all groups.
     */
    public synchronized void clearGroups() {
        Set<WebSpeakPlayer> invalidPlayers = new HashSet<>();
        for (var group : groups) {
            invalidPlayers.addAll(group.getAudioModifiedPlayers());
            onRemoveGroup(group);
        }
        invalidateAudioModifiers(invalidPlayers);
        groups.clear();
    }
    
    /**
     * Arbitrarily modify the group list for this player, and call the appropriate
     * callbacks based on what was changed.
     * 
     * @param modifier Function to modify the groups.
     * @throws IllegalArgumentException If the modifier function adds duplicate groups.
     * @implNote Because of the amount of checks & listeners needed, this method is
     *           fairly inefficient. Use the other group methods for simpler tasks.
     */
    public synchronized void modifyGroups(Consumer<? super List<WebSpeakGroup>> modifier) throws IllegalArgumentException {
        List<WebSpeakGroup> prev = List.copyOf(groups);
        List<WebSpeakGroup> modified = new ArrayList<>(groups);
        modifier.accept(modified);

        // Ths could probably be more efficient, but it's called rarely with a small dataset so it doesn't really matter.
        Set<WebSpeakGroup> duplicateChecker = new HashSet<>();
        for (var item : modified) {
            if (!duplicateChecker.add(item)) {
                throw new IllegalArgumentException("Player may not have two instances of a group. (" + item + ")");
            }
        }

        if (prev.equals(modified)) {
            return;
        }

        List<WebSpeakGroup> removedGroups = new ArrayList<>(modified.size());
        List<WebSpeakGroup> addedGroups = new ArrayList<>(modified.size());

        for (var group : modified) {
            if (!prev.contains(group)) {
                addedGroups.add(group);
            }
        }

        for (var group : prev) {
            if (!modified.contains(group)) {
                removedGroups.add(group);
            }
        }
        
        groups.clear();
        groups.addAll(modified);

        // Invalidate all players who are in all groups + removed groups because order
        // could have changed.
        Set<WebSpeakPlayer> invalidModifiers = new HashSet<>();
        for (var group : groups) {
            invalidModifiers.addAll(group.getAudioModifiedPlayers());
        }
        for (var group : removedGroups) {
            invalidModifiers.addAll(group.getAudioModifiedPlayers());
        }

        for (var added : addedGroups) {
            onAddGroup(added);
        }
        for (var removed : removedGroups) {
            onRemoveGroup(removed);
        }
        
        invalidateAudioModifiers(invalidModifiers);
    }
    
    /**
     * Queue a collection of players to have their audio modifiers re-calculated.
     * @param players Players collection.
     */
    public void invalidateAudioModifiers(Collection<? extends WebSpeakPlayer> players) {
        invalidAudioModifiers.addAll(players);
        // for (var player : players) {
        //     updatePlayerAudioModifiers(player);
        // }
    }

    /**
     * Queue a player to have its audio modifier re-calculated.
     * @param player Target player.
     */
    public void invalidateAudioModifier(WebSpeakPlayer player) {
        invalidAudioModifiers.add(player);
    }

    private final Consumer<Collection<? extends WebSpeakPlayer>> invalidateAudioModifiersListener = this::invalidateAudioModifiers;
    
    /**
     * Called when this player is added to a group.
     * @param group Group that was added to.
     */
    protected void onAddGroup(WebSpeakGroup group) {
        group.onAddPlayer(this);
        group.ON_MODIFIER_INVALIDATED.addListener(invalidateAudioModifiersListener);
    }

    /**
     * Called when this player is removed from a group.
     * @param group Group that was removed from.
     */
    protected void onRemoveGroup(WebSpeakGroup group) {
        group.onRemovePlayer(this);
        group.ON_MODIFIER_INVALIDATED.removeListener(invalidateAudioModifiersListener);
    }

    /**
     * Cache of the audio modifiers being used for each player. This way, we don't
     * have to constantly re-calculate them.
     */
    private final WeakHashMap<WebSpeakPlayer, AudioModifier> audioModifierCache = new WeakHashMap<>();

    /**
     * A set of players who need their audio modifiers updated.
     */
    private final Set<WebSpeakPlayer> invalidAudioModifiers = new HashSet<>();
    
    /**
     * Re-calculate the audio modifiers used on a given player and send to the client.
     * @param player Player to calculate.
     */
    protected synchronized void updatePlayerAudioModifiers(WebSpeakPlayer player) {
        if (isConnected() && server.areInScope(this, player)) {
            AudioModifier modifier = computeAudioModifier(player);
            if (!modifier.equals(audioModifierCache.get(player))) {
                // new SetAudioModifierS2CPacket(player.playerId, modifier).send(wsContext);
                audioModifierCache.put(player, modifier);
                sendAudioModifierUpdate(player, modifier);
            }
        } else {
            // Just invalidate the cache if we're not in scope.
            audioModifierCache.remove(player);
        }
    }

    private synchronized void sendAudioModifierUpdate(WebSpeakPlayer target, AudioModifier modifier) {
        new SetAudioModifierS2CPacket(target.playerId, modifier).send(connection);
    }
    
    /**
     * Compute the audio modifier this player will use for a target player based on
     * both players' groups.
     * 
     * @param player Target player.
     * @return The combined audio modifier.
     * @implNote This method does not modify or rely on the cache in any way. It
     *           always computes the audio modifier from scratch and returns it,
     *           regardless of scope.
     */
    public synchronized AudioModifier computeAudioModifier(WebSpeakPlayer player) {
        AudioModifier modifier = AudioModifier.DEFAULT;
        for (var group : groups) {
            modifier = AudioModifier.combine(group.computeAudioModifier(player), modifier);
        }
        return modifier;
    }
    
    /**
     * Get the audio modifier this player has cached for another player without
     * recalculating.
     * 
     * @param player Target player.
     * @return The audio modifier, or <code>null</code> if the audio modifier hasn't
     *         been cached.
     */
    public synchronized AudioModifier getCachedAudioModifier(WebSpeakPlayer player) {
        return audioModifierCache.get(player);
    }
    
    /**
     * Compute the audio modifier this player will use for a target player based on
     * both players' groups. If the audio modifier has been cached use the cached
     * version. Otherwise, compute it and save it in the cache.
     * 
     * @param player Target player.
     * @return Combined audio modifier.
     */
    public synchronized AudioModifier getAudioModifier(WebSpeakPlayer player) {
        return audioModifierCache.computeIfAbsent(player, this::computeAudioModifier);
    }

    protected synchronized void tickAudioModifiers() {
        for (var player : invalidAudioModifiers) {
            updatePlayerAudioModifiers(player);
        }
        invalidAudioModifiers.clear();
    }

    public final WebSpeakChannel getChannel() {
        return channel;
    }

    public synchronized void setChannel(WebSpeakChannel channel) {
        if (channel == this.channel) {
            return;
        }

        if (channel != null) {
            WebSpeakServer channelServer = channel.getAssociatedServer();
            if (channelServer != null && channelServer != getServer()) {
                throw new IllegalArgumentException("Channel is associated with the wrong server!");
            }
        }

        if (this.channel != null) {
            this.channel.onRemovePlayer(this);
        }
        this.channel = channel;
        if (channel != null) {
            this.channel.onAddPlayer(this);
        }
        if (getServer().getFlag(WebSpeakFlags.DEBUG_CHANNEL_SWAPS)) {
                LOGGER.info("Player joined channel " + (channel != null ? channel.getName() : "null"));
        }
    }
    
    /**
     * Called when this player has joined the scope of another player.
     * @param other The other player.
     */
    protected void onJoinedScope(WebSpeakPlayer other) {
        if (isConnected()) {
            sendAudioModifierUpdate(other, getAudioModifier(other));
        }
    }

    /**
     * Called when this player has left the scope of another player. Called for all
     * players in scope when this player's client disconnects.
     * 
     * @param other The other player.
     */
    protected void onLeftScope(WebSpeakPlayer other) {
        audioModifierCache.remove(other);
    }


    /**
     * Get the global location of this player.
     * @return Player location vector.
     */
    public abstract WebSpeakVector getLocation();

    /**
     * Get the forward direction of this player.
     * @return Player forward vector
     */
    public WebSpeakVector getForward() {
        return new WebSpeakVector(0, 0, 1);
    }

    /**
     * Get the up direction of this player.
     * @return Player up vector
     */
    public WebSpeakVector getUp() {
        return new WebSpeakVector(0, 1, 0);
    }

    /**
     * Check if this player is in scope with another player.
     * 
     * @param other Other player.
     * @return If we're in scope
     */
    public boolean isInScope(WebSpeakPlayer other) {
        if (!this.getAudioModifier(other).isSpatialized() || !other.getAudioModifier(this).isSpatialized()) {
            return true;
        }

        float range = server.getMaxAudioRange();
        return this.getLocation().squaredDistanceTo(other.getLocation()) <= range * range;
    }

    /**
     * Perform any additional ticking this webspeak player desires.
     */
    public void tick() {
        if(this.connection instanceof PlayerRelayConnection playerCon)
        {
            playerCon.tick();
        }
        tickAudioModifiers();
        if (isPlayerListDirty) {
            server.onUpdatePlayerListEntry(playerId, playerListEntry);
            isPlayerListDirty = false;
        }
    }
    
    /**
     * Get the username of this player. Should be overwritten by game implementations.
     * @return Player username.
     */
    public String getUsername() {
        return playerId;
    }

    // public final WsContext getWsContext() {
    //     return wsContext;
    // }
    
    public final boolean isConnected() {
        return connection != null && connection.isConnected();
    }
    
    public PlayerConnection getConnection() {
        return connection;
    }

    /**
     * Set the player's connection. Should not be used except internally.
     * @param connection New connection.
     */
    public void setConnection(PlayerConnection connection) {
        this.connection = connection;
    }

    /**
     * Called after the player is removed from the server.
     */
    protected void onRemoved() {
        setChannel(null);
        clearGroups();
    }

    /**
     * Get a URL for clients to connect to this webspeak player.
     * 
     * @param frontendAddress Base URL of the frontend.
     * @param backendAddress  Base URL of the backend.
     * @return Connection URL.
     */
    public final String getConnectionURL(String frontendAddress, String backendAddress) {
        return getConnectionURL(frontendAddress, backendAddress, getSessionId());
    }

    /**
     * Get a URL for clients to connect to webspeak with a given session ID.
     * 
     * @param frontendAddress Base URL of the frontend.
     * @param backendAddress  Base URL of the backend.
     * @param sessionID       Session ID to connect with.
     * @return Connection URL.
     */
    public static String getConnectionURL(String frontendAddress, String backendAddress, String sessionID) {
        return frontendAddress + "?server=" +
                URIComponent.encode(backendAddress) + "&id=" + sessionID;
    }

    @Override
    public String toString() {
        return "WebSpeakPlayer[" + playerId + "]";
    }
}
