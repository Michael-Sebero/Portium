package com.michaelsebero.portium.world.v116.feature;

import com.michaelsebero.portium.block.nether.BlockNetherVine;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.world.v116.GenFlags;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/** Weeping vines hanging under crimson forest ceilings, and twisting vines rising in warped forests (1.16). */
public final class NetherVines {

    private static final EnumFacing[] DIRECTIONS = EnumFacing.values();

    /** A cluster of weeping vines under a ceiling, on a patch of nether wart block (1.16's WeepingVineFeature). */
    public static boolean placeWeeping(World world, Random random, BlockPos pos) {
        if (PortiumBlocks.WEEPING_VINES == null || !world.isAirBlock(pos)) return false;
        Block above = world.getBlockState(pos.up()).getBlock();
        if (above != Blocks.NETHERRACK && above != Blocks.NETHER_WART_BLOCK) return false;
        placeWartRoof(world, random, pos);
        hangWeeping(world, random, pos);
        return true;
    }

    private static void placeWartRoof(World world, Random random, BlockPos pos) {
        world.setBlockState(pos, Blocks.NETHER_WART_BLOCK.getDefaultState(), GenFlags.GENERATION);
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 200; i++) {
            at.setPos(pos.getX() + random.nextInt(6) - random.nextInt(6), pos.getY() + random.nextInt(2) - random.nextInt(5),
                    pos.getZ() + random.nextInt(6) - random.nextInt(6));
            if (!world.isAirBlock(at)) continue;
            int touching = 0;
            for (EnumFacing side : DIRECTIONS) {
                Block block = world.getBlockState(at.offset(side)).getBlock();
                if (block == Blocks.NETHERRACK || block == Blocks.NETHER_WART_BLOCK) touching++;
                if (touching > 1) break;
            }
            if (touching == 1) world.setBlockState(at, Blocks.NETHER_WART_BLOCK.getDefaultState(), GenFlags.GENERATION);
        }
    }

    private static void hangWeeping(World world, Random random, BlockPos pos) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 100; i++) {
            at.setPos(pos.getX() + random.nextInt(8) - random.nextInt(8), pos.getY() + random.nextInt(2) - random.nextInt(7),
                    pos.getZ() + random.nextInt(8) - random.nextInt(8));
            if (!world.isAirBlock(at)) continue;
            Block above = world.getBlockState(at.up()).getBlock();
            if (above != Blocks.NETHERRACK && above != Blocks.NETHER_WART_BLOCK) continue;
            int length = MathHelper.getInt(random, 1, 8);
            if (random.nextInt(6) == 0) length *= 2;
            if (random.nextInt(5) == 0) length = 1;
            weepingColumn(world, random, at, length, 17, 25, GenFlags.GENERATION);
        }
    }

    /** A weeping vine hanging down from {@code start}: {@code length} long, ending in an end aged between the bounds. */
    public static void weepingColumn(World world, Random random, BlockPos start, int length, int minAge, int maxAge, int flags) {
        column(world, random, start, length, minAge, maxAge, EnumFacing.DOWN, (BlockNetherVine) PortiumBlocks.WEEPING_VINES, flags);
    }

    /** Twisting vines rising from the ground around {@code pos} (1.16's TwistingVineFeature). */
    public static boolean spreadTwisting(World world, Random random, BlockPos pos, int spread, int height, int maxLength, int flags) {
        if (PortiumBlocks.TWISTING_VINES == null || cannotStartTwisting(world, pos)) return false;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < spread * spread; i++) {
            at.setPos(pos.getX() + MathHelper.getInt(random, -spread, spread), pos.getY() + MathHelper.getInt(random, -height, height),
                    pos.getZ() + MathHelper.getInt(random, -spread, spread));
            if (!dropToGround(world, at) || cannotStartTwisting(world, at)) continue;
            int length = MathHelper.getInt(random, 1, maxLength);
            if (random.nextInt(6) == 0) length *= 2;
            if (random.nextInt(5) == 0) length = 1;
            column(world, random, at.toImmutable(), length, 17, 25, EnumFacing.UP, (BlockNetherVine) PortiumBlocks.TWISTING_VINES, flags);
        }
        return true;
    }

    /** Moves down through air to the first spot above the ground; false if it falls out of the world. */
    private static boolean dropToGround(World world, BlockPos.MutableBlockPos at) {
        do {
            at.move(EnumFacing.DOWN);
            if (at.getY() < 0) return false;
        } while (world.isAirBlock(at));
        at.move(EnumFacing.UP);
        return true;
    }

    private static boolean cannotStartTwisting(World world, BlockPos pos) {
        if (!world.isAirBlock(pos)) return true;
        Block below = world.getBlockState(pos.down()).getBlock();
        return below != Blocks.NETHERRACK && below != PortiumBlocks.WARPED_NYLIUM && below != PortiumBlocks.WARPED_WART_BLOCK;
    }

    private static void column(World world, Random random, BlockPos start, int length, int minAge, int maxAge, EnumFacing growth, BlockNetherVine top,
                               int flags) {
        BlockPos at = start;
        for (int i = growth == EnumFacing.DOWN ? 0 : 1; i <= length; i++) {
            if (world.isAirBlock(at)) {
                if (i == length || !world.isAirBlock(at.offset(growth))) {
                    IBlockState end = top.getDefaultState().withProperty(BlockNetherVine.AGE, MathHelper.getInt(random, minAge, maxAge));
                    world.setBlockState(at, end, flags);
                    break;
                }
                world.setBlockState(at, top.getBody().getDefaultState(), flags);
            }
            at = at.offset(growth);
        }
    }

    private NetherVines() {}
}
