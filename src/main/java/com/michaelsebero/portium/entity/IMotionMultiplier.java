package com.michaelsebero.portium.entity;

import net.minecraft.util.math.Vec3d;

/** 1.14's {@code Entity.setMotionMultiplier}: the next move is scaled by it (sweet berry bushes), mixed into Entity. */
public interface IMotionMultiplier {

    void portium$setMotionMultiplier(Vec3d multiplier);
}
