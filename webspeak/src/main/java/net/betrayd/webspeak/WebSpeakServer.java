package net.betrayd.webspeak;

import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * The main server for WebSpeak. Responsible for keeping track of players, managing coordinate updates,
 * and facilitating everything else that WebSpeak needs to do.
 */
public class WebSpeakServer implements Executor {

    public WebSpeakServer(@NotNull ServerBackend serverBackend) {
        this.serverBackend = serverBackend;
    }

    // EVENTS

    private final Event.Invokable<WebSpeakServer> onStartTick = Event.create();
    private final Event.Invokable<WebSpeakServer> onEndTick = Event.create();

    public Event<WebSpeakServer> getOnStartTick() {
        return onStartTick;
    }

    public Event<WebSpeakServer> getOnEndTick() {
        return onEndTick;
    }

    // PLAYERS & COMMUNICATION

    /**
     * Manages primary communication between the browsers and WebSpeak
     */
    @Getter @NonNull
    private final ServerBackend serverBackend;

    // TICKING & LIFECYCLE

    private volatile boolean inTick;
    private volatile Thread tickThread;
    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    /**
     * Tick webspeak.
     */
    public synchronized void tick() {
        inTick = true;
        tickThread = Thread.currentThread();

        onStartTick.invoke(this);

        // execute tasks
        Runnable command;
        while ((command = tasks.poll()) != null) {
            command.run();
        }

        onEndTick.invoke(this);
    }

    /**
     * Check if the current thread is currently executing a webspeak tick.
     */
    public boolean isInTick() {
        return inTick && Thread.currentThread().equals(tickThread);
    }

    /**
     * Queue a command to be executed during the next tick.
     * If we're in the middle of a tick, execute it now.
     * @param command the runnable task
     */
    @Override
    public void execute(@NonNull Runnable command) {
        if (isInTick())
            command.run();
        else
            tasks.add(command);
    }
}
