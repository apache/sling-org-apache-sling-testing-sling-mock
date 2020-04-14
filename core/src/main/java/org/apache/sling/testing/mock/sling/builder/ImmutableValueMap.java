/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.sling.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * {@link ValueMap} that does not support changing its content.
 * <p>
 * All methods that may change the content will throw a
 * {@link UnsupportedOperationException}.
 * </p>
 * <p>
 * Static convenience methods provide similar behavior as Guava ImmutableMap
 * variants.
 * </p>
 */
@ProviderType
public final class ImmutableValueMap implements ValueMap {

    private final ValueMap map;

    /**
     * @param map Value map
     */
    ImmutableValueMap(@NotNull ValueMap map) {
        this.map = map;
    }

    /**
     * @param map Map
     */
    ImmutableValueMap(@NotNull Map<String, Object> map) {
        this.map = new ValueMapDecorator(map);
    }

    @Override
    public @Nullable <T> T get(@NotNull String name, @NotNull Class<T> type) {
        return this.map.get(name, type);
    }

    @Override
    public <T> T get(@NotNull String name, T defaultValue) {
        return this.map.get(name, defaultValue);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return this.map.get(key);
    }

    @Override
    public Set<String> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(this.map.entrySet());
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ImmutableValueMap)) {
            return false;
        }
        return this.map.entrySet().equals(((ImmutableValueMap) obj).map.entrySet());
    }

    @Override
    public String toString() {
        return map.toString();
    }

    // mutable operations not supported
    /**
     * @deprecated Unsupported operation
     */
    @Override
    @Deprecated
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation
     */
    @Override
    @Deprecated
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation
     */
    @Override
    @Deprecated
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Unsupported operation
     */
    @Override
    @Deprecated
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the empty map. This map behaves and performs comparably to
     * {@link Collections#emptyMap}, and is preferable mainly for consistency
     * and maintainability of your code.
     * @return ImmutableValueMap
     */
    public static @NotNull ImmutableValueMap of() {
        return new ImmutableValueMap(ValueMap.EMPTY);
    }

    /**
     * Returns an immutable map containing a single entry. This map behaves and
     * performs comparably to {@link Collections#singletonMap} but will not
     * accept a null key or value. It is preferable mainly for consistency and
     * maintainability of your code.
     * @param k1 Key 1
     * @param v1 Value 1
     * @return ImmutableValueMap
     */
    public static @NotNull ImmutableValueMap of(@NotNull String k1, @NotNull Object v1) {
        return new ImmutableValueMap(Collections.singletonMap(k1, v1));
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     * @param k1 Key 1
     * @param v1 Value 1
     * @param k2 Key 2
     * @param v2 Value 2
     * @return ImmutableValueMap
     * @throws IllegalArgumentException if duplicate keys are provided
     */
    public static @NotNull ImmutableValueMap of(@NotNull String k1, @NotNull Object v1, @NotNull String k2,
            @NotNull Object v2) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return new ImmutableValueMap(map);
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     * @param k1 Key 1
     * @param v1 Value 1
     * @param k2 Key 2
     * @param v2 Value 2
     * @param k3 Key 3
     * @param v3 Value 3
     * @return ImmutableValueMap
     * @throws IllegalArgumentException if duplicate keys are provided
     */
    public static @NotNull ImmutableValueMap of(@NotNull String k1, @NotNull Object v1, @NotNull String k2,
            @NotNull Object v2, @NotNull String k3, @NotNull Object v3) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return new ImmutableValueMap(map);
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     * @param k1 Key 1
     * @param v1 Value 1
     * @param k2 Key 2
     * @param v2 Value 2
     * @param k3 Key 3
     * @param v3 Value 3
     * @param k4 Key 4
     * @param v4 Value 4
     * @return ImmutableValueMap
     * @throws IllegalArgumentException if duplicate keys are provided
     */
    public static @NotNull ImmutableValueMap of( // NOPMD
            @NotNull String k1, @NotNull Object v1, @NotNull String k2, @NotNull Object v2, @NotNull String k3,
            @NotNull Object v3, @NotNull String k4, @NotNull Object v4) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return new ImmutableValueMap(map);
    }

    /**
     * Returns an immutable map containing the given entries, in order.
     * @param k1 Key 1
     * @param v1 Value 1
     * @param k2 Key 2
     * @param v2 Value 2
     * @param k3 Key 3
     * @param v3 Value 3
     * @param k4 Key 4
     * @param v4 Value 4
     * @param k5 Key 5
     * @param v5 Value 5
     * @return ImmutableValueMap
     * @throws IllegalArgumentException if duplicate keys are provided
     */
    public static ImmutableValueMap of( // NOPMD
            @NotNull String k1, @NotNull Object v1, @NotNull String k2, @NotNull Object v2, @NotNull String k3,
            @NotNull Object v3, @NotNull String k4, @NotNull Object v4, @NotNull String k5, @NotNull Object v5) {
        Map<String, Object> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        map.put(k5, v5);
        return new ImmutableValueMap(map);
    }

    // looking for of() with > 5 entries? Use the builder instead.

    /**
     * Returns a new builder. The generated builder is equivalent to the builder
     * created by the {@link Builder} constructor.
     * @return Builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Returns an immutable map containing the same entries as {@code map}. If
     * {@code map} somehow contains entries with duplicate keys (for example, if
     * it is a {@code SortedMap} whose comparator is not <i>consistent with
     * equals</i>), the results of this method are undefined.
     * <p>
     * Despite the method name, this method attempts to avoid actually copying
     * the data when it is safe to do so. The exact circumstances under which a
     * copy will or will not be performed are undocumented and subject to
     * change.
     * </p>
     * @param map Map
     * @return ImmutableValueMap
     * @throws NullPointerException if any key or value in {@code map} is null
     */
    public static @NotNull ImmutableValueMap copyOf(@NotNull Map<String, Object> map) {
        if (map instanceof ValueMap) {
            return new ImmutableValueMap((ValueMap) map);
        } else {
            return new ImmutableValueMap(map);
        }
    }

    /**
     * Builder interface for {@link ImmutableValueMap}.
     */
    public static final class Builder {

        private final @NotNull Map<String, Object> map = new HashMap<>();

        /**
         * Associates {@code key} with {@code value} in the built map. Duplicate
         * keys are not allowed, and will cause {@link #build} to fail.
         * @param key Key
         * @param value value
         * @return this
         */
        public @NotNull Builder put(@NotNull String key, @NotNull Object value) {
            map.put(key, value);
            return this;
        }

        /**
         * Adds the given {@code entry} to the map, making it immutable if
         * necessary. Duplicate keys are not allowed, and will cause
         * {@link #build} to fail.
         * @param entry Entry
         * @return this
         */
        public @NotNull Builder put(@NotNull Entry<String, Object> entry) {
            return put(entry.getKey(), entry.getValue());
        }

        /**
         * Associates all of the given map's keys and values in the built map.
         * Duplicate keys are not allowed, and will cause {@link #build} to
         * fail.
         * @param value Value
         * @return this
         * @throws NullPointerException if any key or value in {@code map} is
         *             null
         */
        public @NotNull Builder putAll(@NotNull Map<String, Object> value) {
            map.putAll(value);
            return this;
        }

        /**
         * Returns a newly-created immutable map.
         * @return ImmutableValueMap
         * @throws IllegalArgumentException if duplicate keys were added
         */
        public @NotNull ImmutableValueMap build() {
            if (map.isEmpty()) {
                return ImmutableValueMap.of();
            } else {
                return new ImmutableValueMap(map);
            }
        }
    }

}
