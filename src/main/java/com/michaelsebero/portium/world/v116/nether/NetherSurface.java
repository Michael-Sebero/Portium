package com.michaelsebero.portium.world.v116.nether;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.biome.BiomeNether116;
import com.michaelsebero.portium.world.v116.noise.OctaveNoise;
import com.michaelsebero.portium.world.v116.noise.SeedRandom;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

/**
 * 1.16's Nether surfaces, laid over the netherrack: soul sand and gravel near the lava sea in the wastes, soul sand
 * and soul soil in valleys, basalt and blackstone in the deltas, nylium and wart blocks in the forests. Then the
 * bedrock floor and roof. Each surface builder seeds its noise from the world seed, as 1.16's do.
 */
final class NetherSurface {

    private static final IBlockState NETHERRACK = Blocks.NETHERRACK.getDefaultState();
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();
    private static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    private static final IBlockState SOUL_SAND = Blocks.SOUL_SAND.getDefaultState();
    private static final IBlockState BEDROCK = Blocks.BEDROCK.getDefaultState();

    private final OctaveNoise wastesNoise;
    private final OctaveNoise forestNoise;
    private final Valley soulSandValley;
    private final Valley basaltDeltas;

    NetherSurface(long seed) {
        this.wastesNoise = OctaveNoise.ofRange(new SeedRandom(seed), -3, 0);
        this.forestNoise = OctaveNoise.ofOctaves(new SeedRandom(seed), 0);
        IBlockState soulSoil = state(PortiumBlocks.SOUL_SOIL);
        IBlockState basalt = state(PortiumBlocks.BASALT);
        IBlockState blackstone = state(PortiumBlocks.BLACKSTONE);
        this.soulSandValley = soulSoil == null ? null : new Valley(seed, new IBlockState[]{SOUL_SAND, soulSoil}, new IBlockState[]{SOUL_SAND, soulSoil});
        this.basaltDeltas = basalt == null || blackstone == null ? null : new Valley(seed, new IBlockState[]{basalt, blackstone}, new IBlockState[]{basalt});
    }

    private static IBlockState state(Block block) {
        return block == null ? null : block.getDefaultState();
    }

    /** Builds one column's surface, {@code depth} being the surface noise there. */
    void build(Biome biome, Random random, ChunkPrimer primer, int x, int z, double depth) {
        BiomeNether116.Kind kind = biome instanceof BiomeNether116 ? ((BiomeNether116) biome).getKind() : null;
        if (kind == BiomeNether116.Kind.SOUL_SAND_VALLEY && this.soulSandValley != null) {
            this.soulSandValley.build(random, primer, x, z, depth);
        } else if (kind == BiomeNether116.Kind.BASALT_DELTAS && this.basaltDeltas != null) {
            this.basaltDeltas.build(random, primer, x, z, depth);
        } else if (kind == BiomeNether116.Kind.CRIMSON_FOREST && PortiumBlocks.CRIMSON_NYLIUM != null) {
            this.forest(random, primer, x, z, depth, PortiumBlocks.CRIMSON_NYLIUM.getDefaultState(), Blocks.NETHER_WART_BLOCK.getDefaultState());
        } else if (kind == BiomeNether116.Kind.WARPED_FOREST && PortiumBlocks.WARPED_NYLIUM != null && PortiumBlocks.WARPED_WART_BLOCK != null) {
            this.forest(random, primer, x, z, depth, PortiumBlocks.WARPED_NYLIUM.getDefaultState(), PortiumBlocks.WARPED_WART_BLOCK.getDefaultState());
        } else {
            this.wastes(random, primer, x, z, depth);
        }
    }

