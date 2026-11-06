package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** Walks to the ride target and mounts it once within a block. */
public class RideEntityTask<E extends EntityLivingBase> extends Task<E> {

    private final float speed;

    public RideEntityTask(float speed) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT,
                MemoryModuleType.RIDE_TARGET, MemoryModuleStatus.VALUE_PRESENT));
        this.speed = speed;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        return !entity.isRiding();
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        Entity mount = BrainUtil.brain(entity).getMemory(MemoryModuleType.RIDE_TARGET).get();
        if (mount.getDistanceSq(entity) < 1.0D) {
            entity.startRiding(mount);
        } else {
            BrainUtil.setTargetEntity(entity, mount, this.speed, 1);
        }
    }
}
