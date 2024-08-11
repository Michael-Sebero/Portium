package com.michaelsebero.portium.modern.shim;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collection;

/** Forge 1.13's registry methods that Forge 1.12's registries, which 1.13 mods are given, lack. */
public final class RegistryShims {

    @Nullable
    private static final Field DEFAULT_KEY = defaultKeyField();

    @Nullable
    public static <V extends IForgeRegistryEntry<V>> ResourceLocation getRegistryName(IForgeRegistry<V> registry) {
        return RegistryManager.ACTIVE.getName(registry);
    }

    public static boolean isEmpty(IForgeRegistry<?> registry) {
        return registry.getKeys().isEmpty();
    }

    @Nullable
    public static ResourceLocation getDefaultKey(IForgeRegistry<?> registry) {
        if (DEFAULT_KEY == null || !(registry instanceof ForgeRegistry)) return null;
        try {
            return (ResourceLocation) DEFAULT_KEY.get(registry);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static <V extends IForgeRegistryEntry<V>> Collection<V> getValues(IForgeRegistry<V> registry) {
        return registry.getValuesCollection();
    }

    @Nullable
    private static Field defaultKeyField() {
        try {
            Field field = ForgeRegistry.class.getDeclaredField("defaultKey");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private RegistryShims() {}

    /**
     * A registry lookup from 1.13 code: 1.13's flattened names (minecraft:poppy, minecraft:granite) find the 1.12
     * blocks and items, or the views of their variants, that they are here.
     */
    @SuppressWarnings("unchecked")
    public static <V extends IForgeRegistryEntry<V>> V getValue(IForgeRegistry<V> registry, net.minecraft.util.ResourceLocation key) {
        if (key == null || registry.containsKey(key) || !"minecraft".equals(key.getNamespace())) return registry.getValue(key);
        if (registry == net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS) {
            net.minecraft.block.Block block = com.michaelsebero.portium.modern.flat.Flattening.block(key.getPath(), net.minecraft.block.Block.class);
            if (block != null && block != net.minecraft.init.Blocks.AIR) return (V) block;
        } else if (registry == net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS) {
            net.minecraft.item.Item item = com.michaelsebero.portium.modern.flat.Flattening.item(key.getPath(), net.minecraft.item.Item.class);
            if (item != null && item != net.minecraft.init.Items.AIR) return (V) item;
        }
        return registry.getValue(key);
    }

    public static boolean containsKey(IForgeRegistry<?> registry, net.minecraft.util.ResourceLocation key) {
        if (registry.containsKey(key)) return true;
        if (registry != net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS && registry != net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS) return false;
        Object value = getValue((IForgeRegistry) registry, key);
        return value != null && value != net.minecraft.init.Blocks.AIR && value != net.minecraft.init.Items.AIR;
    }

    /** Forge 1.13's {@code GameData.getWrapper}: a registry as 1.13's {@code IRegistry}. */
    public static <T> com.michaelsebero.portium.modern.mc.util.registry.IRegistry<T> wrapper(Class<T> type) {
        return com.michaelsebero.portium.modern.mc.util.registry.ForgeRegistryView.of(type);
    }

    public static <T> com.michaelsebero.portium.modern.mc.util.registry.IRegistry<T> wrapperDefaulted(Class<T> type) {
        return com.michaelsebero.portium.modern.mc.util.registry.ForgeRegistryView.of(type);
    }
}
