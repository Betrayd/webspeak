package net.betrayd.webspeak.v1.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.betrayd.webspeak.v1.util.WebSpeakEvents;
import net.betrayd.webspeak.v1.util.WebSpeakEvents.WebSpeakEvent;

/**
 * A collection that calls events when it is modified.
 */
public class SimpleObservableCollection<T> implements Collection<T> {

    public final WebSpeakEvent<Consumer<Collection<? extends T>>> ON_ADDED = WebSpeakEvents.createSimple();
    public final WebSpeakEvent<Consumer<Collection<? extends T>>> ON_REMOVED = WebSpeakEvents.createSimple();

    protected final void onAdded(T val) {
        ON_ADDED.invoker().accept(Collections.singleton(val));
    }

    protected final void onRemoved(T val) {
        ON_REMOVED.invoker().accept(Collections.singleton(val));
    }

    private final Collection<T> base;

    public SimpleObservableCollection(Collection<T> baseCollection) {
        this.base = baseCollection;
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return base.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new ObservableIterator(base.iterator());
    }

    @Override
    public Object[] toArray() {
        return base.toArray();
    }

    @Override
    public <U> U[] toArray(U[] a) {
        return base.toArray(a);
    }

    @Override
    public boolean add(T e) {
        if (base.add(e)) {
            onAdded(e);
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked") // base.remove only returns true on correct type.
    public boolean remove(Object o) {
        if (base.remove(o)) {
            onRemoved((T) o);
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return base.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (base.addAll(c)) {
            ON_ADDED.invoker().accept(c);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        List<T> removed = new ArrayList<>(size());
        Iterator<T> it = base.iterator();
        while (it.hasNext()) {
            T val = it.next();
            if (c.contains(val)) {
                it.remove();
                removed.add(val);
            }
        }
        
        if (!removed.isEmpty()) {
            ON_REMOVED.invoker().accept(removed);
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        List<T> removed = new ArrayList<>(size());
        Iterator<T> it = base.iterator();
        while (it.hasNext()) {
            T val = it.next();
            if (!c.contains(val)) {
                it.remove();
                removed.add(val);
            }
        }
        
        if (!removed.isEmpty()) {
            ON_REMOVED.invoker().accept(removed);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        ON_REMOVED.invoker().accept(base);
        base.clear();
    }
    
    private class ObservableIterator implements Iterator<T> {
        final Iterator<T> baseIterator;

        T currentVal;

        ObservableIterator(Iterator<T> baseIterator) {
            this.baseIterator = baseIterator;
        }
        @Override
        public boolean hasNext() {
            return baseIterator.hasNext();
        }
        @Override
        public T next() {
            currentVal = baseIterator.next();
            return currentVal;
        }

        @Override
        public void remove() {
            baseIterator.remove();
            onRemoved(currentVal);
        }
    }
}
