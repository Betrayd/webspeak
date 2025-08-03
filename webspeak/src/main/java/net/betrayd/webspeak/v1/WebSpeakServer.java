package net.betrayd.webspeak.v1;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.betrayd.webspeak.v1.WebSpeakFlags.WebSpeakFlag;
import net.betrayd.webspeak.v1.impl.PlayerCoordinateManager;
import net.betrayd.webspeak.v1.impl.RTCManager;
import net.betrayd.webspeak.v1.impl.RelationGraph;
import net.betrayd.webspeak.v1.impl.ServerBackend;
import net.betrayd.webspeak.v1.impl.WebSpeakFlagHolder;
import net.betrayd.webspeak.v1.impl.jetty.JettyServerBackend;
import net.betrayd.webspeak.v1.impl.net.WebSpeakNet;
import net.betrayd.webspeak.v1.impl.net.packets.LocalPlayerInfoS2CPacket;
import net.betrayd.webspeak.v1.impl.net.packets.PlayerListPackets;
import net.betrayd.webspeak.v1.impl.net.packets.SetPannerOptionsC2SPacket;
import net.betrayd.webspeak.v1.impl.relay.RelayServerBackend;
import net.betrayd.webspeak.v1.impl.util.WebSpeakUtils;
import net.betrayd.webspeak.v1.util.PannerOptions;
import net.betrayd.webspeak.v1.util.WSPlayerListEntry;
import net.betrayd.webspeak.v1.util.WebSpeakEvents;
import net.betrayd.webspeak.v1.util.WebSpeakEvents.WebSpeakEvent;
import net.betrayd.webspeak.v1.util.WebSpeakMath;

//TODO: remove everthing I changed to this class in the last commit and make it good. THis class is the worst offender of the slapped together code I threw in

/**
 * The primary WebSpeak server
 * 
 * @param <T> The player implementation to use
 */
public class WebSpeakServer implements Executor {
    // This is a bit of a monoclass lol, but I think it's okay.

    @SuppressWarnings("exports")
    public static final Logger LOGGER = LoggerFactory.getLogger(WebSpeakServer.class);

    public static final String VERSION_STRING = "1.0";

    public static interface WebSpeakPlayerFactory<T extends WebSpeakPlayer> {
        public T create(WebSpeakServer server, String playerId, String sessionId);
    }

    private ServerBackend serverBackend;

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    /**
     * All the players that are relevent to the game
     */
    private final Map<String, WebSpeakPlayer> players = new ConcurrentHashMap<>();

    // If the channel is not being referenced by any players or held externally, no need to keep it around.
    private final Set<WebSpeakChannel> channels = Collections
            .synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    
    private final WebSpeakChannel defaultChannel;

    // private final RelationGraph<WebSpeakPlayer> rtcConnections = new RelationGraph<>();
    private final RTCManager rtcManager = new RTCManager(this);
    private final PlayerCoordinateManager playerCoordinateManager = new PlayerCoordinateManager(this);

    /**
     * Keep track of all players in scope with each other.
     * @apiNote Only players that have a client connected are considered for scope.
     */
    private final RelationGraph<WebSpeakPlayer> scopes = new RelationGraph<>();

    final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_SESSION_CONNECTED = WebSpeakEvents.createSimple();
    final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_SESSION_DISCONNECTED = WebSpeakEvents.createSimple();
    final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_ADDED = WebSpeakEvents.createSimple();
    final WebSpeakEvent<Consumer<WebSpeakPlayer>> ON_PLAYER_REMOVED = WebSpeakEvents.createSimple();

    protected final WebSpeakEvent<BiConsumer<WebSpeakPlayer, WebSpeakPlayer>> ON_JOIN_SCOPE = WebSpeakEvents.createBiConsumer();
    protected final WebSpeakEvent<BiConsumer<WebSpeakPlayer, WebSpeakPlayer>> ON_LEAVE_SCOPE = WebSpeakEvents.createBiConsumer();
    
    private final WebSpeakFlagHolder flagHolder = new WebSpeakFlagHolder();
    
    public <T> T setFlag(WebSpeakFlag<T> flag, T value) {
        return flagHolder.setFlag(flag, value);
    }

    public <T> T getFlag(WebSpeakFlag<T> flag) {
        return flagHolder.getFlag(flag);
    }

