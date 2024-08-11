package com.michaelsebero.portium.modern.shim;

import net.minecraft.util.registry.IRegistry;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** 1.12's simple registries (the baked model registry, say) as the maps 1.13 has in their place. */
public final class RegistryMap<K, V> extends AbstractMap<K, V> {

    private final IRegistry<K, V> registry;

    public RegistryMap(IRegistry<K, V> registry) {
        this.registry = registry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        return this.registry.getObject((K) key);
    }

    @Override
    public V put(K key, V value) {
        V old = this.registry.getObject(key);
        this.registry.putObject(key, value);
        return old;
    }

    @Override
    public boolean containsKey(Object key) {
        return this.registry.getKeys().contains(key);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                Iterator<K> keys = RegistryMap.this.registry.getKeys().iterator();
                return new Iterator<Entry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        return keys.hasNext();
                    }

                    @Override
                    public Entry<K, V> next() {
                        K key = keys.next();
                        return new SimpleEntry<K, V>(key, RegistryMap.this.registry.getObject(key)) {
                            @Override
                            public V setValue(V value) {
                                RegistryMap.this.registry.putObject(key, value);
                                return super.setValue(value);
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return RegistryMap.this.registry.getKeys().size();
            }
        };
    }
}
