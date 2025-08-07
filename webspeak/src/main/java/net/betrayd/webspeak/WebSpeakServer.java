package net.betrayd.webspeak;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.event.ServerEvents;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * The main server for WebSpeak. Responsible for keeping track of players, managing coordinate updates,
 * and facilitating everything else that WebSpeak needs to do.
 * @param <T> The player implementation that this server will use.
 */
public class WebSpeakServer<T extends WebSpeakPlayer> implements Executor {

    /**
     * The server backend responsible for initiating connections and communicating with players.
     */
    @Getter
    private final ServerBackend backend;

    /**
     * Container for various events related to this server.
     */
    @Getter
    private final ServerEvents serverEvents = new ServerEvents();

    private volatile boolean inTick;
    private volatile Thread tickThread;

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    /**
     * A map of all players with their session IDs.
     */
    private final BiMap<String, T> players = HashBiMap.create();
    private final BiMap<String, T> playersUnmod = Maps.unmodifiableBiMap(players);

    /**
     * A map of all audio sources with their audio IDs.
     */
    private final BiMap<String, AudioSource3D> audioSources = HashBiMap.create();
    private final BiMap<String, AudioSource3D> audioSourcesUnmod = Maps.unmodifiableBiMap(players);


    public WebSpeakServer(ServerBackend backend) {
        this.backend = backend;
    }

    /**
     * Return a map of all players with their session IDs.
     * @return Unmodifiable map view. Updated when players are added or removed.
     */
    public final BiMap<String, T> getPlayers() {
        return playersUnmod;
    }

    /**
     * Return a map of all audio sources with their audio source IDs.
     * @return Unmodifiable map view. Updated when audio sources are added or removed.
     */
    public final BiMap<String, AudioSource3D> getAudioSources() {
        return audioSourcesUnmod;
    }

    /**
     * Get a player by their session ID.
     * @param sessionId Player's session ID.
     * @return The player, or <code>null</code> if no player by that ID exists.
     */
    @Nullable
    public final T getPlayer(String sessionId) {
        return players.get(sessionId);
    }

    /**
     * Get a player's session ID.
     * @param player Player to get.
     * @return The session ID, or <code>null</code> if the player is not part of this server.
     */
    @Nullable
    public final String getSessionId(WebSpeakPlayer player) {
        return players.inverse().get(player);
    }

    /**
     * Get an audio source by its ID.
     * @param audioId Audio source ID
     * @return The audio source, or <code>null</code> if no audio source by that ID exists.
     */
    @Nullable
    public final AudioSource3D getAudioSource(String audioId) {
        return audioSources.get(audioId);
    }

    /**
     * Get an audio source's audio ID.
     * @param audioSource Audio source to get.
     * @return The audio ID, or <code>null</code> if the audio source doesn't belong to this server.
     */
    @Nullable
    public final String getAudioID(AudioSource3D audioSource) {
        return audioSources.inverse().get(audioSource);
    }

    /**
     * Add a player to the server.
     *
     * @param player    Player to add.
     * @param sessionId Session ID to assign.
     * @param audioId   Audio ID to assign.
     * @throws IllegalArgumentException If the player belongs to the wrong server.
     * @throws IllegalStateException    If the session ID or audio ID already exist.
     */
    public void addPlayer(T player, String sessionId, String audioId) throws IllegalArgumentException, IllegalStateException {
        assertInTick();
        if (player.getServer() != this) {
            throw new IllegalArgumentException("Player belongs to the wrong server!");
        }

        if (players.containsKey(sessionId)) {
            throw new IllegalStateException("Duplicate session ID: " + sessionId);
        }
        if (audioSources.containsKey(audioId)) {
            throw new IllegalStateException("Duplicate audio ID: " + audioId);
        }
        players.put(sessionId, player);
        audioSources.put(audioId, player);

        player.onPlayerAdded(sessionId, audioId);
        serverEvents.ON_PLAYER_ADDED.invoker().onPlayerAdded(player, sessionId, audioId);
        serverEvents.ON_AUDIO_SOURCE_ADDED.invoker().onAudioSourceAdded(player, audioId);
    }

