/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the 
 * terms of the Apache License, Version 2.0 which is available at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0 
 ********************************************************************************/
package ceylon.modules.jboss.runtime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple graph impl.
 *
 * @param <K> exact key type
 * @param <V> exact vertex value type
 * @param <E> exact edge cost type
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class Graph<K, V, E> {
    private ConcurrentMap<K, Vertex<V, E>> vertices = new ConcurrentHashMap<K, Vertex<V, E>>();

    public Vertex<V, E> createVertex(K key, V value) {
        Vertex<V, E> v = new Vertex<V, E>(value);
        Vertex<V, E> previous = vertices.putIfAbsent(key, v);
        return (previous != null) ? previous : v;
    }

    public Vertex<V, E> getVertex(K key) {
        return vertices.get(key);
    }

    public Collection<Vertex<V, E>> getVertices() {
        return vertices != null ? Collections.unmodifiableCollection(vertices.values()) : Collections.<Vertex<V, E>>emptySet();
    }

    public static class Vertex<V, E> {
        private V value;
        private Set<Edge<V, E>> in;
        private Set<Edge<V, E>> out;

        Vertex(V value) {
            this.value = value;
        }

        public V getValue() {
            return value;
        }

        private void addIn(Edge<V, E> edge) {
            if (in == null)
                in = new HashSet<Edge<V, E>>();

            in.add(edge);
        }

        private void addOut(Edge<V, E> edge) {
            if (out == null)
                out = new HashSet<Edge<V, E>>();

            out.add(edge);
        }

        public Set<Edge<V, E>> getIn() {
            return in != null ? Collections.unmodifiableSet(in) : Collections.<Edge<V, E>>emptySet();
        }

        public Set<Edge<V, E>> getOut() {
            return out != null ? Collections.unmodifiableSet(out) : Collections.<Edge<V, E>>emptySet();
        }

        public int hashCode() {
            return value.hashCode();
        }

        @SuppressWarnings({"unchecked"})
        public boolean equals(Object obj) {
            if (obj instanceof Vertex == false)
                return false;

            Vertex<V, E> other = (Vertex<V, E>) obj;
            return value.equals(other.value);
        }
    }

    public static class Edge<V, E> {
        private E cost;
        private Vertex<V, E> from;
        private Vertex<V, E> to;

        public static <V, E> void create(E cost, Vertex<V, E> from, Vertex<V, E> to) {
            new Edge<V, E>(cost, from, to);
        }

        private Edge(E cost, Vertex<V, E> from, Vertex<V, E> to) {
            if (from == null)
                throw new IllegalArgumentException("Null from");
            if (to == null)
                throw new IllegalArgumentException("Null to");

            this.cost = cost;
            this.from = from;
            this.to = to;

            from.addOut(this);
            to.addIn(this);
        }

        public E getCost() {
            return cost;
        }

        public Vertex<V, E> getFrom() {
            return from;
        }

        public Vertex<V, E> getTo() {
            return to;
        }
    }
}
