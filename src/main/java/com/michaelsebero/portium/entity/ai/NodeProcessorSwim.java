package com.michaelsebero.portium.entity.ai;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * Paths through water (1.13). With breaching allowed, the air just above the surface is also usable, so dolphins
 * can path up to breathe and jump out of the water.
 */
public class NodeProcessorSwim extends NodeProcessor {

    private final boolean allowBreaching;

    public NodeProcessorSwim(boolean allowBreaching) {
        this.allowBreaching = allowBreaching;
    }

    @Override
    public PathPoint getStart() {
        return this.openPoint(MathHelper.floor(this.entity.getEntityBoundingBox().minX), MathHelper.floor(this.entity.getEntityBoundingBox().minY + 0.5D),
                MathHelper.floor(this.entity.getEntityBoundingBox().minZ));
    }

    @Override
    public PathPoint getPathPointToCoords(double x, double y, double z) {
        return this.openPoint(MathHelper.floor(x - this.entity.width / 2.0F), MathHelper.floor(y + 0.5D), MathHelper.floor(z - this.entity.width / 2.0F));
    }

    @Override
    public int findPathOptions(PathPoint[] options, PathPoint current, PathPoint target, float maxDistance) {
        int count = 0;
        for (EnumFacing facing : EnumFacing.values()) {
            PathPoint point = this.getWaterNode(current.x + facing.getXOffset(), current.y + facing.getYOffset(), current.z + facing.getZOffset());
            if (point != null && !point.visited && point.distanceTo(target) < maxDistance) options[count++] = point;
        }
        return count;
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z, EntityLiving entity, int xSize, int ySize, int zSize, boolean canBreakDoors, boolean canEnterDoors) {
        return this.getPathNodeType(world, x, y, z);
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() == Material.AIR && isSwimmable(world.getBlockState(pos.down()))) return PathNodeType.OPEN;
        return isSwimmable(state) ? PathNodeType.WATER : PathNodeType.BLOCKED;
    }

    private static boolean isSwimmable(IBlockState state) {
        return state.getMaterial() == Material.WATER && !state.getMaterial().blocksMovement();
    }

    @Nullable
    private PathPoint getWaterNode(int x, int y, int z) {
        PathNodeType type = this.isFree(x, y, z);
        return type == PathNodeType.WATER || this.allowBreaching && type == PathNodeType.OPEN ? this.openPoint(x, y, z) : null;
    }

    /** {@link PathNodeType#OPEN} stands in for 1.13's "breach" type: air directly above water. */
    private PathNodeType isFree(int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = x; i < x + this.entitySizeX; ++i) {
            for (int j = y; j < y + this.entitySizeY; ++j) {
                for (int k = z; k < z + this.entitySizeZ; ++k) {
                    IBlockState state = this.blockaccess.getBlockState(pos.setPos(i, j, k));
                    if (state.getMaterial() == Material.AIR && isSwimmable(this.blockaccess.getBlockState(pos.down()))) return PathNodeType.OPEN;
                    if (!isSwimmable(state)) return PathNodeType.BLOCKED;
                }
            }
        }
        return PathNodeType.WATER;
    }
}