    public Map<WebSpeakFlag<?>, Object> getFlags() {
        return flagHolder.getFlags();
    }

    private final PannerOptions pannerOptions = new PannerOptions();

    private float maxAudioRange = 24;

    /**
     * The object used to control the panning config on the client.
     * Make sure to call {@link #updatePannerOptions} after updating.
     */
    public PannerOptions getPannerOptions() {
        return pannerOptions;
    }

    /**
     * Send an updated copy of the panner options to all clients.
     */
    public void updatePannerOptions() {
        maxAudioRange = (float) WebSpeakMath.getMaxRange(pannerOptions);
        WebSpeakNet.sendPacketTo(getPlayers(), SetPannerOptionsC2SPacket.PACKET, pannerOptions);
    }

    /**
     * Get the max audible range of players based on the current panner options.
     * @return Max range.
     */
    public float getMaxAudioRange() {
        return maxAudioRange;
    }

    /**
     * Get the base Javalin app
     */
    // public Javalin getApp() {
    //     return app;
    // }

    public boolean isRunning() {
        return serverBackend.isRunning();
    }

    public int getPort() {
        return serverBackend.getPort();
    }

    public WebSpeakServer() {
        defaultChannel = createChannel("default");
    }

    /**
     * Start the server as a websocket endpoint.
     * 
     * @param port The port to start on.
     * @throws Exception If something bad happens while starting the server.
     */
    public synchronized void startJetty(int port) throws Exception {
        serverBackend = new JettyServerBackend(this);
        serverBackend.start(port);
    }
    
    /**
     * A function to start the server using websocket client connections to bypass SSL and port forwarding limitations
     * @param relayServerURL the URL of the relay server
     * @throws Exception
     */
    public synchronized void startRelay(String relayServerURL) throws Exception {
        //create a relayServer backed with our ID set to a random UUID because the chance we make a duplicate one is about 0
        String serverID = UUID.randomUUID().toString();
        LOGGER.info("Server ID is: " + serverID);
        serverBackend = new RelayServerBackend(this, relayServerURL, serverID);
        serverBackend.start(-1);
    }

    /**
     * Starts the server in relay mode, with a given ID. 
     * <p><b>This method is not recommended as if another server uses the same id the relay may refuse the connection</b></p>
     * @param relayServerURL the URL of the relay server
     * @param serverID our ID on the relay server
     * @throws Exception
     */
    public synchronized void startRelay(String relayServerURL, String serverID) throws Exception {
        serverBackend = new RelayServerBackend(this, relayServerURL, serverID);
        serverBackend.start(-1);
    }

    /**
     * Get the Address the relay server is using
     * @return the address the relay server is running on, or NULL if we are not in relay mode
     */
    public String getRelayAddress(){
        if(serverBackend instanceof RelayServerBackend relayB)
        {
            return relayB.getRelayAddress();
        }
        //this should throw but I don't know what to throw so this is fine I guess
        return null;
    }

    /**
     * Get the ID assosiated with the server on the relay
     * @return the ID used on the relay, or NULL if we are not in relay mode
     */
    public String getRelayServerID() {
        if(serverBackend instanceof RelayServerBackend relayB)
        {
            return relayB.getServerID();
        }
        //this should throw but I don't know what to throw so this is fine I guess
        return null;
    }

    /**
     * Synchronously shutdown the server instance. Could block for some time.
     */
    public void stop() {
        synchronized(this) {
            if (!serverBackend.isRunning())
                return;
            
            for (var playerID : List.copyOf(players.keySet())) {
                removePlayer(playerID);
            }
        }
        try {
            serverBackend.stop();
        } catch (Exception e) {
            LOGGER.error("Error stopping WebSpeak server.", e);
        }
    }

    void onUpdatePlayerListEntry(String playerID, WSPlayerListEntry entry) {
        Map<String, WSPlayerListEntry> map = Map.of(playerID, entry);
        for (var player : this.players.values()) {
            if (player.isConnected()) {
                player.getConnection().sendPacket(PlayerListPackets.SET_PLAYER_ENTRIES_S2C, map);
            }
        }
    }

