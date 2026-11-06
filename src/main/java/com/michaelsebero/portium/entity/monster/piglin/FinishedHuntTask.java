package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import net.minecraft.world.WorldServer;

import com.michaelsebero.portium.entity.monster.EntityHoglin;
import net.minecraft.entity.EntityLivingBase;

/** Its hoglin target died: no more hunting for a while. */
public class FinishedHuntTask<E extends EntityPiglin> extends Task<E> {

    public FinishedHuntTask() {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.HUNTED_RECENTLY, MemoryModuleStatus.REGISTERED));
    }

    @Override
    protected void startExecuting(WorldServer world, E piglin, long gameTime) {
        EntityLivingBase target = piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get();
        if (target instanceof EntityHoglin && target.getHealth() <= 0.0F) PiglinTasks.dontKillAnyMoreHoglinsForAWhile(piglin);
    }
}
