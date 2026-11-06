package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import net.minecraft.world.WorldServer;

import com.michaelsebero.portium.entity.monster.EntityHoglin;

/** An adult that sees a huntable hoglin, when nobody nearby hunted lately, starts a hunt and rallies the others. */
public class StartHuntTask<E extends EntityPiglin> extends Task<E> {

    public StartHuntTask() {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.ANGRY_AT,
                MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.HUNTED_RECENTLY, MemoryModuleStatus.VALUE_ABSENT,
                MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleStatus.REGISTERED));
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E piglin) {
        return !piglin.isChild() && !PiglinTasks.hasAnyoneNearbyHuntedRecently(piglin);
    }

    @Override
    protected void startExecuting(WorldServer world, E piglin, long gameTime) {
        EntityHoglin hoglin = (EntityHoglin) piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN).get();
        PiglinTasks.setAngerTarget(piglin, hoglin);
        PiglinTasks.dontKillAnyMoreHoglinsForAWhile(piglin);
        PiglinTasks.broadcastAngerTarget(piglin, hoglin);
        PiglinTasks.broadcastDontKillAnyMoreHoglinsForAWhile(piglin);
    }
}