    /**
     * Add an audio source to the server.
     *
     * @param audioSource Audio source to add.
     * @param audioId     ID to assign it.
     * @return <code>true</code> if it was added; <code>false</code> if it wasn't due to a duplicate ID.
     * @throws IllegalArgumentException If the audio source is a player (use addPlayer instead)
     */
    public boolean addAudioSource(AudioSource3D audioSource, String audioId) throws IllegalArgumentException {
        assertInTick();
        if (audioSource instanceof WebSpeakPlayer) {
            throw new IllegalArgumentException("Players must be added through addPlayer");
        }
        if (audioSources.putIfAbsent(audioId, audioSource) == null) {
            serverEvents.ON_AUDIO_SOURCE_ADDED.invoker().onAudioSourceAdded(audioSource, audioId);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Request a session ID from the relay and use it to create a player.
     *
     * @param audioId       Audio ID to assign.
     * @param playerFactory Player factory method.
     * @return A future that completes once the player has been created.
     */
    public CompletableFuture<T> createPlayer(String audioId, Function<WebSpeakServer<T>, T> playerFactory) {
        assertInTick();
        return backend.requestSessionID().thenApplyAsync(sid -> {
            T player = playerFactory.apply(this);
            addPlayer(player, sid, audioId);
            return player;
        }, this);
    }

    /**
     * Remove a player from the server.
     * @param player Player to remove.
     * @return If the player was found and could be removed.
     */
    public boolean removePlayer(WebSpeakPlayer player) {
        assertInTick();
        if (players.containsValue(player)) {
            player.onPlayerRemove();

            String aid = audioSources.inverse().remove(player);
            String sid = players.inverse().remove(player);

            serverEvents.ON_AUDIO_SOURCE_REMOVED.invoker().onAudioSourceRemoved(player, aid);
            serverEvents.ON_PLAYER_REMOVED.invoker().onPlayerRemoved(player, sid, aid);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove an audio source from the server.
     * @param source Audio source to remove.
     * @return If the audio source was found and could be removed.
     * @implNote Delegates to removePlayer if audio source is a player.
     */
    public boolean removeAudioSource(AudioSource3D source) {
        assertInTick();
        if (source instanceof WebSpeakPlayer p) {
            return removePlayer(p);
        } else {
            String aid = audioSources.inverse().remove(source);
            if (aid != null) {
                serverEvents.ON_AUDIO_SOURCE_REMOVED.invoker().onAudioSourceRemoved(source, aid);
                return true;
            }
            return false;
        }
    }

    /**
     * <p>Tick the server.</p>
     * <p>This should be called once every game tick, and it's where all the functionality such as
     * updating player positions takes place.</p>
     */
    public synchronized final void tick() {
        inTick = true;
        tickThread = Thread.currentThread();
        startTick();

        backend.tick();
        executeTasks();

        endTick();
        inTick = false;
    }

    void assertInTick() {
        if (!isInTick()) {
            throw new IllegalStateException("This function can only be called from within a webspeak tick.");
        }
    }

    /**
     * Check whether we're in the middle of ticking the server.
     * @return <code>true</code> if the calling thread is currently executing a webspeak tick.
     */
    public boolean isInTick() {
        return inTick && Thread.currentThread() == tickThread;
    }

    protected void startTick() {
        serverEvents.ON_START_TICK.invoker().run();
    }

    protected void endTick() {
        serverEvents.ON_END_TICK.invoker().run();
    }

    private void executeTasks() {
        Runnable command;
        while ((command = tasks.poll()) != null) {
            command.run();
        }
    }

    /**
     * Execute a command during the next server tick.
     * If we're currently ticking, run it now.
     * @param command the runnable task
     * @see #executeDeferred
     */
    @Override
    public final void execute(@NonNull Runnable command) {
        if (isInTick()) {
            command.run();
        } else {
            tasks.add(command);
        }
    }

    /**
     * Execute a command during the next server tick.
     * @param command the runnable task
     * @see #execute
     */
    public final void executeDeferred(@NonNull Runnable command) {
        tasks.add(command);
    }
}
