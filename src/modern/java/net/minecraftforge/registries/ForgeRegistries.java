package net.minecraftforge.registries;

import com.michaelsebero.portium.modern.ModernRegistries;

/**
 * Portium's replacement for Forge 1.13's: 1.12's registries, which have the same names. 1.12 has no registry of 1.13's
 * entity types, tile entity types or mod dimensions; those are null. Fields are raw: the Minecraft 1.13 this compiles
 * against is unpatched, so its blocks and items aren't Forge registry entries; the descriptors are what count.
 */
@SuppressWarnings("rawtypes")
public class ForgeRegistries {

    public static final IForgeRegistry BLOCKS = get("minecraft:blocks");
    public static final IForgeRegistry ITEMS = get("minecraft:items");
    public static final IForgeRegistry POTIONS = get("minecraft:potions");
    public static final IForgeRegistry BIOMES = get("minecraft:biomes");
    public static final IForgeRegistry SOUND_EVENTS = get("minecraft:soundevents");
    public static final IForgeRegistry POTION_TYPES = get("minecraft:potiontypes");
    public static final IForgeRegistry ENCHANTMENTS = get("minecraft:enchantments");
    public static final IForgeRegistry VILLAGER_PROFESSIONS = get("minecraft:villagerprofessions");
    public static final IForgeRegistry ENTITIES = null;
    public static final IForgeRegistry TILE_ENTITIES = null;
    public static final IForgeRegistry MOD_DIMENSIONS = null;
    public static final IForgeRegistry DATA_SERIALIZERS = get("minecraft:dataserializers");

    private static IForgeRegistry get(String name) {
        return ModernRegistries.get(name);
    }
}
