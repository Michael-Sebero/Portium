package com.michaelsebero.portium.modern;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * How to make the tile entities of newer mods, by class: from their tile entity type's factory. 1.12 remakes a saved
 * tile entity with its class's constructor taking nothing, which 1.16's tile entities (taking their type) lack.
 */
public final class TileEntityFactories {

    private static final Map<Class<?>, Supplier<?>> FACTORIES = new ConcurrentHashMap<>();

    public static void register(Class<?> tileClass, Supplier<?> factory) {
        FACTORIES.putIfAbsent(tileClass, factory);
    }

    /** A new tile entity of the class: by its type's factory if it has one, else by its constructor taking nothing. */
    public static Object make(Class<?> tileClass) throws InstantiationException, IllegalAccessException {
        Supplier<?> factory = FACTORIES.get(tileClass);
        return factory != null ? factory.get() : tileClass.newInstance();
    }

    private TileEntityFactories() {}
}
