package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** Backs away from a seen attack target that's too close (piglins with crossbows). */
public class AttackStrafingTask<E extends EntityLiving> extends Task<E> {

    private final int distance;
    private final float speed;

    public AttackStrafingTask(int distance, float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleStatus.VALUE_PRESENT));
        this.distance = distance;
        this.speed = speed;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        EntityLivingBase target = target(entity);
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.VISIBLE_MOBS).get().contains(target)
                && target.getDistanceSq(entity) < (double) (this.distance * this.distance);
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        BrainUtil.brain(entity).setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(target(entity), true));
        entity.getMoveHelper().strafe(-this.speed, 0.0F);
    }

    private static EntityLivingBase target(EntityLivingBase entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
