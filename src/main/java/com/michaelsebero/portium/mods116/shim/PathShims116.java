package com.michaelsebero.portium.mods116.shim;

import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * 1.16's static helpers of the walk node processor (mods' own processors call them), answered by 1.12's walk
 * processor, whose same checks are instance methods that need no mob.
 */
public final class PathShims116 extends WalkNodeProcessor {

    private static final ThreadLocal<PathShims116> PROCESSOR = ThreadLocal.withInitial(PathShims116::new);

    private PathShims116() {}

    /** 1.16's {@code getBlockNodeType(world, pos)}: the block's own type. */
    public static PathNodeType blockNodeType(IBlockAccess world, BlockPos pos) {
        return PROCESSOR.get().getPathNodeTypeRaw(world, pos.getX(), pos.getY(), pos.getZ());
    }

    /** 1.16's {@code getFloorNodeType(world, pos)}: the type standing there, its floor and neighbours counted. */
    public static PathNodeType floorNodeType(IBlockAccess world, BlockPos pos) {
        return PROCESSOR.get().getPathNodeType(world, pos.getX(), pos.getY(), pos.getZ());
    }

    /** 1.16's {@code getSurroundingDanger(world, pos, type)}. */
    public static PathNodeType surroundingDanger(IBlockAccess world, BlockPos pos, PathNodeType type) {
        return PROCESSOR.get().checkNeighborBlocks(world, pos.getX(), pos.getY(), pos.getZ(), type);
    }

    /** 1.16's {@code getGroundY(world, pos)}: the top of what's below. */
    public static double groundY(IBlockAccess world, BlockPos pos) {
        BlockPos below = pos.down();
        AxisAlignedBB box = world.getBlockState(below).getCollisionBoundingBox(world, below);
        return below.getY() + (box == null ? 0.0D : box.maxY);
    }
}
