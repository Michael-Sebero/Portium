package com.michaelsebero.portium.world.v116.feature;

import com.michaelsebero.portium.block.nether.BlockNylium;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;
import java.util.function.Function;

/**
 * Roots, fungi and sprouts scattered over nylium (1.16's NetherVegetationFeature): the forests' ground cover, and what
 * bone meal on nylium grows.
 */
public final class NetherVegetation {

    /** 87 crimson roots, 11 crimson fungi, 1 warped fungus (1.16's weights). */
    public static final Function<Random, IBlockState> CRIMSON_FOREST = random -> pick(random,
            new Block[] {PortiumBlocks.CRIMSON_ROOTS, PortiumBlocks.CRIMSON_FUNGUS, PortiumBlocks.WARPED_FUNGUS}, new int[] {87, 11, 1});

    /** 85 warped roots, 1 crimson roots, 13 warped fungi, 1 crimson fungus. */
    public static final Function<Random, IBlockState> WARPED_FOREST = random -> pick(random,
            new Block[] {PortiumBlocks.WARPED_ROOTS, PortiumBlocks.CRIMSON_ROOTS, PortiumBlocks.WARPED_FUNGUS, PortiumBlocks.CRIMSON_FUNGUS},
            new int[] {85, 1, 13, 1});

    public static final Function<Random, IBlockState> SPROUTS = random -> PortiumBlocks.NETHER_SPROUTS == null ? null
            : PortiumBlocks.NETHER_SPROUTS.getDefaultState();

    private static IBlockState pick(Random random, Block[] blocks, int[] weights) {
        int total = 0;
        for (int weight : weights) total += weight;
        int roll = random.nextInt(total);
        for (int i = 0; i < blocks.length; i++) {
            roll -= weights[i];
            if (roll < 0) return blocks[i] == null ? null : blocks[i].getDefaultState();
        }
        return null;
    }

    /**
     * Tries {@code spread * spread} spots around {@code pos} (above nylium), up to {@code spread} away and {@code
     * height} up or down, placing a plant on each empty one it can stand on. Returns whether any was placed. {@code
     * flags} are {@link com.michaelsebero.portium.world.v116.GenFlags}'.
     */
    public static boolean place(World world, Random random, BlockPos pos, Function<Random, IBlockState> plants, int spread, int height, int flags) {
        if (!BlockNylium.isNylium(world.getBlockState(pos.down()).getBlock())) return false;
        if (pos.getY() < 1 || pos.getY() + 1 >= 256) return false;
        int placed = 0;
        for (int i = 0; i < spread * spread; i++) {
            BlockPos at = pos.add(random.nextInt(spread) - random.nextInt(spread), random.nextInt(height) - random.nextInt(height),
                    random.nextInt(spread) - random.nextInt(spread));
            IBlockState plant = plants.apply(random);
            if (plant != null && world.isAirBlock(at) && at.getY() > 0 && plant.getBlock().canPlaceBlockAt(world, at)) {
                world.setBlockState(at, plant, flags);
                placed++;
            }
        }
        return placed > 0;
    }

    private NetherVegetation() {}
}
