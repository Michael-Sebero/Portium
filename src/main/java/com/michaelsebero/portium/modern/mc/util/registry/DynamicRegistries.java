package com.michaelsebero.portium.modern.mc.util.registry;

/**
 * 1.16's {@code net.minecraft.util.registry.DynamicRegistries}: a world's registries, asked for by key. Portium's
 * registries are global, so this only stands for the world they're asked through (the glue's {@code WorldGlue}
 * answers the asking).
 */
public class DynamicRegistries {

    public final Object world;

    public DynamicRegistries(Object world) {
        this.world = world;
    }
}
