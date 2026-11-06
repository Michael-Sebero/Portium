package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** Walks to the attack target, or stops walking once it can hit (or shoot) it. */
public class MoveToTargetTask extends Task<EntityLiving> {

    private final float speed;

    public MoveToTargetTask(float speed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleStatus.REGISTERED));
        this.speed = speed;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLiving entity, long gameTime) {
        EntityLivingBase target = BrainUtil.brain(entity).getMemory(MemoryModuleType.ATTACK_TARGET).get();
        Brain<?> brain = BrainUtil.brain(entity);
        if (BrainUtil.isMobVisible(entity, target) && BrainUtil.canFireAtTarget(entity, target, 1)) {
            brain.removeMemory(MemoryModuleType.WALK_TARGET);
        } else {
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(target, true));
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityPosWrapper(target, false), this.speed, 0));
        }
    }
}
