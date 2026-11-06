package com.michaelsebero.portium.init;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.lang.reflect.Method;

/**
 * Portium's block and item ids look native: {@code minecraft:heart_of_the_sea}, as 1.13 to 1.16 name them, unless
 * 1.12 already has something by that name (then {@code portium:}). The old {@code portium:} names stay as aliases, so
 * recipes, loot tables, commands and items saved under them still find the thing, and worlds saved with them are
 * remapped as they load. Assets stay in Portium's own folder (ClientRegistration maps models there).
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PortiumNames {

    private static Method addAlias;

    /** Names Portium registered under minecraft: rather than portium:, by the registry's type (blocks, items...). */
    private static final java.util.Map<Class<?>, java.util.Set<String>> MOVED = new java.util.concurrent.ConcurrentHashMap<>();

    /** The id a Portium block or item registers under. */
    public static ResourceLocation of(IForgeRegistry<?> registry, String name) {
        ResourceLocation vanilla = new ResourceLocation("minecraft", name);
        if (!PortiumConfig.mechanics.minecraftIds) return new ResourceLocation(Tags.MOD_ID, name);
        if (registry.containsKey(vanilla)) {
            Portium.LOGGER.info("Keeping portium:{} ({} already has minecraft:{})", name, registry.getRegistrySuperType().getSimpleName(), name);
            return new ResourceLocation(Tags.MOD_ID, name);
        }
        moved(registry.getRegistrySuperType(), name);
        return vanilla;
    }

    /** Records that Portium's {@code name} in a registry of {@code type} is {@code minecraft:name}. */
    public static void moved(Class<?> type, String name) {
        MOVED.computeIfAbsent(type, key -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(name);
    }

    /**
     * The minecraft: id an old portium: name stands for, or null: what Forge's aliases were meant to answer (they don't
     * outlast startup), asked by Portium's registry mixin for every portium: lookup that finds nothing.
     */
    @javax.annotation.Nullable
    public static ResourceLocation movedFrom(Class<?> type, ResourceLocation id) {
        if (!Tags.MOD_ID.equals(id.getNamespace())) return null;
        java.util.Set<String> names = MOVED.get(type);
        return names != null && names.contains(id.getPath()) ? new ResourceLocation("minecraft", id.getPath()) : null;
    }

    /** After registering under a minecraft: id, its portium: name becomes an alias for it. */
    public static <T extends IForgeRegistryEntry<T>> void alias(IForgeRegistry<T> registry, ResourceLocation id) {
        if (!"minecraft".equals(id.getNamespace())) return;
        moved(registry.getRegistrySuperType(), id.getPath());
        // Forge's own alias isn't added: it doesn't outlast startup, and with portium: names answered by
        // MixinForgeRegistryPortiumNames, Forge takes each alias for a bugged one and warns at every world load.
        if (Boolean.TRUE) return;
        if (!(registry instanceof ForgeRegistry)) return;
        try {
            if (addAlias == null) {
                addAlias = ForgeRegistry.class.getDeclaredMethod("addAlias", ResourceLocation.class, ResourceLocation.class);
                addAlias.setAccessible(true);
            }
            addAlias.invoke(registry, new ResourceLocation(Tags.MOD_ID, id.getPath()), id);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Portium.LOGGER.warn("Couldn't alias {} to {}", new ResourceLocation(Tags.MOD_ID, id.getPath()), id, e);
        }
    }

    /** Worlds saved with portium: ids get the same blocks and items under their minecraft: ids. */
    @SubscribeEvent
    public static void onMissingBlocks(RegistryEvent.MissingMappings<Block> event) {
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            if (!Tags.MOD_ID.equals(mapping.key.getNamespace())) continue;
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft", mapping.key.getPath()));
            if (block != null && block.getRegistryName() != null && "minecraft".equals(block.getRegistryName().getNamespace())
                    && block.getRegistryName().getPath().equals(mapping.key.getPath())) {
                mapping.remap(block);
            }
        }
    }

    @SubscribeEvent
    public static void onMissingItems(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (!Tags.MOD_ID.equals(mapping.key.getNamespace())) continue;
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", mapping.key.getPath()));
            if (item != null && item.getRegistryName() != null && "minecraft".equals(item.getRegistryName().getNamespace())
                    && item.getRegistryName().getPath().equals(mapping.key.getPath())) {
                mapping.remap(item);
            }
        }
    }

    /** Whether a registered id is one of Portium's (its own namespace, or a minecraft: id it took). */
    public static boolean isPortium(IForgeRegistryEntry<?> entry) {
        return entry instanceof Block ? PortiumBlocks.all().contains(entry) : entry instanceof Item && PortiumItems.all().contains(entry);
    }

    private PortiumNames() {}
}