    /**
     * ticks the server: updates connections on distance etc.
     */
    public synchronized void tick() {

        if(serverBackend instanceof RelayServerBackend b)
        {
            b.tickBaseConnection();
        }

        if (!isRunning())
            return;
        Runnable task;
        while ((task = tasks.poll()) != null) {
            task.run();
        }

        // rtcManager.tickRTC();
        tickScopes();
        for (var player : players.values()) {
            player.tick();
        }

        playerCoordinateManager.tick();
    }

    private void tickScopes() {
        for (var channel : channels) {
            tickChannelScope(channel);
        }
    }

    private void tickChannelScope(WebSpeakChannel channel) {
        List<WebSpeakPlayer> connectedPlayers = channel.getPlayers().stream().filter(p -> p.isConnected()).toList();

        for (var pair : WebSpeakUtils.compareAll(connectedPlayers)) {
            if (pair.a().equals(pair.b())) {
                continue;
            }

            boolean wasInScope = scopes.containsRelation(pair.a(), pair.b());
            boolean isInScope = pair.a().isInScope(pair.b());

            if (!wasInScope && isInScope) {
                scopes.add(pair.a(), pair.b());
                joinScope(pair.a(), pair.b());
            } else if (wasInScope && !isInScope) {
                scopes.remove(pair.a(), pair.b());
                leaveScope(pair.a(), pair.b());
            }
        }
    }

    /**
     * Remove the player from the scope of all other players. Useful if player was
     * removed or disconnected.
     * 
     * @param player Player to kick.
     */
    protected synchronized void kickScopes(WebSpeakPlayer player) {
        for (var other : scopes.getRelations(player)) {
            leaveScope(player, other);
        }
        scopes.removeAll(player);
    }

    private void joinScope(WebSpeakPlayer a, WebSpeakPlayer b) {
        rtcManager.connectRTC(a, b);
        a.onJoinedScope(b);
        b.onJoinedScope(a);
        ON_JOIN_SCOPE.invoker().accept(a, b);
    }

    // Left package-private so channel can clear scopes when player removed.
    private void leaveScope(WebSpeakPlayer a, WebSpeakPlayer b) {
        rtcManager.disconnectRTC(a, b);
        a.onLeftScope(b);
        b.onLeftScope(a);
        ON_LEAVE_SCOPE.invoker().accept(a, b);
    }

    /**
     * Check if two players are in scope with each other.
     * 
     * @param a Player A.
     * @param b Player B.
     * @return If the players are in scope.
     * @apiNote Players can only be considered for scope when they have a web client
     *          connected.
     * @implNote This doesn't actually re-query the players; it simply checks if the
     *           scope manager thinks they're in scope.
     */
    public final boolean areInScope(WebSpeakPlayer a, WebSpeakPlayer b) {
        return (a == b) || scopes.containsRelation(a, b);
    }
    
    /**
     * Get a collection of all the players that are in scope with a given player.
     * 
     * @param player Player to check.
     * @return All players in scope.
     * @apiNote Players can only be considered for scope when they have a web client
     *          connected.
     */
    public final Collection<WebSpeakPlayer> getPlayersInScope(WebSpeakPlayer player) {
        return scopes.getRelations(player);
    }

    /**
     * Called when a websocket connection is established. Should not be used except internally.
     * @param connection New connection.
     */
    public void onWebsocketConnected(PlayerConnection connection) {
        connection.sendPacket(LocalPlayerInfoS2CPacket.PACKET, new LocalPlayerInfoS2CPacket(connection.getPlayer().getPlayerId()));
        connection.sendPacket(SetPannerOptionsC2SPacket.PACKET, pannerOptions);

        playerCoordinateManager.onPlayerConnected(connection.getPlayer());
        sendEntirePlayerList(connection);

        ON_SESSION_CONNECTED.invoker().accept(connection.getPlayer());
    }

    /**
     * Called when a websocket has disconnected. Should not be used except internally.
     * @param connection The connection.
     */
    public void onWebsocketDisconnected(PlayerConnection connection) {
        kickScopes(connection.getPlayer());
        LOGGER.info("Player {} disconnected from voice.", connection.getPlayer().getPlayerId());
    }

    public synchronized void sendEntirePlayerList(PlayerConnection target) {
        Map<String, WSPlayerListEntry> entries = new HashMap<>();
        for (var player : players.values()) {
            entries.put(player.getPlayerId(), player.getPlayerListEntry());
        }
        target.sendPacket(PlayerListPackets.SET_PLAYER_ENTRIES_S2C, entries);
    }

