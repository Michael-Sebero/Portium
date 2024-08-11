package com.michaelsebero.portium.modern.mc.util.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** One of 1.13's registries that 1.12 has no counterpart of: names and numbers for what 1.13 code registers. */
public class SimpleRegistry<T> implements IRegistry<T> {

    @Nullable
    private final ResourceLocation defaultKey;
    private final BiMap<ResourceLocation, T> byName = HashBiMap.create();
    private final List<T> byId = new ArrayList<>();
    private final Map<T, Integer> ids = new HashMap<>();

    public SimpleRegistry(@Nullable ResourceLocation defaultKey) {
        this.defaultKey = defaultKey;
    }

    @Nullable
    @Override
    public synchronized ResourceLocation getKey(T value) {
        ResourceLocation key = this.byName.inverse().get(value);
        return key != null ? key : this.defaultKey;
    }

    @Override
    public synchronized T get(@Nullable ResourceLocation name) {
        T value = name == null ? null : this.byName.get(name);
        return value != null || this.defaultKey == null ? value : this.byName.get(this.defaultKey);
    }

    @Override
    public ResourceLocation getDefaultKey() {
        return this.defaultKey;
    }

    @Override
    public synchronized int getId(@Nullable T value) {
        Integer id = value == null ? null : this.ids.get(value);
        return id == null ? -1 : id;
    }

    @Nullable
    @Override
    public synchronized T get(int id) {
        return id >= 0 && id < this.byId.size() ? this.byId.get(id) : null;
    }

    @Override
    public synchronized Iterator<T> iterator() {
        List<T> values = new ArrayList<>(this.byId);
        values.removeIf(value -> value == null);
        return values.iterator();
    }

    @Nullable
    @Override
    public synchronized T getNullable(@Nullable ResourceLocation name) {
        return name == null ? null : this.byName.get(name);
    }

    @Override
    public synchronized void register(int id, ResourceLocation name, T value) {
        while (this.byId.size() <= id) this.byId.add(null);
        this.byId.set(id, value);
        this.ids.put(value, id);
        this.byName.forcePut(name, value);
    }

    @Override
    public synchronized void put(ResourceLocation name, T value) {
        Integer existing = this.ids.get(value);
        register(existing != null ? existing : this.byId.size(), name, value);
    }

    @Override
    public synchronized Set<ResourceLocation> keySet() {
        return Collections.unmodifiableSet(new java.util.HashSet<>(this.byName.keySet()));
    }

    @Override
    public synchronized boolean isEmpty() {
        return this.byName.isEmpty();
    }

    @Nullable
    @Override
    public synchronized T getRandom(Random random) {
        List<T> values = new ArrayList<>(this.byName.values());
        return values.isEmpty() ? null : values.get(random.nextInt(values.size()));
    }

    @Override
    public synchronized boolean containsKey(ResourceLocation name) {
        return this.byName.containsKey(name);
    }
}