    /** 1.16's NetherSurfaceBuilder: netherrack, with soul sand and gravel patches just above the lava sea. */
    private void wastes(Random random, ChunkPrimer primer, int x, int z, double depth) {
        int seaLevel = NetherFeatures.SEA_LEVEL;
        int localX = x & 15;
        int localZ = z & 15;
        boolean soulSand = this.wastesNoise.noise((double) x * 0.03125D, (double) z * 0.03125D, 0.0D) * 75.0D + random.nextDouble() > 0.0D;
        boolean gravel = this.wastesNoise.noise((double) x * 0.03125D, 109.0D, (double) z * 0.03125D) * 75.0D + random.nextDouble() > 0.0D;
        int layers = (int) (depth / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        int left = -1;
        IBlockState top = NETHERRACK;
        IBlockState under = NETHERRACK;
        for (int y = 127; y >= 0; y--) {
            IBlockState here = primer.getBlockState(localX, y, localZ);
            if (here.getMaterial() == Material.AIR) {
                left = -1;
            } else if (here.getBlock() == Blocks.NETHERRACK) {
                if (left == -1) {
                    boolean bare = false;
                    if (layers <= 0) {
                        bare = true;
                        under = NETHERRACK;
                    } else if (y >= seaLevel - 4 && y <= seaLevel + 1) {
                        top = NETHERRACK;
                        under = NETHERRACK;
                        if (gravel) {
                            top = GRAVEL;
                            under = NETHERRACK;
                        }
                        if (soulSand) {
                            top = SOUL_SAND;
                            under = SOUL_SAND;
                        }
                    }
                    if (y < seaLevel && bare) top = LAVA;
                    left = layers;
                    primer.setBlockState(localX, y, localZ, y >= seaLevel - 1 ? top : under);
                } else if (left > 0) {
                    left--;
                    primer.setBlockState(localX, y, localZ, under);
                }
            }
        }
    }

    /** 1.16's NetherForestsSurfaceBuilder: nylium over netherrack, with wart block patches. */
    private void forest(Random random, ChunkPrimer primer, int x, int z, double depth, IBlockState nylium, IBlockState wart) {
        int seaLevel = NetherFeatures.SEA_LEVEL;
        int localX = x & 15;
        int localZ = z & 15;
        double bareNoise = this.forestNoise.noise((double) x * 0.1D, (double) seaLevel, (double) z * 0.1D);
        boolean bare = bareNoise > 0.15D + random.nextDouble() * 0.35D;
        double wartNoise = this.forestNoise.noise((double) x * 0.1D, 109.0D, (double) z * 0.1D);
        boolean wartTop = wartNoise > 0.25D + random.nextDouble() * 0.9D;
        int layers = (int) (depth / 3.0D + 3.0D + random.nextDouble() * 0.25D);
        int left = -1;
        IBlockState under = NETHERRACK;
        for (int y = 127; y >= 0; y--) {
            IBlockState top = nylium;
            IBlockState here = primer.getBlockState(localX, y, localZ);
            if (here.getMaterial() == Material.AIR) {
                left = -1;
            } else if (here.getBlock() == Blocks.NETHERRACK) {
                if (left == -1) {
                    boolean thin = false;
                    if (layers <= 0) {
                        thin = true;
                        under = NETHERRACK;
                    }
                    if (bare) top = NETHERRACK;
                    else if (wartTop) top = wart;
                    if (y < seaLevel && thin) top = LAVA;
                    left = layers;
                    primer.setBlockState(localX, y, localZ, y >= seaLevel - 1 ? top : under);
                } else if (left > 0) {
                    left--;
                    primer.setBlockState(localX, y, localZ, under);
                }
            }
        }
    }

    /**
     * 1.16's ValleySurfaceBuilder (soul sand valleys and basalt deltas): floors and ceilings take one of a few blocks,
     * whichever's own noise is highest there, and floors near the lava sea are sometimes gravel.
     */
    private static final class Valley {

        private final IBlockState[] floors;
        private final OctaveNoise[] floorNoises;
        private final IBlockState[] ceilings;
        private final OctaveNoise[] ceilingNoises;
        private final OctaveNoise gravelNoise;

        Valley(long seed, IBlockState[] floors, IBlockState[] ceilings) {
            this.floors = floors;
            this.floorNoises = noises(seed, floors.length);
            this.ceilings = ceilings;
            this.ceilingNoises = noises(seed + floors.length, ceilings.length);
            this.gravelNoise = OctaveNoise.ofOctaves(new SeedRandom(seed + floors.length + ceilings.length), 0);
        }

        private static OctaveNoise[] noises(long seed, int count) {
            OctaveNoise[] noises = new OctaveNoise[count];
            for (int i = 0; i < count; i++) noises[i] = OctaveNoise.ofOctaves(new SeedRandom(seed + i), -4);
            return noises;
        }

        private static IBlockState pick(IBlockState[] states, OctaveNoise[] noises, int x, int y, int z) {
            IBlockState best = states[0];
            double bestValue = noises[0].noise(x, y, z);
            for (int i = 1; i < states.length; i++) {
                double value = noises[i].noise(x, y, z);
                // Like 1.16's stream max: an equal value keeps the earlier block.
                if (Double.compare(value, bestValue) > 0) {
                    best = states[i];
                    bestValue = value;
                }
            }
            return best;
        }

        void build(Random random, ChunkPrimer primer, int x, int z, double depth) {
            int seaLevel = NetherFeatures.SEA_LEVEL;
            int gravelLevel = seaLevel + 1;
            int localX = x & 15;
            int localZ = z & 15;
            int ceilingDepth = (int) (depth / 3.0D + 3.0D + random.nextDouble() * 0.25D);
            int floorDepth = (int) (depth / 3.0D + 3.0D + random.nextDouble() * 0.25D);
            boolean gravel = this.gravelNoise.noise((double) x * 0.03125D, 109.0D, (double) z * 0.03125D) * 75.0D + random.nextDouble() > 0.0D;
            IBlockState ceiling = pick(this.ceilings, this.ceilingNoises, x, seaLevel, z);
            IBlockState floor = pick(this.floors, this.floorNoises, x, seaLevel, z);
            IBlockState above = primer.getBlockState(localX, 128, localZ);
            for (int y = 127; y >= 0; y--) {
                IBlockState here = primer.getBlockState(localX, y, localZ);
                boolean hereOpen = here.getMaterial() == Material.AIR || here == LAVA;
                if (above.getBlock() == Blocks.NETHERRACK && hereOpen) {
                    // The underside of netherrack above: a ceiling.
                    for (int i = 0, up = y + 1; i < ceilingDepth && up < 256; i++, up++) {
                        if (primer.getBlockState(localX, up, localZ).getBlock() != Blocks.NETHERRACK) break;
                        primer.setBlockState(localX, up, localZ, ceiling);
                    }
                }
                boolean aboveOpen = above.getMaterial() == Material.AIR || above == LAVA;
                if (aboveOpen && here.getBlock() == Blocks.NETHERRACK) {
                    // The top of netherrack under an opening: a floor.
                    for (int i = 0, down = y; i < floorDepth && down >= 0 && primer.getBlockState(localX, down, localZ).getBlock() == Blocks.NETHERRACK; i++, down--) {
                        boolean nearSea = y >= gravelLevel - 4 && y <= gravelLevel + 1;
                        primer.setBlockState(localX, down, localZ, gravel && nearSea ? GRAVEL : floor);
                    }
                }
                above = here;
            }
        }
    }

    /** 1.16's bedrock: five layers thinning out from the floor up and from the roof down. */
    static void bedrock(ChunkPrimer primer, Random random) {
        int roof = NetherTerrain.HEIGHT - 1;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (int i = 0; i < 5; i++) {
                    if (i <= random.nextInt(5)) primer.setBlockState(x, roof - i, z, BEDROCK);
                }
                for (int i = 4; i >= 0; i--) {
                    if (i <= random.nextInt(5)) primer.setBlockState(x, i, z, BEDROCK);
                }
            }
        }
    }
}
