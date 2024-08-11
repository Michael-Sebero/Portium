package com.michaelsebero.portium.world.v116.nether;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.biome.BiomeNether116;
import com.michaelsebero.portium.world.v116.GenFlags;
import com.michaelsebero.portium.world.v116.feature.HugeFungus;
import com.michaelsebero.portium.world.v116.feature.NetherVegetation;
import com.michaelsebero.portium.world.v116.feature.NetherVines;
import com.michaelsebero.portium.world.v116.noise.SeedRandom;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * The features each 1.16 Nether biome decorates a chunk with, in 1.16's order. 1.16 seeds every feature from the
 * chunk's decoration seed, its decoration stage and its index in that stage, where a stage's structures (every
 * structure in the game, whether or not it can be in that biome) count first, so each feature here carries the
 * index 1.16 gives it.
 * <p>
 * 1.12 populates the 16x16 area offset 8 blocks from a chunk's corner, so the features land 8 blocks along from where
 * 1.16 puts them, over the same kind of ground.
 */
final class NetherDecoration {

    // 1.16's decoration stages.
    private static final int LOCAL_MODIFICATIONS = 2;
    private static final int SURFACE_STRUCTURES = 4;
    private static final int UNDERGROUND_DECORATION = 7;
    private static final int VEGETAL_DECORATION = 8;

    /** How many of 1.16's structures place in each stage, coming before its features when structures generate. */
    private static int structures(int stage) {
        switch (stage) {
            case 3: return 2; // mineshaft, buried treasure
            case SURFACE_STRUCTURES: return 13; // outpost, mansion, pyramids, igloo, ruined portal, shipwreck, hut, monument, ruin, end city, village, bastion
            case 5: return 1; // stronghold
            case UNDERGROUND_DECORATION: return 2; // fortress, nether fossil
            default: return 0;
        }
    }

    /** A feature placed from a chunk's corner. */
    interface Feature {
        void place(World world, Random random, BlockPos origin);
    }

    private static final class Entry {
        final int stage;
        final int index;
        final Feature feature;

        Entry(int stage, int index, Feature feature) {
            this.stage = stage;
            this.index = index;
            this.feature = feature;
        }
    }

    /** One biome's list, built stage by stage in the order 1.16 lists them. */
    private static final class Builder {
        final List<Entry> entries = new ArrayList<>();
        final int[] counts = new int[10];
        final boolean structures;

        Builder(boolean structures) {
            this.structures = structures;
        }

        Builder add(int stage, Feature feature) {
            int index = (this.structures ? structures(stage) : 0) + this.counts[stage]++;
            if (feature != null) this.entries.add(new Entry(stage, index, feature));
            return this;
        }

        List<Entry> build() {
            this.entries.sort((a, b) -> a.stage != b.stage ? Integer.compare(a.stage, b.stage) : Integer.compare(a.index, b.index));
            return this.entries;
        }
    }

    private final List<Entry> wastes;
    private final List<Entry> soulSandValley;
    private final List<Entry> crimsonForest;
    private final List<Entry> warpedForest;
    private final List<Entry> basaltDeltas;

