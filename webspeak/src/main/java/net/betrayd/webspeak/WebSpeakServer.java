package net.betrayd.webspeak;

import lombok.Getter;
import lombok.NonNull;
import net.betrayd.webspeak.event.WebSpeakEvent;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

public class WebSpeakServer implements Executor {

    public final WebSpeakEvent<Runnable> ON_START_TICK = WebSpeakEvent.createNoArg();
    public final WebSpeakEvent<Runnable> ON_END_TICK = WebSpeakEvent.createNoArg();

    /**
     * If we're currently in the middle of a server tick
     */
    @Getter
    private volatile boolean inTick;

    private final ConcurrentLinkedQueue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public final void tick() {
        inTick = true;
        startTick();

        executeTasks();

        endTick();
        inTick = false;
    }

    protected void startTick() {
        ON_START_TICK.invoker().run();
    }

    protected void endTick() {
        ON_END_TICK.invoker().run();
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
    public void execute(@NonNull Runnable command) {
        if (inTick) {
            command.run();
        } else {
            tasks.add(command);
        }
    }
}
