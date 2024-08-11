package com.michaelsebero.portium.world;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.biome.BiomeFrozenOcean;
import com.michaelsebero.portium.world.biome.BiomePortiumOcean;
import com.michaelsebero.portium.world.biome.OceanTemperature;
import com.michaelsebero.portium.world.gen.feature.OceanFloor;
import com.michaelsebero.portium.world.gen.feature.WorldGenBlueIce;
import com.michaelsebero.portium.world.gen.feature.WorldGenCoralReef;
import com.michaelsebero.portium.world.gen.feature.WorldGenIceberg;
import com.michaelsebero.portium.world.gen.feature.WorldGenKelpColumn;
import com.michaelsebero.portium.world.gen.feature.WorldGenSeaPickles;
import com.michaelsebero.portium.world.gen.feature.WorldGenSeagrassPatch;
import com.michaelsebero.portium.world.gen.layer.GenLayerMixOceans;
import com.michaelsebero.portium.world.gen.layer.GenLayerOceanTemperature;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * 1.13 ocean world generation: the ocean temperature biome layers, and the decoration that fills oceans with
 * kelp, seagrass, coral reefs, sea pickles, icebergs and blue ice.
 * <p>
 * Feature placement follows 1.12's population rules: everything is placed inside the 16x16 area offset by 8 blocks
 * from the chunk corner, so nothing forces neighbouring chunks to generate.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class OceanWorldGen {

    /** Vanilla's biome information noise (1.12 GRASS_COLOR_NOISE, 1.13 INFO_NOISE), recreated with the same seed. */
    private static final NoiseGeneratorPerlin INFO_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    private static final WorldGenKelpColumn KELP = new WorldGenKelpColumn();
    private static final WorldGenCoralReef CORAL_REEF = new WorldGenCoralReef();
    private static final WorldGenSeaPickles SEA_PICKLES = new WorldGenSeaPickles(20);
    private static final WorldGenBlueIce BLUE_ICE = new WorldGenBlueIce();
    private static WorldGenIceberg packedIceberg;
    private static WorldGenIceberg blueIceberg;

    /** The biome layer event fires on the terrain bus, so it needs registering there. */
    public static void registerTerrainEvents() {
        MinecraftForge.TERRAIN_GEN_BUS.register(new TerrainBus());
    }

    public static final class TerrainBus {

        @SubscribeEvent
        public void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
            if (!PortiumConfig.worldGen.oceanVariants || !WorldGeneration.isBuildingModern()) return;
            WorldType type = event.getWorldType();
            if (type == WorldType.FLAT || type == WorldType.DEBUG_ALL_BLOCK_STATES || type == WorldType.DEFAULT_1_1) return;
            GenLayer[] layers = event.getNewBiomeGens();
            if (layers.length < 2) return;

            long seed = event.getSeed();
            GenLayer temperatures = GenLayerZoom.magnify(2001L, new GenLayerOceanTemperature(2L), 6);
            GenLayer mixed = new GenLayerMixOceans(100L, layers[0], temperatures);
            GenLayer voronoi = new GenLayerVoronoiZoom(10L, mixed);
            mixed.initWorldGenSeed(seed);
            voronoi.initWorldGenSeed(seed);
            GenLayer[] replaced = layers.clone();
            replaced[0] = mixed;
            replaced[1] = voronoi;
            if (replaced.length > 2) replaced[2] = mixed;
            event.setNewBiomeGens(replaced);
        }
    }

    // ---- Decoration ----

    private enum OceanKind { WARM, LUKEWARM, NORMAL, COLD, FROZEN, RIVER, SWAMP, NONE }

    private static OceanKind kindOf(Biome biome) {
        if (biome instanceof BiomeFrozenOcean) return OceanKind.FROZEN;
        if (biome instanceof BiomePortiumOcean) {
            OceanTemperature temperature = ((BiomePortiumOcean) biome).getOceanTemperature();
            return temperature == OceanTemperature.WARM ? OceanKind.WARM : temperature == OceanTemperature.LUKEWARM ? OceanKind.LUKEWARM : OceanKind.COLD;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
            return BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY) ? OceanKind.FROZEN : OceanKind.NORMAL;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
            return BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY) ? OceanKind.NONE : OceanKind.RIVER;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)) return OceanKind.SWAMP;
        return OceanKind.NONE;
    }

    private static boolean isDeep(Biome biome) {
        return biome == Biomes.DEEP_OCEAN || biome.getBaseHeight() <= -1.8F;
    }

    /**
     * The biome that decides what decorates the 16x16 area starting at {@code origin}: the one at the area's far
     * corner, as in 1.13.2, which decorates a chunk by the biome of the next chunk along diagonally. Plants keep
     * 1.13's offset from biome borders: an ocean chunk beside a beach can be bare, a beach's edge can have kelp.
     */
    private static Biome decorationBiome(World world, BlockPos origin) {
        return world.getBiome(origin.add(16, 0, 16));
    }

    private static int noiseCount(BlockPos origin, int ratio, double factor) {
        double noise = INFO_NOISE.getValue(origin.getX() / factor, origin.getZ() / factor);
        return (int) Math.ceil(noise * ratio);
    }

    @SubscribeEvent
    public static void onDecoratePre(DecorateBiomeEvent.Pre event) {
        World world = event.getWorld();
        if (!WorldGeneration.isModern(world)) return;
        BlockPos origin = event.getChunkPos().getBlock(8, 0, 8);
        Biome biome = decorationBiome(world, origin);
        if (kindOf(biome) != OceanKind.FROZEN || !PortiumConfig.worldGen.icebergs) return;
        Random rand = event.getRand();
        if (packedIceberg == null) {
            packedIceberg = new WorldGenIceberg(Blocks.PACKED_ICE.getDefaultState());
            blueIceberg = PortiumBlocks.BLUE_ICE == null ? null : new WorldGenIceberg(PortiumBlocks.BLUE_ICE.getDefaultState());
        }
        BlockPos icebergPos = new BlockPos(origin.getX() + 4 + rand.nextInt(8), world.getSeaLevel(), origin.getZ() + 4 + rand.nextInt(8));
        if (rand.nextInt(16) == 0) packedIceberg.generate(world, rand, icebergPos);
        if (rand.nextInt(200) == 0 && blueIceberg != null) blueIceberg.generate(world, rand, icebergPos);
    }

    @SubscribeEvent
    public static void onDecoratePost(DecorateBiomeEvent.Post event) {
        World world = event.getWorld();
        if (!WorldGeneration.isModern(world)) return;
        Random rand = event.getRand();
        BlockPos origin = event.getChunkPos().getBlock(8, 0, 8);
        Biome biome = decorationBiome(world, origin);
        OceanKind kind = kindOf(biome);
        if (kind == OceanKind.NONE) return;
        boolean deep = isDeep(biome);

        // In 1.13's order: coral, then seagrass, then sea pickles or kelp, each taking the floor spots left to it.
        if (kind == OceanKind.WARM && PortiumConfig.worldGen.coralReefs && !PortiumBlocks.CORAL_BLOCKS.isEmpty()) {
            int reefs = noiseCount(origin, 20, 400.0D);
            for (int i = 0; i < reefs; i++) {
                CORAL_REEF.generate(world, rand, OceanFloor.at(world, origin.getX() + rand.nextInt(16), origin.getZ() + rand.nextInt(16)));
            }
        }

        if (PortiumConfig.worldGen.seagrass && kind != OceanKind.FROZEN && PortiumBlocks.SEAGRASS != null) {
            int count;
            double tall;
            switch (kind) {
                case WARM:
                case LUKEWARM: count = 80; tall = deep ? 0.8D : 0.3D; break;
                case COLD: count = deep ? 40 : 32; tall = deep ? 0.8D : 0.3D; break;
                case RIVER: count = 48; tall = 0.4D; break;
                case SWAMP: count = 64; tall = 0.6D; break;
                default: count = 48; tall = deep ? 0.8D : 0.3D; break;
            }
            new WorldGenSeagrassPatch(count, tall).generate(world, rand, origin.add(rand.nextInt(16), 0, rand.nextInt(16)));
        }

        if (kind == OceanKind.WARM && PortiumConfig.worldGen.coralReefs && PortiumBlocks.SEA_PICKLE != null && rand.nextInt(16) == 0) {
            SEA_PICKLES.generate(world, rand, origin.add(rand.nextInt(16), 0, rand.nextInt(16)));
        }

        if (PortiumConfig.worldGen.kelp && PortiumBlocks.KELP != null && (kind == OceanKind.NORMAL || kind == OceanKind.COLD || kind == OceanKind.LUKEWARM)) {
            int columns = noiseCount(origin, kind == OceanKind.LUKEWARM ? 80 : 120, 80.0D);
            for (int i = 0; i < columns; i++) {
                KELP.generate(world, rand, OceanFloor.at(world, origin.getX() + rand.nextInt(16), origin.getZ() + rand.nextInt(16)));
            }
        }

        if (kind == OceanKind.FROZEN && PortiumConfig.worldGen.icebergs && PortiumBlocks.BLUE_ICE != null) {
            int attempts = rand.nextInt(20);
            for (int i = 0; i < attempts; i++) {
                BLUE_ICE.generate(world, rand, origin.add(rand.nextInt(16), 30 + rand.nextInt(32), rand.nextInt(16)));
            }
        }
    }

    private OceanWorldGen() {}
}
