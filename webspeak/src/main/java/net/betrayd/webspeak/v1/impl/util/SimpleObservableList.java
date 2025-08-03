package net.betrayd.webspeak.v1.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public class SimpleObservableList<T> extends SimpleObservableCollection<T> implements List<T> {

    private final List<T> base;

    public SimpleObservableList(List<T> baseList) {
        super(baseList);
        this.base = baseList;
    }
    
    public SimpleObservableList() {
        this(new ArrayList<>());
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (base.addAll(index, c)) {
            ON_ADDED.invoker().accept(c);
            return true;
        }
        return false;
    }

    @Override
    public T get(int index) {
        return base.get(index);
    }

    @Override
    public T set(int index, T element) {
        T oldVal = base.set(index, element);
        if (oldVal != element) {
            onRemoved(oldVal);
            onAdded(element);
        }
        return oldVal;
    }

    @Override
    public void add(int index, T element) {
        base.add(index, element);
        onAdded(element);
    }

    @Override
    public T remove(int index) {
        T oldVal = base.remove(index);
        onRemoved(oldVal);
        return oldVal;
    }

    @Override
    public int indexOf(Object o) {
        return base.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return base.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new ObservableListIterator(base.listIterator());
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new ObservableListIterator(base.listIterator(index));
    }
    
    private void onSublistAdded(Collection<? extends T> vals) {
        ON_ADDED.invoker().accept(vals);
    }

    private void onSublistRemoved(Collection<? extends T> vals) {
        ON_REMOVED.invoker().accept(vals);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        var sub = new SimpleObservableList<>(base.subList(fromIndex, toIndex));
        sub.ON_ADDED.addListener(this::onSublistAdded);
        sub.ON_REMOVED.addListener(this::onSublistRemoved);
        return sub;
    }

    private class ObservableListIterator implements ListIterator<T> {

        final ListIterator<T> baseIterator;

        ObservableListIterator(ListIterator<T> baseIterator) {
            this.baseIterator = baseIterator;
        }
        private T val;

        @Override
        public boolean hasNext() {
            return baseIterator.hasNext();
        }

        @Override
        public T next() {
            val = baseIterator.next();
            return val;
        }

        @Override
        public boolean hasPrevious() {
            return baseIterator.hasPrevious();
        }

        @Override
        public T previous() {
            val = baseIterator.previous();
            return val;
        }

        @Override
        public int nextIndex() {
            return baseIterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return baseIterator.previousIndex();
        }

        @Override
        public void remove() {
            baseIterator.remove();
            onRemoved(val);
        }

        @Override
        public void set(T e) {
            var prevVal = val;
            baseIterator.set(e);
            val = e;
            if (prevVal != val) {
                onRemoved(prevVal);
                onAdded(val);
            }
        }

        @Override
        public void add(T e) {
            baseIterator.add(e);
            onAdded(e);
        }
        
    }
}
