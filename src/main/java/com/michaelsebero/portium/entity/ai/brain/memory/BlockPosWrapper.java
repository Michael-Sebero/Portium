package com.michaelsebero.portium.entity.ai.brain.memory;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** A block's centre as a look or walk target. */
public class BlockPosWrapper implements PosWrapper {

    private final BlockPos pos;
    private final Vec3d centre;

    public BlockPosWrapper(BlockPos pos) {
        this.pos = pos;
        this.centre = new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    @Override
    public Vec3d getPos() {
        return this.centre;
    }

    @Override
    public BlockPos getBlockPos() {
        return this.pos;
    }

    @Override
    public boolean isVisibleTo(EntityLivingBase entity) {
        return true;
    }

    @Override
    public String toString() {
        return "BlockPosTracker{blockPos=" + this.pos + ", centerPosition=" + this.centre + '}';
    }
}
