package net.betrayd.webspeak.v1.impl.util;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import net.betrayd.webspeak.v1.util.WebSpeakEvents;
import net.betrayd.webspeak.v1.util.WebSpeakEvents.WebSpeakEvent;

public class ObservableSet<T> extends AbstractSet<T> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private ClosableLock lockRead() {
        return ClosableLock.lock(lock.readLock());
    }
    private ClosableLock lockWrite() {
        return ClosableLock.lock(lock.writeLock());
    }

    private final Set<T> base;

    public ObservableSet(Set<T> base) {
        if (base == null) {
            throw new NullPointerException("base");
        }
        this.base = base;
    }

    public final WebSpeakEvent<Consumer<T>> ON_ADDED = WebSpeakEvents.createSimple();
    public final WebSpeakEvent<Consumer<T>> ON_REMOVED = WebSpeakEvents.createSimple();

    @Override
    public int size() {
        try (var lock = lockRead()) {
            return base.size();
        }
    }

    @Override
    public boolean isEmpty() {
        try (var lock = lockRead()) {
            return base.isEmpty();
        }
    }

    @Override
    public boolean contains(Object o) {
        try (var lock = lockRead()) {
            return base.contains(o);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new ObservableIterator(base.iterator());
    }

    @Override
    public Object[] toArray() {
        try (var lock = lockRead()) {
            return base.toArray();
        }
    }

    @Override
    public <F> F[] toArray(F[] a) {
        try (var lock = lockRead()) {
            return base.toArray(a);
        }
    }

    @Override
    public <F> F[] toArray(IntFunction<F[]> generator) {
        try (var lock = lockRead()) {
            return base.toArray(generator);
        }
    }

    @Override
    public boolean add(T e) {
        boolean added;
        try (var lock = lockWrite()) {
            added = base.add(e);
        }
        if (added) {
            ON_ADDED.invoker().accept(e);
        }
        return added;
    }

    @Override
    @SuppressWarnings("unchecked") // remove is only successful if it's the right type
    public boolean remove(Object o) {
        boolean removed;
        try (var lock = lockWrite()) {
            removed = base.remove(o);
        }
        if (removed) {
            ON_REMOVED.invoker().accept((T) o);
        }
        return removed;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        try (var lock = lockRead()) {
            return base.containsAll(c);
        }
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        List<T> added = new ArrayList<>(c.size());

        try (var lock = lockWrite()) {
            for (var item : c) {
                if (base.add(item)) {
                    added.add(item);
                }
            }
        }
        
        for (var item : added) {
            ON_ADDED.invoker().accept(item);
        }

        return !added.isEmpty();
    }

    @Override
    @SuppressWarnings("unchecked") // Item only in collection if it's the correct type.
    public void clear() {
        Object[] items = base.toArray();
        try (var lock = lockWrite()) {
            base.clear();
        }
        for (var item : items) {
            ON_REMOVED.invoker().accept((T) item);
        }
    }

    private class ObservableIterator implements Iterator<T> {
        final Iterator<T> base;
        T currentVal;

        ObservableIterator(Iterator<T> base) {
            this.base = base;
        }

        @Override
        public boolean hasNext() {
            return base.hasNext();
        }

        @Override
        public T next() {
            currentVal = base.next();
            return currentVal;
        }

        @Override
        public void remove() {
            base.remove();
            ON_REMOVED.invoker().accept(currentVal);
        }
    }
}
