package com.michaelsebero.portium.world.gen.structure;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.12 locates every scattered feature as {@code Temple}. 1.13 names each kind, so this repeats 1.12's placement
 * (one candidate per region of {@code spacing} chunks, 8 chunks apart) and keeps candidates whose biome picks the
 * requested kind.
 */
public final class TempleLocator {

    private static final int SEPARATION = 8;
    private static final int SALT = 14357617;
    private static final int MAX_RINGS = 100;

    public static boolean isTempleName(String name) {
        return kindOf(name) != null;
    }

    @Nullable
    private static Kind kindOf(String name) {
        switch (name) {
            case "Desert_Pyramid": return Kind.DESERT_PYRAMID;
            case "Jungle_Pyramid": return Kind.JUNGLE_PYRAMID;
            case "Igloo": return Kind.IGLOO;
            case "Swamp_Hut": return Kind.SWAMP_HUT;
            default: return null;
        }
    }

    /** The kind of scattered feature 1.12 builds in a biome, as in {@code MapGenScatteredFeature.Start}. */
    @Nullable
    private static Kind kindIn(Biome biome) {
        if (biome == Biomes.JUNGLE || biome == Biomes.JUNGLE_HILLS) return Kind.JUNGLE_PYRAMID;
        if (biome == Biomes.SWAMPLAND) return Kind.SWAMP_HUT;
        if (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS) return Kind.DESERT_PYRAMID;
        if (biome == Biomes.ICE_PLAINS || biome == Biomes.COLD_TAIGA) return Kind.IGLOO;
        return null;
    }

    @Nullable
    public static BlockPos findNearest(World world, String name, BlockPos pos, int spacing) {
        Kind kind = kindOf(name);
        if (kind == null) return null;
        int originX = pos.getX() >> 4;
        int originZ = pos.getZ() >> 4;
        for (int ring = 0; ring <= MAX_RINGS; ring++) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    int regionX = Math.floorDiv(originX + dx * spacing, spacing);
                    int regionZ = Math.floorDiv(originZ + dz * spacing, spacing);
                    Random random = world.setRandomSeed(regionX, regionZ, SALT);
                    int chunkX = regionX * spacing + random.nextInt(spacing - SEPARATION);
                    int chunkZ = regionZ * spacing + random.nextInt(spacing - SEPARATION);
                    BlockPos center = new BlockPos(chunkX * 16 + 8, 64, chunkZ * 16 + 8);
                    if (kindIn(world.getBiomeProvider().getBiome(center)) != kind) continue;
                    double distance = center.distanceSq(pos.getX(), center.getY(), pos.getZ());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = center;
                    }
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    private enum Kind {
        DESERT_PYRAMID, JUNGLE_PYRAMID, IGLOO, SWAMP_HUT
    }

    private TempleLocator() {}
}
