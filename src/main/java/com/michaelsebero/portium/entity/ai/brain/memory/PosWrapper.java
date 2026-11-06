package com.michaelsebero.portium.entity.ai.brain.memory;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** 1.14's {@code IPosWrapper}: something a mob looks at or walks to, a block or an entity. */
public interface PosWrapper {

    Vec3d getPos();

    BlockPos getBlockPos();

    boolean isVisibleTo(EntityLivingBase entity);
}
