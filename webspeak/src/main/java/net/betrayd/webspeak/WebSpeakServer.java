package net.betrayd.webspeak;

import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.event.ServerEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class WebSpeakServer implements Executor {

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

    /**
     * If we're currently in the middle of a server tick
     */
    @Getter
    private volatile boolean inTick;

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private final Map<String, WebSpeakPlayer> players = new HashMap<>();
    private final Map<String, WebSpeakPlayer> playersUnmodifiable = Collections.unmodifiableMap(players);

    public WebSpeakServer(ServerBackend backend) {
        this.backend = backend;
    }

    @Nullable
    public WebSpeakPlayer getPlayer(String id) {
        return players.get(id);
    }

    public boolean addPlayer(WebSpeakPlayer player) {
        if (player.getServer() != this) {
            throw new IllegalArgumentException("Player belongs to the wrong server!");
        }
        return players.put(player.getPlayerId(), player) != null;
    }

    /**
     * Get a map of all player IDs in the server with their corresponding player.
     * @return Unmodifiable player map.
     */
    public Map<String, WebSpeakPlayer> getPlayers() {
        return playersUnmodifiable;
    }

    public final void tick() {
        inTick = true;
        startTick();

        executeTasks();

        endTick();
        inTick = false;
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
     */
    @Override
    public final void execute(@NonNull Runnable command) {
        if (inTick) {
            command.run();
        } else {
            tasks.add(command);
        }
    }

    /**
     * Execute a command during the next server tick (even if we're currently ticking)
     * @param command the runnable task
     */
    public final void executeDeferred(@NonNull Runnable command) {
        tasks.add(command);
    }
}
