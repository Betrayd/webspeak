package net.betrayd.webspeak;

import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.event.ServerEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

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

    private final Map<String, T> players = new HashMap<>();
    private final Map<String, T> playersUnmodifiable = Collections.unmodifiableMap(players);

    public WebSpeakServer(ServerBackend backend) {
        this.backend = backend;
    }

    @Nullable
    public T getPlayer(String id) {
        return players.get(id);
    }

    /**
     * Get a map of all player IDs in the server with their corresponding players.
     * @return Unmodifiable player map.
     */
    public Map<String, T> getPlayers() {
        return playersUnmodifiable;
    }

    /**
     * Attempt to add a player to the server.
     *
     * @param player Player to add.
     * @return <code>true</code> if the player was added; <code>false</code> if there was already a player with that ID.
     * @throws IllegalArgumentException If the player belongs to the wrong server.
     */
    public boolean addPlayer(T player) throws IllegalArgumentException {
        if (player.getServer() != this) {
            throw new IllegalArgumentException("Player belongs to the wrong server!");
        }
        return players.putIfAbsent(player.getPlayerId(), player) == null;
    }

    public interface PlayerFactory<T extends WebSpeakPlayer> {
        T create(WebSpeakServer<T> server, String playerId, String sessionId);
    }

    /**
     * Generate a session ID, create a player, and add it to the server.
     *
     * @param id      Player ID to assign. Must not already be present in map.
     * @param factory Factory function to create the player (called <em>after</em> the session ID is created).
     * @return A future that completes with the new player if it was successfully added.
     */
    public CompletableFuture<WebSpeakPlayer> createPlayer(String id, PlayerFactory<T> factory) {
        return backend.requestSessionID().thenApplyAsync(session -> {
            T player = factory.create(this, id, session);
            if (!addPlayer(player)) {
                throw new IllegalStateException("A player already exists with that ID.");
            }
            return player;
        }, this);
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

        executeTasks();

        endTick();
        inTick = false;
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