    NetherDecoration(boolean structures) {
        this.wastes = new Builder(structures)
                .add(VEGETAL_DECORATION, null) // spring_lava: needs stone, none in the Nether
                .add(VEGETAL_DECORATION, BROWN_MUSHROOM_NORMAL)
                .add(VEGETAL_DECORATION, RED_MUSHROOM_NORMAL)
                .add(UNDERGROUND_DECORATION, SPRING_OPEN)
                .add(UNDERGROUND_DECORATION, PATCH_FIRE)
                .add(UNDERGROUND_DECORATION, PATCH_SOUL_FIRE)
                .add(UNDERGROUND_DECORATION, GLOWSTONE_EXTRA)
                .add(UNDERGROUND_DECORATION, GLOWSTONE)
                .add(UNDERGROUND_DECORATION, BROWN_MUSHROOM_NETHER)
                .add(UNDERGROUND_DECORATION, RED_MUSHROOM_NETHER)
                .add(UNDERGROUND_DECORATION, ORE_MAGMA)
                .add(UNDERGROUND_DECORATION, SPRING_CLOSED)
                .add(UNDERGROUND_DECORATION, ORE_GRAVEL)
                .add(UNDERGROUND_DECORATION, ORE_BLACKSTONE)
                .add(UNDERGROUND_DECORATION, ORE_GOLD)
                .add(UNDERGROUND_DECORATION, ORE_QUARTZ)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_LARGE)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_SMALL)
                .build();
        this.soulSandValley = new Builder(structures)
                .add(VEGETAL_DECORATION, null) // spring_lava
                .add(LOCAL_MODIFICATIONS, BASALT_PILLAR)
                .add(UNDERGROUND_DECORATION, SPRING_OPEN)
                .add(UNDERGROUND_DECORATION, GLOWSTONE_EXTRA)
                .add(UNDERGROUND_DECORATION, GLOWSTONE)
                .add(UNDERGROUND_DECORATION, PATCH_CRIMSON_ROOTS)
                .add(UNDERGROUND_DECORATION, PATCH_FIRE)
                .add(UNDERGROUND_DECORATION, PATCH_SOUL_FIRE)
                .add(UNDERGROUND_DECORATION, ORE_MAGMA)
                .add(UNDERGROUND_DECORATION, SPRING_CLOSED)
                .add(UNDERGROUND_DECORATION, ORE_SOUL_SAND)
                .add(UNDERGROUND_DECORATION, ORE_GRAVEL)
                .add(UNDERGROUND_DECORATION, ORE_BLACKSTONE)
                .add(UNDERGROUND_DECORATION, ORE_GOLD)
                .add(UNDERGROUND_DECORATION, ORE_QUARTZ)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_LARGE)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_SMALL)
                .build();
        this.basaltDeltas = new Builder(structures)
                .add(SURFACE_STRUCTURES, DELTA)
                .add(VEGETAL_DECORATION, null) // spring_lava_double
                .add(SURFACE_STRUCTURES, SMALL_BASALT_COLUMNS)
                .add(SURFACE_STRUCTURES, LARGE_BASALT_COLUMNS)
                .add(UNDERGROUND_DECORATION, BASALT_BLOBS)
                .add(UNDERGROUND_DECORATION, BLACKSTONE_BLOBS)
                .add(UNDERGROUND_DECORATION, SPRING_DELTA)
                .add(UNDERGROUND_DECORATION, PATCH_FIRE)
                .add(UNDERGROUND_DECORATION, PATCH_SOUL_FIRE)
                .add(UNDERGROUND_DECORATION, GLOWSTONE_EXTRA)
                .add(UNDERGROUND_DECORATION, GLOWSTONE)
                .add(UNDERGROUND_DECORATION, BROWN_MUSHROOM_NETHER)
                .add(UNDERGROUND_DECORATION, RED_MUSHROOM_NETHER)
                .add(UNDERGROUND_DECORATION, ORE_MAGMA)
                .add(UNDERGROUND_DECORATION, SPRING_CLOSED_DOUBLE)
                .add(UNDERGROUND_DECORATION, ORE_GOLD_DELTAS)
                .add(UNDERGROUND_DECORATION, ORE_QUARTZ_DELTAS)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_LARGE)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_SMALL)
                .build();
        this.crimsonForest = new Builder(structures)
                .add(VEGETAL_DECORATION, null) // spring_lava
                .add(VEGETAL_DECORATION, BROWN_MUSHROOM_NORMAL)
                .add(VEGETAL_DECORATION, RED_MUSHROOM_NORMAL)
                .add(UNDERGROUND_DECORATION, SPRING_OPEN)
                .add(UNDERGROUND_DECORATION, PATCH_FIRE)
                .add(UNDERGROUND_DECORATION, GLOWSTONE_EXTRA)
                .add(UNDERGROUND_DECORATION, GLOWSTONE)
                .add(UNDERGROUND_DECORATION, ORE_MAGMA)
                .add(UNDERGROUND_DECORATION, SPRING_CLOSED)
                .add(VEGETAL_DECORATION, WEEPING_VINES)
                .add(VEGETAL_DECORATION, CRIMSON_FUNGI)
                .add(VEGETAL_DECORATION, CRIMSON_FOREST_VEGETATION)
                .add(UNDERGROUND_DECORATION, ORE_GRAVEL)
                .add(UNDERGROUND_DECORATION, ORE_BLACKSTONE)
                .add(UNDERGROUND_DECORATION, ORE_GOLD)
                .add(UNDERGROUND_DECORATION, ORE_QUARTZ)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_LARGE)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_SMALL)
                .build();
        this.warpedForest = new Builder(structures)
                .add(VEGETAL_DECORATION, null) // spring_lava
                .add(VEGETAL_DECORATION, BROWN_MUSHROOM_NORMAL)
                .add(VEGETAL_DECORATION, RED_MUSHROOM_NORMAL)
                .add(UNDERGROUND_DECORATION, SPRING_OPEN)
                .add(UNDERGROUND_DECORATION, PATCH_FIRE)
                .add(UNDERGROUND_DECORATION, PATCH_SOUL_FIRE)
                .add(UNDERGROUND_DECORATION, GLOWSTONE_EXTRA)
                .add(UNDERGROUND_DECORATION, GLOWSTONE)
                .add(UNDERGROUND_DECORATION, ORE_MAGMA)
                .add(UNDERGROUND_DECORATION, SPRING_CLOSED)
                .add(VEGETAL_DECORATION, WARPED_FUNGI)
                .add(VEGETAL_DECORATION, WARPED_FOREST_VEGETATION)
                .add(VEGETAL_DECORATION, NETHER_SPROUTS)
                .add(VEGETAL_DECORATION, TWISTING_VINES)
                .add(UNDERGROUND_DECORATION, ORE_GRAVEL)
                .add(UNDERGROUND_DECORATION, ORE_BLACKSTONE)
                .add(UNDERGROUND_DECORATION, ORE_GOLD)
                .add(UNDERGROUND_DECORATION, ORE_QUARTZ)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_LARGE)
                .add(UNDERGROUND_DECORATION, ORE_DEBRIS_SMALL)
                .build();
    }

    /**
     * Decorates the area from {@code origin} (8 blocks along from a chunk's corner) with {@code biome}'s features,
     * seeded as 1.16 seeds the chunk at {@code (chunkX, chunkZ)}. {@code structures} places the chunk's structures,
     * which 1.16 does at the start of the underground decoration stage, after the basalt deltas' surface features.
     */
    void decorate(World world, long worldSeed, int chunkX, int chunkZ, BlockPos origin, Biome biome, Runnable structures) {
        SeedRandom random = new SeedRandom();
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        boolean structuresPlaced = false;
        for (Entry entry : this.listFor(biome)) {
            if (!structuresPlaced && entry.stage >= UNDERGROUND_DECORATION) {
                structures.run();
                structuresPlaced = true;
            }
            random.setFeatureSeed(decorationSeed, entry.index, entry.stage);
            entry.feature.place(world, random, origin);
        }
        if (!structuresPlaced) structures.run();
    }

    private List<Entry> listFor(Biome biome) {
        if (!(biome instanceof BiomeNether116)) return this.wastes;
        switch (((BiomeNether116) biome).getKind()) {
            case SOUL_SAND_VALLEY: return this.soulSandValley;
            case CRIMSON_FOREST: return this.crimsonForest;
            case WARPED_FOREST: return this.warpedForest;
            case BASALT_DELTAS: return this.basaltDeltas;
            default: return this.wastes;
        }
    }

    // ---- Placements ----

    /** 1.16's square placement: somewhere in the 16x16 area. */
    private static BlockPos square(Random random, BlockPos pos) {
        int x = random.nextInt(16) + pos.getX();
        int z = random.nextInt(16) + pos.getZ();
        return new BlockPos(x, pos.getY(), z);
    }

    /** 1.16's range placement: y from {@code bottom} to below {@code top}. */
    private static BlockPos range(Random random, BlockPos pos, int bottom, int topOffset, int maximum) {
        return new BlockPos(pos.getX(), random.nextInt(maximum - topOffset) + bottom, pos.getZ());
    }

    /** The feature {@code count} times, each somewhere in the area at a height from 0 to {@code height}. */
    private static Feature countSquareRange(int count, int height, Feature feature) {
        return (world, random, origin) -> {
            for (int i = 0; i < count; i++) feature.place(world, random, range(random, square(random, origin), 0, 0, height));
        };
    }

    /** The feature {@code count} times, each somewhere in the area at y {@code bottom} to {@code maximum - topOffset}. */
    private static Feature countSquareRange(int count, int bottom, int topOffset, int maximum, Feature feature) {
        return (world, random, origin) -> {
            for (int i = 0; i < count; i++) feature.place(world, random, range(random, square(random, origin), bottom, topOffset, maximum));
        };
    }

    /**
     * 1.16's count_multilayer: {@code count} spots per layer, each on the {@code layer}th floor down from the top of
     * its column, layer after layer until a layer finds nowhere.
     */
    private static Feature multilayer(int count, Feature feature) {
        return (world, random, origin) -> {
            List<BlockPos> spots = new ArrayList<>();
            boolean found;
            int layer = 0;
            do {
                found = false;
                for (int i = 0; i < count; i++) {
                    int x = random.nextInt(16) + origin.getX();
                    int z = random.nextInt(16) + origin.getZ();
                    int y = floorOf(world, x, world.getHeight(x, z), z, layer);
                    if (y != Integer.MAX_VALUE) {
                        spots.add(new BlockPos(x, y, z));
                        found = true;
                    }
                }
                layer++;
            } while (found);
            for (BlockPos spot : spots) feature.place(world, random, spot);
        };
    }

    private static int floorOf(World world, int x, int top, int z, int layer) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, top, z);
        IBlockState above = world.getBlockState(at);
        int floors = 0;
        for (int y = top; y >= 1; y--) {
            at.setY(y - 1);
            IBlockState here = world.getBlockState(at);
            if (!isEmpty(here) && isEmpty(above) && here.getBlock() != Blocks.BEDROCK) {
                if (floors == layer) return at.getY() + 1;
                floors++;
            }
            above = here;
        }
        return Integer.MAX_VALUE;
    }

    private static boolean isEmpty(IBlockState state) {
        Material material = state.getMaterial();
        return material == Material.AIR || material == Material.WATER || material == Material.LAVA;
    }

    /** 1.16's heightmap_spread_double: from the ground to twice its height (above the Nether's roof, too). */
    private static BlockPos spreadDouble(World world, Random random, BlockPos pos) {
        int height = world.getHeight(pos.getX(), pos.getZ());
        return height == 0 ? null : new BlockPos(pos.getX(), random.nextInt(height * 2), pos.getZ());
    }

    /** 1.16's fire placement: positions drawn up front, the count re-rolled each time round as 1.16's loop does. */
    private static Feature fire(Feature feature) {
        return (world, random, origin) -> {
            List<BlockPos> spots = new ArrayList<>();
            for (int i = 0; i < random.nextInt(random.nextInt(10) + 1) + 1; i++) {
                int x = random.nextInt(16) + origin.getX();
                int z = random.nextInt(16) + origin.getZ();
                int y = random.nextInt(120) + 4;
                spots.add(new BlockPos(x, y, z));
            }
            for (BlockPos spot : spots) feature.place(world, random, spot);
        };
    }

    // ---- Features ----

    private static final Feature BROWN_MUSHROOM_NORMAL = mushroomsNormal(Blocks.BROWN_MUSHROOM.getDefaultState(), 4);
    private static final Feature RED_MUSHROOM_NORMAL = mushroomsNormal(Blocks.RED_MUSHROOM.getDefaultState(), 8);

    private static Feature mushroomsNormal(IBlockState mushroom, int chance) {
        return (world, random, origin) -> {
            if (random.nextFloat() >= 1.0F / (float) chance) return;
            BlockPos at = spreadDouble(world, random, square(random, origin));
            if (at != null) NetherFeatures.patch(world, random, at, mushroom, 64, null);
        };
    }

    private static final Feature BROWN_MUSHROOM_NETHER = mushroomsNether(Blocks.BROWN_MUSHROOM.getDefaultState());
    private static final Feature RED_MUSHROOM_NETHER = mushroomsNether(Blocks.RED_MUSHROOM.getDefaultState());

    /** 1.16 places these from the area's corner: it has no square placement. */
    private static Feature mushroomsNether(IBlockState mushroom) {
        return (world, random, origin) -> {
            if (random.nextFloat() >= 0.5F) return;
            NetherFeatures.patch(world, random, range(random, origin, 0, 0, 128), mushroom, 64, null);
        };
    }

    private static final Feature SPRING_OPEN = countSquareRange(8, 4, 8, 128,
            (world, random, pos) -> NetherFeatures.spring(world, pos, false, 4, 1, NetherFeatures::netherrack));
    private static final Feature SPRING_CLOSED = closedSprings(16);
    private static final Feature SPRING_CLOSED_DOUBLE = closedSprings(32);

    private static Feature closedSprings(int count) {
        return countSquareRange(count, 10, 20, 128, (world, random, pos) -> NetherFeatures.spring(world, pos, false, 5, 0, NetherFeatures::netherrack));
    }

    private static final Feature SPRING_DELTA = countSquareRange(16, 4, 8, 128,
            (world, random, pos) -> NetherFeatures.spring(world, pos, true, 4, 1, NetherFeatures::deltaSpringRock));

    private static final Feature PATCH_FIRE = fire((world, random, pos) -> NetherFeatures.patch(world, random, pos, Blocks.FIRE.getDefaultState(), 64, Blocks.NETHERRACK));
    private static final Feature PATCH_SOUL_FIRE = PortiumBlocks.SOUL_FIRE == null || PortiumBlocks.SOUL_SOIL == null ? null
            : fire((world, random, pos) -> NetherFeatures.patch(world, random, pos, PortiumBlocks.SOUL_FIRE.getDefaultState(), 64, PortiumBlocks.SOUL_SOIL));

    /** 1.16's glowstone placement: up to 10 blobs, their spots drawn as each is placed. */
    private static final Feature GLOWSTONE_EXTRA = (world, random, origin) -> {
        int count = random.nextInt(random.nextInt(10) + 1);
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(16) + origin.getX();
            int z = random.nextInt(16) + origin.getZ();
            int y = random.nextInt(120) + 4;
            NetherFeatures.glowstone(world, random, new BlockPos(x, y, z));
        }
    };
    private static final Feature GLOWSTONE = countSquareRange(10, 128, NetherFeatures::glowstone);

    private static final Feature ORE_MAGMA = (world, random, origin) -> {
        for (int i = 0; i < 4; i++) {
            BlockPos at = square(random, origin);
            at = new BlockPos(at.getX(), NetherFeatures.SEA_LEVEL - 5 + random.nextInt(10), at.getZ());
            NetherFeatures.ore(world, random, at, NetherFeatures::isNetherrack, Blocks.MAGMA.getDefaultState(), 33);
        }
    };

    private static Feature ore(int count, int bottom, int topOffset, int maximum, Block block, int size) {
        if (block == null) return null;
        IBlockState state = block.getDefaultState();
        return countSquareRange(count, bottom, topOffset, maximum, (world, random, pos) -> NetherFeatures.ore(world, random, pos, NetherFeatures::isNetherrack, state, size));
    }

    private static final Feature ORE_GRAVEL = ore(2, 5, 0, 37, Blocks.GRAVEL, 33);
    private static final Feature ORE_BLACKSTONE = ore(2, 5, 10, 37, PortiumBlocks.BLACKSTONE, 33);
    private static final Feature ORE_GOLD = ore(10, 10, 20, 128, PortiumBlocks.NETHER_GOLD_ORE, 10);
    private static final Feature ORE_QUARTZ = ore(16, 10, 20, 128, Blocks.QUARTZ_ORE, 14);
    private static final Feature ORE_GOLD_DELTAS = ore(20, 10, 20, 128, PortiumBlocks.NETHER_GOLD_ORE, 10);
    private static final Feature ORE_QUARTZ_DELTAS = ore(32, 10, 20, 128, Blocks.QUARTZ_ORE, 14);
    private static final Feature ORE_SOUL_SAND = countSquareRange(12, 32,
            (world, random, pos) -> NetherFeatures.ore(world, random, pos, NetherFeatures::isNetherrack, Blocks.SOUL_SAND.getDefaultState(), 12));

    private static final Feature ORE_DEBRIS_LARGE = PortiumBlocks.ANCIENT_DEBRIS == null ? null : (world, random, origin) -> {
        BlockPos at = square(random, origin);
        at = new BlockPos(at.getX(), random.nextInt(8) + random.nextInt(8) - 8 + 16, at.getZ());
        NetherFeatures.hiddenOre(world, random, at, NetherFeatures::isNetherStone, PortiumBlocks.ANCIENT_DEBRIS.getDefaultState(), 3);
    };
    private static final Feature ORE_DEBRIS_SMALL = PortiumBlocks.ANCIENT_DEBRIS == null ? null : (world, random, origin) -> {
        BlockPos at = range(random, square(random, origin), 8, 16, 128);
        NetherFeatures.hiddenOre(world, random, at, NetherFeatures::isNetherStone, PortiumBlocks.ANCIENT_DEBRIS.getDefaultState(), 2);
    };

    private static final Feature BASALT_PILLAR = PortiumBlocks.BASALT == null ? null : countSquareRange(10, 128, NetherFeatures::basaltPillar);

    private static final Feature PATCH_CRIMSON_ROOTS = PortiumBlocks.CRIMSON_ROOTS == null ? null
            : (world, random, origin) -> NetherFeatures.patch(world, random, range(random, origin, 0, 0, 128), PortiumBlocks.CRIMSON_ROOTS.getDefaultState(), 64, null);

    private static final Feature DELTA = multilayer(40, NetherFeatures::delta);
    private static final Feature SMALL_BASALT_COLUMNS = multilayer(4, (world, random, pos) -> NetherFeatures.basaltColumns(world, random, pos, 1, 0, 1, 3));
    private static final Feature LARGE_BASALT_COLUMNS = multilayer(2, (world, random, pos) -> NetherFeatures.basaltColumns(world, random, pos, 2, 1, 5, 5));
    private static final Feature BASALT_BLOBS = PortiumBlocks.BASALT == null ? null : countSquareRange(75, 128,
            (world, random, pos) -> NetherFeatures.replaceBlob(world, random, pos, Blocks.NETHERRACK, PortiumBlocks.BASALT.getDefaultState(), 3, 4));
    private static final Feature BLACKSTONE_BLOBS = PortiumBlocks.BLACKSTONE == null ? null : countSquareRange(25, 128,
            (world, random, pos) -> NetherFeatures.replaceBlob(world, random, pos, Blocks.NETHERRACK, PortiumBlocks.BLACKSTONE.getDefaultState(), 3, 4));

    private static final Feature WEEPING_VINES = countSquareRange(10, 128, NetherVines::placeWeeping);
    private static final Feature TWISTING_VINES = countSquareRange(10, 128, (world, random, pos) -> NetherVines.spreadTwisting(world, random, pos, 8, 4, 8, GenFlags.GENERATION));
    private static final Feature CRIMSON_FUNGI = multilayer(8, (world, random, pos) -> HugeFungus.place(world, random, pos, false, false));
    private static final Feature WARPED_FUNGI = multilayer(8, (world, random, pos) -> HugeFungus.place(world, random, pos, true, false));
    private static final Feature CRIMSON_FOREST_VEGETATION = vegetation(6, NetherVegetation.CRIMSON_FOREST);
    private static final Feature WARPED_FOREST_VEGETATION = vegetation(5, NetherVegetation.WARPED_FOREST);
    private static final Feature NETHER_SPROUTS = vegetation(4, NetherVegetation.SPROUTS);

    private static Feature vegetation(int count, Function<Random, IBlockState> plants) {
        return multilayer(count, (world, random, pos) -> NetherVegetation.place(world, random, pos, plants, 8, 4, GenFlags.GENERATION));
    }
}