    /**
     * Remove a player from everyone's player list.
     * @param playerID ID of player to remove.
     */
    private synchronized void removeFromPlayerList(String playerID) {
        List<String> payload = Collections.singletonList(playerID);
        WebSpeakNet.sendPacketTo(players.values(), PlayerListPackets.REMOVE_PLAYER_ENTRIES_S2C, payload);

    }

    /**
     * Get an unmodifiable collection of all the channels in the server.
     * @return Unmodifiable view of channels.
     */
    public Set<WebSpeakChannel> getChannels() {
        return Collections.unmodifiableSet(channels);
    }

    /**
     * Create a WebSpeak channel.
     * @param name Channel name. Mainly for debugging purposes.
     * @return The channel.
     */
    public WebSpeakChannel createChannel(String name) {
        WebSpeakChannel channel = new WebSpeakChannel(name);
        channels.add(channel);
        return channel;
    }

    public synchronized boolean addChannel(WebSpeakChannel channel) {
        return channels.add(channel);
    }

    public WebSpeakChannel getDefaultChannel() {
        return defaultChannel;
    }

    /**
     * Get an unmodifiable collection of all the players in the server.
     * @return All webspeak players
     */
    public Collection<WebSpeakPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    /**
     * Count the number of players connected to WebSpeak.
     * @return Number of players.
     */
    public int numPlayers() {
        return players.size();
    }

    /**
     * Get an unmodifiable collection of all the players in the server.
     * @return All webspeak players
     */
    public Map<String, WebSpeakPlayer> getPlayerMap() {
        return Collections.unmodifiableMap(players);
    }

    /**
     * Check if a given player is part of the webspeak server.
     * 
     * @param player Player to check for.
     * @return If the player was there.
     */
    public boolean hasPlayer(Object player) {
        if (player instanceof WebSpeakPlayer webPlayer) {
            return hasPlayerId(webPlayer.getPlayerId());
        } else {
            return false;
        }
    }

    /**
     * Check if a player exists by its ID.
     * @param playerId Player ID to check for.
     * @return If there was a player wsith that ID.
     */
    public boolean hasPlayerId(String playerId) {
        return players.containsKey(playerId);
    }

    /**
     * Get a player by its ID.
     * 
     * @param playerId Player ID to search for.
     * @return Found player. <code>null</code> if no player exists with that ID.
     */
    public WebSpeakPlayer getPlayer(String playerId) {
        return players.get(playerId);
    }

