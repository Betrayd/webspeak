package net.betrayd.webspeak.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface Event<T> {
    void addListener(Consumer<T> listener);
    boolean removeListener(Object listener);

    interface Invokable<T> extends Event<T> {
        void invoke(T value);
    }

    static <T> Event.Invokable<T> create() {
        return new SimpleEvent<>();
    }
}

class SimpleEvent<T> implements Event.Invokable<T> {

    private final List<Consumer<T>> listeners = new ArrayList<>();

    @Override
    public synchronized void invoke(T value) {
        for (var l : listeners) {
            l.accept(value);
        }
    }

    @Override
    public synchronized void addListener(Consumer<T> listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized boolean removeListener(Object listener) {
        return listeners.remove(listener);
    }
}