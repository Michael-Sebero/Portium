package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.entity.passive.EntityDolphin;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 1.13's swimming navigator. Compared with 1.12's it accepts waypoints from further away when moving fast, which
 * keeps quick swimmers like dolphins from circling their waypoints, and dolphins may path through the surface.
 */
public class PathNavigateWater extends PathNavigate {

    private boolean breaching;

    public PathNavigateWater(EntityLiving entity, World world) {
        super(entity, world);
    }

    @Override
    protected PathFinder getPathFinder() {
        // Called from the super constructor, before this class's fields are assigned.
        this.breaching = this.entity instanceof EntityDolphin;
        this.nodeProcessor = new NodeProcessorSwim(this.breaching);
        return new PathFinder(this.nodeProcessor);
    }

    @Override
    protected boolean canNavigate() {
        return this.breaching || this.isInLiquid();
    }

    @Override
    protected Vec3d getEntityPosition() {
        return new Vec3d(this.entity.posX, this.entity.posY + this.entity.height * 0.5D, this.entity.posZ);
    }

    @Override
    protected void pathFollow() {
        Vec3d position = this.getEntityPosition();
        float width = this.entity.width;
        float reach = width > 0.75F ? width / 2.0F : 0.75F - width / 2.0F;
        if (Math.abs(this.entity.motionX) > 0.2D || Math.abs(this.entity.motionZ) > 0.2D) {
            reach *= (float) Math.sqrt(this.entity.motionX * this.entity.motionX + this.entity.motionY * this.entity.motionY
                    + this.entity.motionZ * this.entity.motionZ) * 6.0F;
        }
        Vec3d node = this.currentPath.getCurrentPos();
        if (Math.abs(this.entity.posX - (node.x + 0.5D)) < reach && Math.abs(this.entity.posZ - (node.z + 0.5D)) < reach
                && Math.abs(this.entity.posY - node.y) < reach * 2.0F) {
            this.currentPath.incrementPathIndex();
        }
        for (int i = Math.min(this.currentPath.getCurrentPathIndex() + 6, this.currentPath.getCurrentPathLength() - 1); i > this.currentPath.getCurrentPathIndex(); --i) {
            Vec3d ahead = this.currentPath.getVectorFromIndex(this.entity, i);
            if (ahead.squareDistanceTo(position) <= 36.0D && this.isDirectPathBetweenPoints(position, ahead, 0, 0, 0)) {
                this.currentPath.setCurrentPathIndex(i);
                break;
            }
        }
        this.checkForStuck(position);
    }

    @Override
    protected boolean isDirectPathBetweenPoints(Vec3d from, Vec3d to, int sizeX, int sizeY, int sizeZ) {
        RayTraceResult hit = this.world.rayTraceBlocks(from, new Vec3d(to.x, to.y + this.entity.height * 0.5D, to.z), false, true, false);
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS;
    }

    @Override
    public boolean canEntityStandOnPos(BlockPos pos) {
        return !this.world.getBlockState(pos).isFullBlock();
    }
}
