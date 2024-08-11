package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.modern.mc.util.registry.IRegistry;
import com.michaelsebero.portium.mods116.shim.WorldShims116;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;

import java.util.Optional;

/** 1.16's dynamic registries, onto Portium's registries (see {@code WorldShims116.registries}). */
public final class WorldGlue {

    /** {@code registries.getRegistry(key)}: the dimension types (1.12's), or Portium's registry of that name. */
    public static MutableRegistry<?> registry(Object registries, RegistryKey<?> key) {
        Object registry = find(key);
        if (registry == null) throw new IllegalStateException("Missing registry: " + key);
        return (MutableRegistry<?>) registry;
    }

    /** {@code registries.func_230521_a_(key)}: the same, if there's one. */
    public static Optional<MutableRegistry<?>> optionalRegistry(Object registries, RegistryKey<?> key) {
        return Optional.ofNullable((MutableRegistry<?>) find(key));
    }

    private static Object find(RegistryKey<?> key) {
        if (key == Registry.field_239698_ad_) return WorldShims116.dimensionTypes();
        return IRegistry.REGISTRY.getNullable(key.func_240901_a_());
    }

    /** 1.16's {@code world.getDimensionKey()}: the key named as 1.12's dimension type (the same keys 1.16's own constants are). */
    public static RegistryKey<net.minecraft.world.World> dimensionKey(net.minecraft.world.World world) {
        return RegistryKey.func_240903_a_(net.minecraft.util.registry.Registry.field_239699_ae_,
                new net.minecraft.util.ResourceLocation(com.michaelsebero.portium.mods116.shim.WorldShims116.dimensionName(world)));
    }

    private WorldGlue() {}
}
