package net.betrayd.webspeak;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.event.Event;
import net.betrayd.webspeak.webrtc.RTCManager;
import net.betrayd.webspeak.webrtc.ice.LocalCandidate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * <p>The main server responsible for WebSpeak. While it doesn't handle in-band or out-of-band communications directly,
 * <code>WebSpeakServer</code> is responsible for coordinating everything that WebSpeakUses.</p>
 *
 * <p>The server has no "startup" sequence in its lifecycle. Because the only required initialization code involved is
 * connecting to the relay (or any other measures used to listen for connections), it's assumed that the
 * <code>ServerBackend</code> that's passed in the constructor has already finished initializing.</p>
 *
 * <p>It <em>does</em>, however, have a shutdown sequence. Calling {@link #stop()} will initiate this sequence,
 * where all players will be gracefully disconnected and the backend will be closed. This might happen asynchronously.</p>
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

    @Getter @NonNull
    private final RTCManager rtcManager;

    private volatile boolean inTick;
    private volatile Thread tickThread;
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private final Event.Invokable<WebSpeakServer> onStartTick = Event.create();
    private final Event.Invokable<WebSpeakServer> onEndTick = Event.create();

    private final Event.Invokable<AudioSourceEvent> onAudioSourceAdded = Event.create();
    private final Event.Invokable<AudioSourceEvent> onAudioSourceRemoved = Event.create();

    private final Event.Invokable<PlayerEvent> onPlayerAdded = Event.create();
    private final Event.Invokable<PlayerEvent> onPlayerRemoved = Event.create();

    private final Event.Invokable<WebSpeakServer> onStop = Event.create();

    private final BiMap<String, WebSpeakPlayer> players = Maps.synchronizedBiMap(HashBiMap.create());
    private final BiMap<String, WebSpeakPlayer> playersUnmod = Maps.unmodifiableBiMap(players);

    private final BiMap<String, AudioSource3D> audioSources = Maps.synchronizedBiMap(HashBiMap.create());
    private final BiMap<String, AudioSource3D> audioSourcesUnmod = Maps.unmodifiableBiMap(audioSources);

    private final CompletableFuture<?> shutdownFuture = new CompletableFuture<>();

    public WebSpeakServer(@NotNull ServerBackend serverBackend) {
        this.serverBackend = serverBackend;

        //TODO: add configuration somewhere else

        //RTCConfiguration config = new RTCConfiguration();
        //RTCIceServer iceServer = new RTCIceServer();
        //iceServer.urls.add("stun:stun.l.google.com:19302");
        //config.iceServers.add(iceServer);

        List<LocalCandidate> localCandidates = new ArrayList<>();
        try{
            localCandidates.add(new LocalCandidate(InetAddress.getByName("stun.l.google.com"), 19302));
        }catch(UnknownHostException failure){

        }

        //Create the RTC manager
        this.rtcManager = new RTCManager(localCandidates, serverBackend, this);

        serverBackend.getOnClose().addListener(this::onStop);
    }

    /**
     * @return if this WebSpeakServer is currently running
     */
    public boolean isRunning(){
        return serverBackend.isOpen();
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

    public Event<WebSpeakServer> getOnStop() {
        return onStop;
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
    public synchronized boolean addAudioSource(@NonNull AudioSource3D audioSource, @NonNull String audioId) {
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
    public synchronized @Nullable AudioSource3D removeAudioSource(String audioId) {
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
    public synchronized @Nullable String removeAudioSource(AudioSource3D audioSource) {
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
    public synchronized boolean addPlayer(@NonNull WebSpeakPlayer player, @NonNull String sessionId) throws IllegalArgumentException {
        if (player.getServer() != this) {
            throw new IllegalArgumentException("Player belongs to the wrong server!");
        }
        if (players.putIfAbsent(sessionId, player) == null) {
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
    public synchronized CompletableFuture<String> addPlayer(WebSpeakPlayer player) {
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
    public synchronized @Nullable WebSpeakPlayer removePlayer(String sessionId, String reason) {
        var player = players.remove(sessionId);
        if (player != null) {
            handleRemovedPlayer(player, sessionId, reason);
        }
        return player;
    }

    /**
     * Remove a player from the server.
     *
     * @param player Player to remove.
     * @return The ID the player had; <code>null</code> if it was not in this server.
     */
    public synchronized @Nullable String removePlayer(WebSpeakPlayer player, String reason) {
        var id = players.inverse().remove(player);
        if (id != null) {
            handleRemovedPlayer(player, id, reason);
        }
        return id;
    }

    private void handleRemovedPlayer(WebSpeakPlayer player, String sessionId, String reason) {
        serverBackend.releaseSessionId(sessionId, reason);
        onPlayerRemoved.invoke(new PlayerEvent(player, sessionId));
    }

    /**
     * Tick webspeak.
     */
    public synchronized void tick() {
        inTick = true;
        tickThread = Thread.currentThread();

        onStartTick.invoke(this);

        serverBackend.tick();

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

    /**
     * Stop the server.
     */
    public final synchronized CompletableFuture<?> stop() {
        // Event listener registered in constructor will call onStop
        serverBackend.close();
        return shutdownFuture;
    }

    /**
     * Called when the server backend has closed.
     * @param reason Reason the server was closed.
     */
    protected void onStop(String reason) {
        // TODO: close logic
        onStop.invoke(this);
        shutdownFuture.complete(null);
    }
}
