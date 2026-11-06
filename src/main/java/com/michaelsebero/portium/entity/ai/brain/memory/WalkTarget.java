package com.michaelsebero.portium.entity.ai.brain.memory;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Where a mob walks, how fast, and how close counts as arrived. */
public class WalkTarget {

    private final PosWrapper target;
    private final float speed;
    private final int distance;

    public WalkTarget(BlockPos pos, float speed, int distance) {
        this(new BlockPosWrapper(pos), speed, distance);
    }

    public WalkTarget(Vec3d pos, float speed, int distance) {
        this(new BlockPosWrapper(new BlockPos(pos)), speed, distance);
    }

    public WalkTarget(PosWrapper target, float speed, int distance) {
        this.target = target;
        this.speed = speed;
        this.distance = distance;
    }

    public PosWrapper getTarget() {
        return this.target;
    }

    public float getSpeed() {
        return this.speed;
    }

    public int getDistance() {
        return this.distance;
    }
}
