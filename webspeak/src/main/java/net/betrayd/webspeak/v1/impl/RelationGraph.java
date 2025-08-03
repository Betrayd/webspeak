package net.betrayd.webspeak.v1.impl;

import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

public class RelationGraph<T> implements Iterable<Map.Entry<T, T>> {
    /**
     * A pair in which the order of the values doesn't matter for the purposes of
     * equality checks.
     */
    private static class UnorderedPair<T> {
        final T a;
        final T b;

        UnorderedPair(T a, T b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (obj instanceof UnorderedPair other) {
                return (Objects.equals(this.a, other.a) && Objects.equals(this.b, other.b))
                        || (Objects.equals(this.a, other.b) && Objects.equals(this.b, other.a));
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            // Commutitive property babyy
            return a.hashCode() + b.hashCode();
        }
    }

    private final Set<UnorderedPair<T>> relations = new HashSet<>();

    /**
     * Add a relation
     * @param a Value A
     * @param b Value B
     * @return If the relation did not already exist
     */
    public boolean add(T a, T b) {
        return relations.add(new UnorderedPair<>(a, b));
    }

    /**
     * Remove a relation between two values.
     * @param a Value A
     * @param b Value B
     * @return If the relation was there and could be removed.
     */
    public boolean remove(Object a, Object b) {
        return relations.remove(new UnorderedPair<>(a, b));
    }

    /**
     * Remove all relations connected to a specific object.
     * @param value Object to remove from the relation graph.
     * @return If any relation was found.
     */
    public boolean removeAll(Object value) {
        return relations.removeIf(pair -> Objects.equals(pair.a, value) || Objects.equals(pair.b, value));
    }

    /**
     * Check if a relation exists
     * @param a Value A
     * @param b Value B
     * @return If values A and B have a relation
     */
    public boolean containsRelation(Object a, Object b) {
        return relations.contains(new UnorderedPair<>(a, b));
    }

    /**
     * Get a collection of all values that a given value has a relation to.
     * @param value Value to check.
     * @return Unmodifiable set of all relations.
     */
    public Collection<T> getRelations(Object value) {
        List<T> list = new ArrayList<>();
        for (var pair : relations) {
            if (Objects.equals(value, pair.a)) {
                list.add(pair.b);
            } else if (Objects.equals(value, pair.b)) {
                list.add(pair.a);
            }
        }
        return list;
    }

    @Override
    public Iterator<Entry<T, T>> iterator() {
        return new RelationIterator();
    }

    public int size() {
        return relations.size();
    }

    private final RelationSet relationSet = new RelationSet();

    /**
     * Return a set of all relations in this graph as map entries. Updates to this graph will reflect in the set and vice-versa.
     * @return Relation set
     */
    public Set<Map.Entry<T,T>> relationSet() {
        return relationSet;
    }
    
    private class RelationSet extends AbstractSet<Map.Entry<T,T>> {

        @Override
        public int size() {
            return relations.size();
        }

        @Override
        public Iterator<Entry<T, T>> iterator() {
            return new RelationIterator();
        }
        
        @Override
        public boolean add(Entry<T, T> e) {
            return RelationGraph.this.add(e.getKey(), e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Map.Entry entry) {
                return RelationGraph.this.remove(entry.getKey(), entry.getValue());
            } else {
                return false;
            }
        }
    }

    private class RelationIterator implements Iterator<Map.Entry<T, T>> {
        final Iterator<UnorderedPair<T>> baseIterator = relations.iterator();

        @Override
        public boolean hasNext() {
            return baseIterator.hasNext();
        }

        @Override
        public Entry<T, T> next() {
            var next = baseIterator.next();
            return new SimpleEntry<>(next.a, next.b);
        }

        @Override
        public void remove() {
            baseIterator.remove();
        }
    }
}
