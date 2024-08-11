package com.michaelsebero.portium.entity.ai;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * 1.13's walk-and-swim pathfinding, for turtles. Water is free to cross, land costs extra, and water next to a solid
 * block costs a little (1.13's "water border" node type, which 1.12 lacks, so it is applied as a cost here).
 */
public class NodeProcessorAmphibious extends WalkNodeProcessor {

    private static final float WATER_BORDER_COST = 4.0F;
    private float oldWalkablePriority;

    @Override
    public void init(IBlockAccess world, EntityLiving mob) {
        super.init(world, mob);
        mob.setPathPriority(PathNodeType.WATER, 0.0F);
        this.oldWalkablePriority = mob.getPathPriority(PathNodeType.WALKABLE);
        mob.setPathPriority(PathNodeType.WALKABLE, 6.0F);
    }

    @Override
    public void postProcess() {
        this.entity.setPathPriority(PathNodeType.WALKABLE, this.oldWalkablePriority);
        super.postProcess();
    }

    @Override
    public PathPoint getStart() {
        AxisAlignedBB box = this.entity.getEntityBoundingBox();
        return this.openPoint(MathHelper.floor(box.minX), MathHelper.floor(box.minY + 0.5D), MathHelper.floor(box.minZ));
    }

    @Override
    public PathPoint getPathPointToCoords(double x, double y, double z) {
        return this.openPoint(MathHelper.floor(x), MathHelper.floor(y + 0.5D), MathHelper.floor(z));
    }

    @Override
    public int findPathOptions(PathPoint[] options, PathPoint current, PathPoint target, float maxDistance) {
        int count = 0;
        double ground = this.groundY(new BlockPos(current.x, current.y, current.z));
        PathPoint south = this.getSafePoint(current.x, current.y, current.z + 1, 1, ground);
        PathPoint west = this.getSafePoint(current.x - 1, current.y, current.z, 1, ground);
        PathPoint east = this.getSafePoint(current.x + 1, current.y, current.z, 1, ground);
        PathPoint north = this.getSafePoint(current.x, current.y, current.z - 1, 1, ground);
        PathPoint up = this.getSafePoint(current.x, current.y + 1, current.z, 0, ground);
        PathPoint down = this.getSafePoint(current.x, current.y - 1, current.z, 1, ground);
        for (PathPoint point : new PathPoint[] {south, west, east, north, up, down}) {
            if (usable(point, target, maxDistance)) options[count++] = point;
        }
        boolean openNorth = isOpenOrCostly(north);
        boolean openSouth = isOpenOrCostly(south);
        boolean openEast = isOpenOrCostly(east);
        boolean openWest = isOpenOrCostly(west);
        if (openNorth && openWest) count = this.addDiagonal(options, count, current.x - 1, current.y, current.z - 1, ground, target, maxDistance);
        if (openNorth && openEast) count = this.addDiagonal(options, count, current.x + 1, current.y, current.z - 1, ground, target, maxDistance);
        if (openSouth && openWest) count = this.addDiagonal(options, count, current.x - 1, current.y, current.z + 1, ground, target, maxDistance);
        if (openSouth && openEast) count = this.addDiagonal(options, count, current.x + 1, current.y, current.z + 1, ground, target, maxDistance);
        return count;
    }

    private int addDiagonal(PathPoint[] options, int count, int x, int y, int z, double ground, PathPoint target, float maxDistance) {
        PathPoint point = this.getSafePoint(x, y, z, 1, ground);
        if (usable(point, target, maxDistance)) options[count++] = point;
        return count;
    }

    private static boolean usable(@Nullable PathPoint point, PathPoint target, float maxDistance) {
        return point != null && !point.visited && point.distanceTo(target) < maxDistance;
    }

    private static boolean isOpenOrCostly(@Nullable PathPoint point) {
        return point == null || point.nodeType == PathNodeType.OPEN || point.costMalus != 0.0F;
    }

    private double groundY(BlockPos pos) {
        if (this.entity.isInWater()) return pos.getY() + 0.5D;
        BlockPos below = pos.down();
        AxisAlignedBB box = this.blockaccess.getBlockState(below).getCollisionBoundingBox(this.blockaccess, below);
        return below.getY() + (box == null ? 0.0D : box.maxY);
    }

    @Nullable
    private PathPoint getSafePoint(int x, int y, int z, int stepHeight, double ground) {
        if (this.groundY(new BlockPos(x, y, z)) - ground > 1.125D) return null;
        PathNodeType type = this.getPathNodeType(this.blockaccess, x, y, z, this.entity, this.entitySizeX, this.entitySizeY, this.entitySizeZ, false, false);
        float priority = this.entity.getPathPriority(type);
        double halfWidth = this.entity.width / 2.0D;
        PathPoint point = null;
        if (priority >= 0.0F) {
            point = this.openPoint(x, y, z);
            point.nodeType = type;
            point.costMalus = Math.max(point.costMalus, priority);
            if (type == PathNodeType.WATER && this.isWaterBorder(x, y, z)) point.costMalus = Math.max(point.costMalus, WATER_BORDER_COST);
        }
        if (type == PathNodeType.WATER || type == PathNodeType.WALKABLE) return point;

        if (point == null && stepHeight > 0 && type != PathNodeType.FENCE && type != PathNodeType.TRAPDOOR) {
            point = this.getSafePoint(x, y + 1, z, stepHeight - 1, ground);
        }
        if (type == PathNodeType.OPEN) {
            AxisAlignedBB box = new AxisAlignedBB(x - halfWidth + 0.5D, y + 0.001D, z - halfWidth + 0.5D, x + halfWidth + 0.5D, y + this.entity.height, z + halfWidth + 0.5D);
            if (this.entity.world.collidesWithAnyBlock(box)) return null;
            PathNodeType below = this.getPathNodeType(this.blockaccess, x, y - 1, z, this.entity, this.entitySizeX, this.entitySizeY, this.entitySizeZ, false, false);
            if (below == PathNodeType.BLOCKED || below == PathNodeType.WATER) {
                point = this.openPoint(x, y, z);
                point.nodeType = below == PathNodeType.BLOCKED ? PathNodeType.WALKABLE : PathNodeType.WATER;
                point.costMalus = Math.max(point.costMalus, priority);
                return point;
            }
            int fallen = 0;
            while (y > 0 && type == PathNodeType.OPEN) {
                --y;
                if (fallen++ >= this.entity.getMaxFallHeight()) return null;
                type = this.getPathNodeType(this.blockaccess, x, y, z, this.entity, this.entitySizeX, this.entitySizeY, this.entitySizeZ, false, false);
                priority = this.entity.getPathPriority(type);
                if (type != PathNodeType.OPEN && priority >= 0.0F) {
                    point = this.openPoint(x, y, z);
                    point.nodeType = type;
                    point.costMalus = Math.max(point.costMalus, priority);
                    break;
                }
                if (priority < 0.0F) return null;
            }
        }
        return point;
    }

    private boolean isWaterBorder(int x, int y, int z) {
        for (EnumFacing facing : EnumFacing.values()) {
            if (this.getPathNodeTypeRaw(this.blockaccess, x + facing.getXOffset(), y + facing.getYOffset(), z + facing.getZOffset()) == PathNodeType.BLOCKED) return true;
        }
        return false;
    }

    /** Like walking, except that standing in water is fine and the top of water counts as ground to walk on. */
    @Override
    public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z) {
        PathNodeType type = this.getPathNodeTypeRaw(world, x, y, z);
        if (type == PathNodeType.WATER) return type;
        if (type == PathNodeType.OPEN && y >= 1) {
            IBlockState belowState = world.getBlockState(new BlockPos(x, y - 1, z));
            Block below = belowState.getBlock();
            PathNodeType belowType = this.getPathNodeTypeRaw(world, x, y - 1, z);
            type = belowType != PathNodeType.WALKABLE && belowType != PathNodeType.OPEN && belowType != PathNodeType.LAVA ? PathNodeType.WALKABLE : PathNodeType.OPEN;
            if (belowType == PathNodeType.DAMAGE_FIRE || below == Blocks.MAGMA) type = PathNodeType.DAMAGE_FIRE;
            if (belowType == PathNodeType.DAMAGE_CACTUS) type = PathNodeType.DAMAGE_CACTUS;
            if (belowType == PathNodeType.DAMAGE_OTHER) type = PathNodeType.DAMAGE_OTHER;
        }
        if (type == PathNodeType.DOOR_OPEN || type == PathNodeType.DOOR_WOOD_CLOSED || type == PathNodeType.DOOR_IRON_CLOSED) type = PathNodeType.BLOCKED;
        return this.checkNeighborBlocks(world, x, y, z, type);
    }
}