    /**
     * Create and add a webspeak player, assigning it a random player ID and session
     * ID.
     * 
     * @param <T>     Player type.
     * @param factory Player factory function.
     * @return The newly-created factory.
     */
    public <T extends WebSpeakPlayer> T createPlayer(WebSpeakPlayerFactory<T> factory) {
        T player = factory.create(this, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        addPlayer(player, true);
        return player;
    }

    /**
     * Add a player to the webspeak server, replacing any player that already exists
     * with that ID.
     * 
     * @param player Player to add.
     * @return The previous player with that ID, if any.
     */
    public WebSpeakPlayer addPlayer(WebSpeakPlayer player) {
        return addPlayer(player, false);
    }

    protected WebSpeakPlayer addPlayer(WebSpeakPlayer player, boolean noCheck) {
        if (player == null) throw new NullPointerException("player");
        WebSpeakPlayer old;
        if (!noCheck) {
            if (player.getServer() != this) {
                throw new IllegalArgumentException("Player belongs to the wrong server!");
            }

            synchronized(this) {
                if (players.values().stream().anyMatch(p -> p.getSessionId().equals(player.getSessionId()))) {
                    throw new IllegalArgumentException("A player already exists with this session ID!");
                }
                old = players.put(player.getPlayerId(), player);
            }
        } else {
            old = players.put(player.getPlayerId(), player);
        }
        if (player.getChannel() == null) {
            player.setChannel(defaultChannel);
        }
        if (old != null) {
            onRemovePlayer(player);
        }
        onAddPlayer(player);
        return old;
    }

    /**
     * Create and add a webspeak player if one does not already exist with a given
     * ID.
     * 
     * @param playerID Player ID to use.
     * @param factory  Player factory.
     * @return The existing or new player instance.
     */
    public WebSpeakPlayer getOrCreatePlayer(String playerID, WebSpeakPlayerFactory<?> factory) {
        boolean added = false;
        WebSpeakPlayer player;
        synchronized(this) {
            player = players.get(playerID);
            if (player == null) {
                player = factory.create(this, playerID, UUID.randomUUID().toString());
                players.put(playerID, player);
                added = true;
            };
        }

        // Call this outside the synchronized block to reduce chance of deadlock.
        if (added) {
            onAddPlayer(player);
        }
        return player;
    }

    private void onAddPlayer(WebSpeakPlayer player) {
        serverBackend.addPlayer(player);
        ON_PLAYER_ADDED.invoker().accept(player);
    }

    /**
     * Remove a player from the webspeak server.
     * 
     * @param player Player to remove.
     * @return If the player was in the webspeak server.
     */
    public boolean removePlayer(Object player) {
        if (player instanceof WebSpeakPlayer webPlayer) {
            return removePlayer(webPlayer.getPlayerId()) != null;
        } else {
            return false;
        }
    }

    /**
     * Remove a player from the webspeak server.
     * 
     * @param playerId ID of the player to remove.
     * @return <code>WebSpeakPlayer</code> instance of the player that was removed.
     *         <code>null</code> if there was no player with that ID.
     */
    public WebSpeakPlayer removePlayer(String playerId) {
        WebSpeakPlayer player = players.remove(playerId);
        if (player != null) {
            onRemovePlayer(player);
        }
        return player;
    }

    protected void onRemovePlayer(WebSpeakPlayer player) {
        try {
            player.setChannel(null); // will automatically kick scopes
            PlayerConnection connection = player.getConnection();
            if (connection != null) {
                connection.disconnect("Player removed from server");
            }
            player.onRemoved();
            removeFromPlayerList(player.getPlayerId());

            serverBackend.removePlayer(player);
            ON_PLAYER_REMOVED.invoker().accept(player);
        } catch (Exception e) {
            LOGGER.error("Error removing player from server: " + player.getPlayerId(), e);
        }
    }

    /**
     * Find a player by its session ID.
     * 
     * @param sessionID Session ID to look for.
     * @return First player with that session ID. <code>null</code> if no player was
     *         found.
     * @implNote This method works by iterating through all players until it finds
     *           one with the session ID. It probably shouldn't be used in a loop.
     */
    public WebSpeakPlayer getPlayerBySessionID(String sessionID) {
        if (sessionID == null)
            return null;
        for (var player : players.values()) {
            if (player.getSessionId().equals(sessionID))
                return player;
        }
        return null;
    }

    /**
     * Called when a web client connects.
     * 
     * @param listener Event listener
     */
    public final void onSessionConnected(Consumer<WebSpeakPlayer> listener) {
        ON_SESSION_CONNECTED.addListener(listener);
    }

    /**
     * Called when a web client disconnects.
     * @param listener Event listener
     */
    public final void onSessionDisconnected(Consumer<WebSpeakPlayer> listener) {
        ON_SESSION_DISCONNECTED.addListener(listener);
    }

    /**
     * Called when a player is added to WebSpeak
     * @param listener Event listener
     */
    public final void onPlayerAdded(Consumer<WebSpeakPlayer> listener) {
        ON_PLAYER_ADDED.addListener(listener);
    }

    /**
     * Called when a player is removed from WebSpeak.
     * @param listener Event listener
     */
    public final void onPlayerRemoved(Consumer<WebSpeakPlayer> listener) {
        ON_PLAYER_REMOVED.addListener(listener);
    }

    /**
     * Called when two players join each other's scope.
     * @param listener Event listener
     */
    public final void onJoinScope(BiConsumer<WebSpeakPlayer, WebSpeakPlayer> listener) {
        ON_JOIN_SCOPE.addListener(listener);
    }

    /**
     * Called when two players leave each other's scope.
     * @param listener Event listener
     */
    public final void onLeaveScope(BiConsumer<WebSpeakPlayer, WebSpeakPlayer> listener) {
        ON_LEAVE_SCOPE.addListener(listener);
    }

    /**
     * Queue a task to be run at the beginning of the next tick.
     * @param command Task to queue.
     */
    @Override
    public void execute(Runnable command) {
        tasks.add(command);
    }
}
