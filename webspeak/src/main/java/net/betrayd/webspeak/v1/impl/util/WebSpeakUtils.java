package net.betrayd.webspeak.v1.impl.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

public class WebSpeakUtils {
    public static record Pair<A, B>(A a, B b) {};

    /**
     * Compare every element in a collection with every other element in the
     * collection. No two elements are ever compared twice, assuming they don't
     * appear twice in the collection. If they do, behavior is undefined.
     * 
     * @param <T>        Collection type.
     * @param collection Collection to use.
     * @return An iterable that iterates over all item pairs in the collection. The
     *         order of the items in the pair is undefined.
     */
    public static <T> Iterable<Pair<T, T>> compareAll(Collection<? extends T> collection) {
        return () -> new ComparingIterator<>(collection, collection.size());
    }

    public static <T> Iterable<Pair<T, T>> compareAll(Iterable<? extends T> iterable, int expectedSize) {
        return () -> new ComparingIterator<>(iterable, expectedSize);
    }

    private static class ComparingIterator<T> implements Iterator<Pair<T, T>> {

        private final Iterable<? extends T> iterable;
        private final Set<T> testedVals;

        private Iterator<? extends T> iteratorA;
        private FilteredIterator<? extends T> iteratorB;

        private T aVal;

        ComparingIterator(Iterable<? extends T> iterable, int expectedSize) {
            this.iterable = iterable;
            testedVals = new HashSet<>(expectedSize, 1);
            iteratorA = iterable.iterator();
        }

        @Override
        public boolean hasNext() {
            return iteratorA.hasNext() || (iteratorB != null && iteratorB.hasNext());
        }

        @Override
        public Pair<T, T> next() {
            if (iteratorB != null && iteratorB.hasNext()) {
                return new Pair<>(aVal, iteratorB.next());
            } else {
                // Setup future iterations and then test against self.
                aVal = iteratorA.next();
                testedVals.add(aVal);
                iteratorB = new FilteredIterator<>(iterable.iterator(), val -> !testedVals.contains(val));
                return new Pair<>(aVal, aVal);
            }
        }
        
    }

    public static class FilteredIterator<T> implements Iterator<T> {
        private final Iterator<? extends T> baseIterator;
        private final Predicate<? super T> predicate;

        private T next;

        // base iterator could actually return a null value,
        // so we can't use a null check.
        private boolean isNextSet;
        
        public FilteredIterator(Iterator<? extends T> baseIterator, Predicate<? super T> predicate) {
            this.baseIterator = baseIterator;
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext() {
            return (isNextSet || findNext());
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            isNextSet = false;
            return next;
        }

        private boolean findNext() {
            while (baseIterator.hasNext()) {
                T obj = baseIterator.next();
                if (predicate.test(obj)) {
                    next = obj;
                    isNextSet = true;
                    return true;
                }
            }
            return false;
        }
    }
}
