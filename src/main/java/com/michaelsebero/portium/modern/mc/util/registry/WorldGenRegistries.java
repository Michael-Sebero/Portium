package com.michaelsebero.portium.modern.mc.util.registry;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.ResourceLocation;

/**
 * 1.16's {@code WorldGenRegistries}: the built-in registries of world generation's configured objects, which 1.16
 * mods register their configured features (ores and the like) into. Portium's are plain registries; Minecraft
 * 1.16.5's own, whose class would build every vanilla 1.16 feature, structure and biome when first touched, isn't
 * used. What mods put here is generated through Forge 1.16's biome loading (see the 1.16 glue's world generation).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class WorldGenRegistries {

    @Srg("field_243650_b")
    public static final IRegistry ROOT = new SimpleRegistry(null);
    @Srg("field_243651_c")
    public static final IRegistry CONFIGURED_SURFACE_BUILDER = root("worldgen/configured_surface_builder");
    @Srg("field_243652_d")
    public static final IRegistry CONFIGURED_CARVER = root("worldgen/configured_carver");
    @Srg("field_243653_e")
    public static final IRegistry CONFIGURED_FEATURE = root("worldgen/configured_feature");
    @Srg("field_243654_f")
    public static final IRegistry CONFIGURED_STRUCTURE_FEATURE = root("worldgen/configured_structure_feature");
    @Srg("field_243655_g")
    public static final IRegistry STRUCTURE_PROCESSOR_LIST = root("worldgen/processor_list");
    @Srg("field_243656_h")
    public static final IRegistry TEMPLATE_POOL = root("worldgen/template_pool");
    /** 1.12's biomes (1.16's biome is 1.12's here). */
    @Srg("field_243657_i")
    public static final IRegistry BIOME = IRegistry.BIOME;
    @Srg("field_243658_j")
    public static final IRegistry NOISE_SETTINGS = root("worldgen/noise_settings");

    private static IRegistry root(String name) {
        IRegistry registry = new SimpleRegistry(null);
        ROOT.put(new ResourceLocation(name), registry);
        return registry;
    }

    @Srg("func_243663_a")
    public static <T> T register(IRegistry registry, String name, T value) {
        return (T) IRegistry.register(registry, new ResourceLocation(name), value);
    }

    @Srg("func_243664_a")
    public static <V, T extends V> T register(IRegistry<V> registry, ResourceLocation name, T value) {
        return IRegistry.register(registry, name, value);
    }

    /** By a 1.16 registry key (a class of Minecraft 1.16.5's own here, taken as an object), with a number. */
    @Srg("func_243662_a")
    public static <V, T extends V> T register(IRegistry<V> registry, int id, Object key, T value) {
        try {
            ResourceLocation name = (ResourceLocation) key.getClass().getMethod("func_240901_a_").invoke(key);
            registry.register(id, name, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            org.apache.logging.log4j.LogManager.getLogger("Portium").debug("Couldn't register {} now", key, e);
        }
        return value;
    }

    /** 1.16's: makes sure the registries exist; they do once this class is loaded. */
    @Srg("func_243661_a")
    public static void init() {}

    private WorldGenRegistries() {}
}
