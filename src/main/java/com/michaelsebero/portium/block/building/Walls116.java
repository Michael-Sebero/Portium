package com.michaelsebero.portium.block.building;

import com.michaelsebero.portium.PortiumConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockWall;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * 1.16's walls on 1.12's: a side rises to the top of the block (1.16's "tall") where the block above covers it, a
 * post shows only where 1.16 shows one, and walls join glass panes and iron bars. 1.12 left gaps between stacked
 * walls, and posts along every straight run with anything on top. The heights are worked out like the rest of a
 * wall's connections, from its surroundings, so 1.12's walls (and mods' walls built on them) all get them.
 */
public final class Walls116 {

    /** Whether a connected side is full height, in sixteenths of the block above's footprint (1.16's {@code WallHeight.TALL}). */
    public static final PropertyBool NORTH_TALL = PropertyBool.create("north_tall");
    public static final PropertyBool EAST_TALL = PropertyBool.create("east_tall");
    public static final PropertyBool SOUTH_TALL = PropertyBool.create("south_tall");
    public static final PropertyBool WEST_TALL = PropertyBool.create("west_tall");

    /** 1.16's test shapes: x1, z1, x2, z2 in sixteenths, a strip from the centre to each side, and the centre pole. */
    private static final int[] NORTH = {7, 0, 9, 9};
    private static final int[] EAST = {7, 7, 16, 9};
    private static final int[] SOUTH = {7, 7, 9, 16};
    private static final int[] WEST = {0, 7, 9, 9};
    private static final int[] CENTER = {7, 7, 9, 9};

    /** Walls stacked higher than this aren't asked about the walls above them (each asks the next). */
    private static final int MAX_DEPTH = 8;
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /** A wall's state with 1.12's connections worked out (its actual state), given 1.16's heights and post. */
    public static IBlockState apply(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (!state.getPropertyKeys().contains(NORTH_TALL)) return state;
        if (!PortiumConfig.mechanics.walls116) {
            return state.withProperty(NORTH_TALL, false).withProperty(EAST_TALL, false).withProperty(SOUTH_TALL, false).withProperty(WEST_TALL, false);
        }
        int[] depth = DEPTH.get();
        if (depth[0] >= MAX_DEPTH) return state;
        depth[0]++;
        try {
            BlockPos up = pos.up();
            IBlockState above = world.getBlockState(up);
            AxisAlignedBB footprint = footprint(above, world, up);
            boolean north = state.getValue(BlockWall.NORTH);
            boolean east = state.getValue(BlockWall.EAST);
            boolean south = state.getValue(BlockWall.SOUTH);
            boolean west = state.getValue(BlockWall.WEST);
            boolean tallNorth = north && covers(footprint, NORTH);
            boolean tallEast = east && covers(footprint, EAST);
            boolean tallSouth = south && covers(footprint, SOUTH);
            boolean tallWest = west && covers(footprint, WEST);
            boolean post;
            if (above.getBlock() instanceof BlockWall && hasPost(above, world, up)) {
                post = true;
            } else if (!north && !east && !south && !west || north != south || east != west) {
                // Alone, an end or a corner.
                post = true;
            } else if (tallNorth && tallSouth || tallEast && tallWest) {
                post = false;
            } else {
                post = isPostOverride(above.getBlock()) || covers(footprint, CENTER);
            }
            return state.withProperty(BlockWall.UP, post).withProperty(NORTH_TALL, tallNorth).withProperty(EAST_TALL, tallEast)
                    .withProperty(SOUTH_TALL, tallSouth).withProperty(WEST_TALL, tallWest);
        } finally {
            depth[0]--;
        }
    }

    /** Whether a wall's sides reach the top somewhere, for its outline. */
    public static boolean anyTall(IBlockState state) {
        return state.getPropertyKeys().contains(NORTH_TALL)
                && (state.getValue(NORTH_TALL) || state.getValue(EAST_TALL) || state.getValue(SOUTH_TALL) || state.getValue(WEST_TALL));
    }

    private static boolean hasPost(IBlockState wall, IBlockAccess world, BlockPos pos) {
        IBlockState actual = wall.getActualState(world, pos);
        return actual.getPropertyKeys().contains(BlockWall.UP) && actual.getValue(BlockWall.UP);
    }

    /** 1.16's {@code wall_post_override} tag: what stands on a post (torches, signs, banners, tripwire). */
    private static boolean isPostOverride(Block block) {
        return block instanceof BlockTorch || block instanceof BlockSign || block instanceof BlockBanner || block instanceof BlockTripWire;
    }

    /** The block above's collision where it meets the wall (1.16 projects its shape down); null if it doesn't. */
    @Nullable
    private static AxisAlignedBB footprint(IBlockState above, IBlockAccess world, BlockPos up) {
        AxisAlignedBB box;
        try {
            box = above.getCollisionBoundingBox(world, up);
        } catch (RuntimeException e) {
            return null;
        }
        return box != null && box.minY <= 1.0E-6 && box.maxY > 1.0E-6 ? box : null;
    }

    private static boolean covers(@Nullable AxisAlignedBB box, int[] area) {
        double eps = 1.0E-6;
        return box != null && box.minX * 16 <= area[0] + eps && box.minZ * 16 <= area[1] + eps && box.maxX * 16 >= area[2] - eps
                && box.maxZ * 16 >= area[3] - eps;
    }

    private Walls116() {}
}
