package com.michaelsebero.portium.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.mixin.accessor.DerivedWorldInfoAccessor;
import net.minecraft.world.World;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.WorldInfo;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * A world's generation, each including the one before:
 * <ul>
 * <li>{@code 1.12.2}: 1.12's own.</li>
 * <li>{@code 1.13.2}: Portium's 1.13 terrain (ocean variants, coral, kelp, icebergs, underwater caves, shipwrecks,
 * ruins, buried treasure, uncarved pumpkins, the Buffet world type).</li>
 * <li>{@code 1.16.5}: also what 1.14 to 1.16 added, as Portium backports it. The default for new worlds.</li>
 * </ul>
 * Other mods' generation is left alone either way. Chosen when a world is created and kept in its level.dat; a world
 * from before the choice existed is 1.13.2 if Portium (or 1.13) saved it and 1.12.2 if only 1.12 did, so its new
 * chunks match the old ones.
 */
public final class WorldGeneration {

    public static final String V1_16 = "1.16.5";
    public static final String MODERN = "1.13.2";
    public static final String LEGACY = "1.12.2";
    /** Oldest first. */
    private static final List<String> VERSIONS = Arrays.asList(LEGACY, MODERN, V1_16);
    public static final String TAG = "PortiumGeneration";

    /** The world whose biome source or chunk generator is being built, for hooks that aren't told. */
    private static final ThreadLocal<WorldInfo> BUILDING = new ThreadLocal<>();

    public static boolean isModern(World world) {
        return isModern(world.getWorldInfo());
    }

    /** At least 1.13.2's generation. */
    public static boolean isModern(WorldInfo info) {
        return isAtLeast(info, MODERN);
    }

    /** 1.16.5's generation. */
    public static boolean is116(World world) {
        return isAtLeast(world.getWorldInfo(), V1_16);
    }

    public static boolean isAtLeast(WorldInfo info, String version) {
        return isAtLeast(of(info), version);
    }

    /** The world's generation, one of the versions. */
    public static String of(WorldInfo info) {
        if (info instanceof DerivedWorldInfo) info = ((DerivedWorldInfoAccessor) info).portium$getDelegate();
        return resolve(((IWorldGeneration) info).portium$getGeneration());
    }

    private static boolean isAtLeast(String generation, String version) {
        return VERSIONS.indexOf(generation) >= VERSIONS.indexOf(version);
    }

    /** The generation of the world being set up, for biome layers and map generators built without a world at hand. */
    public static boolean isBuildingModern() {
        return isBuildingAtLeast(MODERN);
    }

    public static boolean isBuildingAtLeast(String version) {
        WorldInfo info = BUILDING.get();
        return isAtLeast(info == null ? resolve(null) : of(info), version);
    }

    /** The Create World screen's next choice: newest first, round again after the oldest. */
    public static String next(String generation) {
        int index = VERSIONS.indexOf(resolve(generation));
        return VERSIONS.get(index == 0 ? VERSIONS.size() - 1 : index - 1);
    }

    public static void startBuilding(WorldInfo info) {
        BUILDING.set(info);
    }

    public static void stopBuilding() {
        BUILDING.remove();
    }

    /** A stored value, or the configured default for new worlds (1.16.5 unless the config names another version). */
    public static String resolve(@Nullable String generation) {
        if (VERSIONS.contains(generation)) return generation;
        String configured = PortiumConfig.worldGen.defaultGeneration;
        return VERSIONS.contains(configured) ? configured : V1_16;
    }

    private WorldGeneration() {}
}
