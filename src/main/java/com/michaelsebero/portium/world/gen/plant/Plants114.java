package com.michaelsebero.portium.world.gen.plant;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.block.plant.BlockBamboo;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.WorldGeneration;
import com.michaelsebero.portium.world.v116.GenFlags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomePlains;
import net.minecraft.world.biome.BiomeTaiga;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Where 1.14's plants grow in worlds generated as 1.16.5 (1.12's overworld, so this follows 1.12's decoration and
 * 1.16's placement rules for the new plants):
 * <ul>
 * <li>Plains and the flower forest (1.12's mutated forest) pick their flower patches as 1.14 does, which adds cornflowers and (flower forests)
 * lilies of the valley ({@code MixinBiomeDecoratorFlowers}).</li>
 * <li>Forests' tall flower patches may be lily of the valley patches, one pick in four ({@code MixinBiomeForestFlowers}).</li>
 * <li>Taigas get sweet berry patches (snowy ones rarely); jungles (not edges or modified ones) get bamboo.</li>
 * </ul>
 * Bone meal on grass gives the new flowers in plains and flower forests in every world.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class Plants114 {

    /** 1.12's GRASS_COLOR_NOISE (1.14's INFO_NOISE), recreated with its seed. */
    private static final NoiseGeneratorPerlin INFO_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    public static boolean applies(World world) {
        return PortiumConfig.worldGen.plants114 && WorldGeneration.is116(world);
    }

    // ---- Flowers ----

    /** 1.14's flower for a plains or flower forest patch, or null to keep 1.12's pick (other biomes). */
    @Nullable
    public static IBlockState pickFlower(Biome biome, Random rand, BlockPos pos) {
        if (biome instanceof BiomePlains) return plainsFlower(rand, pos);
        if (biome == Biomes.MUTATED_FOREST) return forestFlower(pos);
        return null;
    }

    /** 1.14's PlainFlowerBlockStateProvider. */
    private static IBlockState plainsFlower(Random rand, BlockPos pos) {
        double noise = INFO_NOISE.getValue((double) pos.getX() / 200.0D, (double) pos.getZ() / 200.0D);
        if (noise < -0.8D) {
            BlockFlower.EnumFlowerType[] tulips = {BlockFlower.EnumFlowerType.ORANGE_TULIP, BlockFlower.EnumFlowerType.RED_TULIP,
                    BlockFlower.EnumFlowerType.PINK_TULIP, BlockFlower.EnumFlowerType.WHITE_TULIP};
            return red(tulips[rand.nextInt(tulips.length)]);
        }
        if (rand.nextInt(3) > 0) {
            int i = rand.nextInt(4);
            if (i == 3 && PortiumBlocks.CORNFLOWER != null) return PortiumBlocks.CORNFLOWER.getDefaultState();
            return red(i == 0 ? BlockFlower.EnumFlowerType.POPPY : i == 1 ? BlockFlower.EnumFlowerType.HOUSTONIA : BlockFlower.EnumFlowerType.OXEYE_DAISY);
        }
        return Blocks.YELLOW_FLOWER.getDefaultState();
    }

    /** 1.14's ForestFlowerBlockStateProvider: the flower by place, from eleven (no blue orchid). */
    private static IBlockState forestFlower(BlockPos pos) {
        double noise = MathHelper.clamp((1.0D + INFO_NOISE.getValue((double) pos.getX() / 48.0D, (double) pos.getZ() / 48.0D)) / 2.0D, 0.0D, 0.9999D);
        int i = (int) (noise * 11.0D);
        switch (i) {
            case 0: return Blocks.YELLOW_FLOWER.getDefaultState();
            case 1: return red(BlockFlower.EnumFlowerType.POPPY);
            case 2: return red(BlockFlower.EnumFlowerType.ALLIUM);
            case 3: return red(BlockFlower.EnumFlowerType.HOUSTONIA);
            case 4: return red(BlockFlower.EnumFlowerType.RED_TULIP);
            case 5: return red(BlockFlower.EnumFlowerType.ORANGE_TULIP);
            case 6: return red(BlockFlower.EnumFlowerType.WHITE_TULIP);
            case 7: return red(BlockFlower.EnumFlowerType.PINK_TULIP);
            case 8: return red(BlockFlower.EnumFlowerType.OXEYE_DAISY);
            case 9: return PortiumBlocks.CORNFLOWER != null ? PortiumBlocks.CORNFLOWER.getDefaultState() : red(BlockFlower.EnumFlowerType.POPPY);
            default:
                return PortiumBlocks.LILY_OF_THE_VALLEY != null ? PortiumBlocks.LILY_OF_THE_VALLEY.getDefaultState() : red(BlockFlower.EnumFlowerType.POPPY);
        }
    }

    private static IBlockState red(BlockFlower.EnumFlowerType type) {
        return Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), type);
    }

    /** 1.12's WorldGenFlowers, for any flower: 64 tries around the spot, on ground it can grow on. */
    public static void flowerPatch(World world, Random rand, BlockPos center, IBlockState flower) {
        Block block = flower.getBlock();
        for (int i = 0; i < 64; i++) {
            BlockPos pos = center.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (world.isAirBlock(pos) && pos.getY() < 255 && (!(block instanceof BlockBush) || ((BlockBush) block).canBlockStay(world, pos, flower))) {
                world.setBlockState(pos, flower, 2);
            }
        }
    }

    /** A lily of the valley patch in a forest (1.14's NO_BONEMEAL_FLOWER: 64 tries, spread 7/3/7, on the surface). */
    public static void lilyPatch(World world, Random rand, BlockPos chunkCorner) {
        if (PortiumBlocks.LILY_OF_THE_VALLEY == null) return;
        BlockPos top = world.getHeight(chunkCorner.add(rand.nextInt(16) + 8, 0, rand.nextInt(16) + 8));
        flowerPatch(world, rand, top, PortiumBlocks.LILY_OF_THE_VALLEY.getDefaultState());
    }

    // ---- Berries and bamboo ----

    @SubscribeEvent
    public static void onDecorate(DecorateBiomeEvent.Post event) {
        World world = event.getWorld();
        if (!applies(world)) return;
        Random rand = event.getRand();
        BlockPos corner = event.getChunkPos().getBlock(0, 0, 0);
        Biome biome = world.getBiome(corner.add(16, 0, 16));
        if (biome instanceof BiomeTaiga && PortiumBlocks.SWEET_BERRY_BUSH != null) {
            // 1.16: a patch every chunk (sparse), one chunk in twelve in snowy taigas.
            if (!biome.getEnableSnow() || rand.nextInt(12) == 0) {
                BlockPos center = doubledHeight(world, rand, corner);
                if (center != null) berryPatch(world, rand, center);
            }
        }
        if ((biome == Biomes.JUNGLE || biome == Biomes.JUNGLE_HILLS) && PortiumBlocks.BAMBOO != null) {
            for (int i = 0; i < 16; i++) {
                BlockPos at = doubledHeight(world, rand, corner);
                if (at != null) bamboo(world, rand, at);
            }
        }
    }

    /** 1.16's heightmap_spread_double placement: a column in the chunk, at a height up to twice its surface. */
    @Nullable
    private static BlockPos doubledHeight(World world, Random rand, BlockPos corner) {
        int x = rand.nextInt(16) + 8;
        int z = rand.nextInt(16) + 8;
        int height = world.getHeight(corner.add(x, 0, z)).getY();
        return height <= 0 ? null : corner.add(x, rand.nextInt(height * 2), z);
    }

    /** 1.16's patch_berry_bush: ripe bushes on grass blocks, 64 tries. */
    public static void berryPatch(World world, Random rand, BlockPos center) {
        IBlockState bush = PortiumBlocks.SWEET_BERRY_BUSH.getDefaultState().withProperty(com.michaelsebero.portium.block.plant.BlockSweetBerryBush.AGE, 3);
        for (int i = 0; i < 64; i++) {
            BlockPos pos = center.add(rand.nextInt(8) - rand.nextInt(8), rand.nextInt(4) - rand.nextInt(4), rand.nextInt(8) - rand.nextInt(8));
            if (world.isAirBlock(pos) && world.getBlockState(pos.down()).getBlock() == Blocks.GRASS
                    && ((BlockBush) PortiumBlocks.SWEET_BERRY_BUSH).canBlockStay(world, pos, bush)) {
                world.setBlockState(pos, bush, GenFlags.GENERATION);
            }
        }
    }

    /** 1.16's BambooFeature without podzol: a 5 to 16 high stalk with leaves at its top. */
    public static void bamboo(World world, Random rand, BlockPos pos) {
        if (!world.isAirBlock(pos) || !BlockBamboo.isPlantableOn(world.getBlockState(pos.down()))) return;
        int height = rand.nextInt(12) + 5;
        IBlockState base = PortiumBlocks.BAMBOO.getDefaultState().withProperty(BlockBamboo.AGE, 1).withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.NONE)
                .withProperty(BlockBamboo.STAGE, 0);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos);
        for (int i = 0; i < height && world.isAirBlock(at); i++) {
            world.setBlockState(at, base, GenFlags.GENERATION);
            at.move(net.minecraft.util.EnumFacing.UP);
        }
        if (at.getY() - pos.getY() >= 3) {
            world.setBlockState(at, base.withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.LARGE).withProperty(BlockBamboo.STAGE, 1), GenFlags.GENERATION);
            world.setBlockState(at.move(net.minecraft.util.EnumFacing.DOWN), base.withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.LARGE), GenFlags.GENERATION);
            world.setBlockState(at.move(net.minecraft.util.EnumFacing.DOWN), base.withProperty(BlockBamboo.LEAVES, BlockBamboo.Leaves.SMALL), GenFlags.GENERATION);
        }
    }

    // ---- Bone meal ----

    /** Bone meal on grass gives 1.14's flowers where they grow (Forge's per-biome flower lists). */
    public static void addBonemealFlowers() {
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (biome instanceof BiomePlains && PortiumBlocks.CORNFLOWER != null) {
                biome.addFlower(PortiumBlocks.CORNFLOWER.getDefaultState(), 20);
            } else if (biome == Biomes.MUTATED_FOREST) {
                if (PortiumBlocks.CORNFLOWER != null) biome.addFlower(PortiumBlocks.CORNFLOWER.getDefaultState(), 10);
                if (PortiumBlocks.LILY_OF_THE_VALLEY != null) biome.addFlower(PortiumBlocks.LILY_OF_THE_VALLEY.getDefaultState(), 10);
            }
        }
    }

    private Plants114() {}
}
