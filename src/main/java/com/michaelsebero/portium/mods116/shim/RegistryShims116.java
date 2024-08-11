package com.michaelsebero.portium.mods116.shim;

import net.minecraft.util.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Forge 1.16's object holder refreshes, which its {@code RegistryObject}s listen for. 1.12's object holder registry
 * has no such listeners: they're kept here, and Portium's glue calls them once 1.16 mods have registered into a
 * registry, as Forge 1.16 does after each registry's event.
 */
public final class RegistryShims116 {

    private static final List<Consumer<Predicate<ResourceLocation>>> HANDLERS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Predicate<ResourceLocation>>> FAILED = new CopyOnWriteArrayList<>();
    private static final java.util.Set<String> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5");

    /** Forge 1.16's {@code ObjectHolderRegistry.addHandler}. */
    public static void addHandler(Consumer<Predicate<ResourceLocation>> handler) {
        HANDLERS.add(handler);
    }

    /** Lets the listeners refresh what they hold from the registry named {@code registry}. */
    public static void refresh(ResourceLocation registry) {
        Predicate<ResourceLocation> filter = registry::equals;
        for (Consumer<Predicate<ResourceLocation>> handler : HANDLERS) {
            try {
                handler.accept(filter);
            } catch (RuntimeException e) {
                // A holder for a registry Portium doesn't have yet (Forge 1.16 throws then); the rest still refresh.
                FAILED.add(handler);
                if (REPORTED.add(String.valueOf(e.getMessage()))) LOGGER.warn("A 1.16 registry object couldn't refresh: {}", e.getMessage());
            }
        }
        HANDLERS.removeAll(FAILED);
        // Then what waits on registered objects.
        com.michaelsebero.portium.mods116.adapter.StairsBlock116.resolvePending();
        com.michaelsebero.portium.mods116.ModernTileEntityTypes.resolvePending();
    }

    /** Forge 1.16's {@code event.getMappings(modId)}: the missing entries of that mod (Forge 1.12 picks the active mod's). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static com.google.common.collect.ImmutableList getMappings(net.minecraftforge.event.RegistryEvent.MissingMappings event, String modId) {
        com.google.common.collect.ImmutableList.Builder mappings = com.google.common.collect.ImmutableList.builder();
        for (Object mapping : event.getAllMappings()) {
            if (modId.equals(((net.minecraftforge.event.RegistryEvent.MissingMappings.Mapping) mapping).key.getNamespace())) mappings.add(mapping);
        }
        return mappings.build();
    }

    private RegistryShims116() {}
}
