package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** Walks to whatever it's looking at. */
public class WalkTowardsLookTargetTask extends Task<EntityLivingBase> {

    private final float speed;
    private final int distance;

    public WalkTowardsLookTargetTask(float speed, int distance) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.VALUE_PRESENT));
        this.speed = speed;
        this.distance = distance;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(brain.getMemory(MemoryModuleType.LOOK_TARGET).get(), this.speed, this.distance));
    }
}
