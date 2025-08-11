package net.betrayd.webspeak;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * The main server for WebSpeak. Responsible for keeping track of players, managing coordinate updates,
 * and facilitating everything else that WebSpeak needs to do.
 */
public class WebSpeakServer implements Executor {

    /**
     * Called when an audio source is added to or removed from the server.
     * @param audioSource The audio source in question.
     * @param audioId The source's audio ID.
     */
    public record AudioSourceEvent(AudioSource3D audioSource, String audioId) {}

    /**
     * Called when a player is added to or removed from the server.
     * @param player The player in question.
     * @param sessionId The player's session ID.
     */
    public record PlayerEvent(WebSpeakPlayer player, String sessionId) {}

    @Getter @NonNull
    private final ServerBackend serverBackend;

    private volatile boolean inTick;
    private volatile Thread tickThread;
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private final Event.Invokable<WebSpeakServer> onStartTick = Event.create();
    private final Event.Invokable<WebSpeakServer> onEndTick = Event.create();

    private final Event.Invokable<AudioSourceEvent> onAudioSourceAdded = Event.create();
    private final Event.Invokable<AudioSourceEvent> onAudioSourceRemoved = Event.create();

    private final Event.Invokable<PlayerEvent> onPlayerAdded = Event.create();
    private final Event.Invokable<PlayerEvent> onPlayerRemoved = Event.create();

    private final BiMap<String, WebSpeakPlayer> players = Maps.synchronizedBiMap(HashBiMap.create());
    private final BiMap<String, WebSpeakPlayer> playersUnmod = Maps.unmodifiableBiMap(players);

    private final BiMap<String, AudioSource3D> audioSources = Maps.synchronizedBiMap(HashBiMap.create());
    private final BiMap<String, AudioSource3D> audioSourcesUnmod = Maps.unmodifiableBiMap(audioSources);

    public WebSpeakServer(@NotNull ServerBackend serverBackend) {
        this.serverBackend = serverBackend;
    }

    public Event<WebSpeakServer> getOnStartTick() {
        return onStartTick;
    }

    public Event<WebSpeakServer> getOnEndTick() {
        return onEndTick;
    }

    public Event<AudioSourceEvent> getOnAudioSourceAdded() {
        return onAudioSourceAdded;
    }

    public Event<AudioSourceEvent> getOnAudioSourceRemoved() {
        return onAudioSourceRemoved;
    }

    public Event<PlayerEvent> getOnPlayerAdded() {
        return onPlayerAdded;
    }

    public Event<PlayerEvent> getOnPlayerRemoved() {
        return onPlayerRemoved;
    }

    /**
     * Return a map of audio source IDs and their corresponding audio sources.
     * @return Unmodifiable view of all audio sources. (includes players)
     */
    public final BiMap<String, AudioSource3D> getAudioSources() {
        return audioSourcesUnmod;
    }


    /**
     * Return a map of all session IDs and their corresponding players.
     *
     * @return Unmodifiable view of all players
     */
    public final BiMap<String, WebSpeakPlayer> getPlayers() {
        return playersUnmod;
    }

    /**
     * Add an audio source to the server.
     *
     * @param audioSource Audio source to add.
     * @param audioId     ID to assign the audio source.
     * @return <code>true</code> if the source was added. <code>false</code> if a source already exists with that ID.
     */
    public boolean addAudioSource(@NonNull AudioSource3D audioSource, @NonNull String audioId) {
        if (audioSources.putIfAbsent(audioId, audioSource) == null) {
            onAudioSourceAdded.invoke(new AudioSourceEvent(audioSource, audioId));
            return true;
        }
        return false;
    }

    /**
     * Remove an audio source by its ID.
     *
     * @param audioId ID of the audio source to remove.
     * @return The audio source that belonged to that ID; <code>null</code> if there was no audio source with that ID.
     */
    public @Nullable AudioSource3D removeAudioSource(String audioId) {
        var result = audioSources.remove(audioId);
        if (result != null) {
            onAudioSourceRemoved.invoke(new AudioSourceEvent(result, audioId));
        }
        return result;
    }

    /**
     * Remove an audio source from the server.
     *
     * @param audioSource Audio source to remove.
     * @return The ID the audio source had; <code>null</code> if it was not in this server.
     */
    public @Nullable String removeAudioSource(AudioSource3D audioSource) {
        var id = audioSources.inverse().remove(audioSource);
        if (id != null) {
            onAudioSourceRemoved.invoke(new AudioSourceEvent(audioSource, id));
        }
        return id;
    }

    /**
     * Add a player to the server.
     *
     * @param player    Player to add.
     * @param sessionId Session ID to give the player.
     * @return <code>true</code> if the player was added. <code>false</code> if the session ID was already in use.
     * @throws IllegalArgumentException If the player belongs to the wrong server.
     */
    public boolean addPlayer(@NonNull WebSpeakPlayer player, @NonNull String sessionId) throws IllegalArgumentException {
        if (player.getServer() != this) {
            throw new IllegalArgumentException("Player belongs to the wrong server!");
        }
        if (players.putIfAbsent(sessionId, player) != null) {
            onPlayerAdded.invoke(new PlayerEvent(player, sessionId));
            return true;
        }
        return false;
    }

    /**
     * Request a session ID from the relay and use it to add a player to the server.
     * @param player Player to add.
     * @return A future that completes with the session ID once the player is added.
     */
    public CompletableFuture<String> addPlayer(WebSpeakPlayer player) {
        if (player.getServer() != this) {
            throw new IllegalArgumentException("Player belongs to the wrong server!");
        }
        return serverBackend.requestSessionId().thenApply(id -> {
            if (!addPlayer(player, id)) {
                throw new RuntimeException("By the time webspeak the backend returned with a session ID, " +
                        "a player had been added with that session ID. So basically a programmer fucked up.");
            }
            return id;
        });
    }

    /**
     * Remove a player by its session ID.
     *
     * @param sessionId Session ID of the player to remove.
     * @return The player that belonged to that ID; <code>null</code> if there was no player with that ID.
     */
    public @Nullable WebSpeakPlayer removePlayer(String sessionId) {
        var player = players.remove(sessionId);
        if (player != null) {
            handleRemovedPlayer(player, sessionId);
        }
        return player;
    }

    /**
     * Remove a player from the server.
     *
     * @param player Player to remove.
     * @return The ID the player had; <code>null</code> if it was not in this server.
     */
    public @Nullable String removePlayer(WebSpeakPlayer player) {
        var id = players.inverse().remove(player);
        if (id != null) {
            handleRemovedPlayer(player, id);
        }
        return id;
    }

    private void handleRemovedPlayer(WebSpeakPlayer player, String sessionId) {
        serverBackend.releaseSessionId(sessionId, "Player removed from server.");
        onPlayerRemoved.invoke(new PlayerEvent(player, sessionId));
    }

    /**
     * Tick webspeak.
     */
    public synchronized void tick() {
        inTick = true;
        tickThread = Thread.currentThread();

        onStartTick.invoke(this);

        Runnable command;
        while ((command = tasks.poll()) != null) {
            command.run();
        }

        onEndTick.invoke(this);
        inTick = false;
    }

    public boolean isInTick() {
        return inTick && Thread.currentThread().equals(tickThread);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        if (isInTick())
            command.run();
        else
            tasks.add(command);
    }
}
