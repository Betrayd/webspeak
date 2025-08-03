package net.betrayd.webspeak.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface WebSpeakEvent<T> {
    void addListener(T listener);
    boolean removeListener(T listener);
    T invoker();

    static <T> WebSpeakEvent<Consumer<T>> createSimple() {
        return new SimpleEvent<>();
    }

    static WebSpeakEvent<Runnable> createNoArg() {
        return new NoArgEvent();
    }

    static <T> WebSpeakEvent<T> createArrayBacked(Function<List<T>, T> invokerFactory) {
        return new ArrayBackedEvent<>(invokerFactory);
    }
}

class SimpleEvent<T> implements WebSpeakEvent<Consumer<T>> {

    final List<Consumer<T>> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addListener(Consumer<T> listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(Consumer<T> listener) {
        return listeners.remove(listener);
    }

    @Override
    public Consumer<T> invoker() {
        return val -> {
            // Make copy for thread safety and in case a listener tries to modify list.
            for (var l : List.copyOf(listeners)) {
                l.accept(val);
            }
        };
    }
}

class NoArgEvent implements WebSpeakEvent<Runnable> {
    final List<Runnable> listeners = Collections.synchronizedList(new ArrayList<>());


    @Override
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    @Override
    public boolean removeListener(Runnable listener) {
        return listeners.remove(listener);
    }

    @Override
    public Runnable invoker() {
        return () -> {
            for (var l : listeners) {
                l.run();
            }
        };
    }
}

class ArrayBackedEvent<T> implements WebSpeakEvent<T> {
    final Function<List<T>, T> invokerFactory;
    final List<T> listeners = Collections.synchronizedList(new ArrayList<>());
    T invoker;

    public ArrayBackedEvent(Function<List<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        invoker = invokerFactory.apply(List.of());
    }

    @Override
    public synchronized void addListener(T listener) {
        listeners.add(listener);
        // Remember the lambda of the invoker is going to store this.
        // Make a copy in case a listener tries to modify the list.
        invoker = invokerFactory.apply(List.copyOf(listeners));
    }

    @Override
    public synchronized boolean removeListener(T listener) {
        boolean success = listeners.remove(listener);
        if (success) {
            invoker = invokerFactory.apply(List.copyOf(listeners));
        }
        return success;
    }

    @Override
    public T invoker() {
        return invoker;
    }
}