package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

/** Calms down for a while when a memory is present (hoglins near a repellent). */
public class RandomlyStopAttackingTask extends Task<EntityLivingBase> {

    private final int duration;

    public RandomlyStopAttackingTask(MemoryModuleType<?> trigger, int duration) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.PACIFIED, MemoryModuleStatus.VALUE_ABSENT,
                trigger, MemoryModuleStatus.VALUE_PRESENT));
        this.duration = duration;
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
        BrainUtil.brain(entity).replaceMemory(MemoryModuleType.PACIFIED, true, this.duration);
        BrainUtil.brain(entity).removeMemory(MemoryModuleType.ATTACK_TARGET);
    }
}
