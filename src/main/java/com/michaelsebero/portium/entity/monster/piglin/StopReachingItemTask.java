package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import net.minecraft.world.WorldServer;

import com.michaelsebero.portium.entity.ai.brain.Brain;

import java.util.Optional;

/** Gives up on an admired item it hasn't reached in ten seconds, and won't walk to one for a while. */
public class StopReachingItemTask<E extends EntityPiglin> extends Task<E> {

    private final int maxTime;
    private final int disableTime;

    public StopReachingItemTask(int maxTime, int disableTime) {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
                MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, MemoryModuleStatus.REGISTERED,
                MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, MemoryModuleStatus.REGISTERED));
        this.maxTime = maxTime;
        this.disableTime = disableTime;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E piglin) {
        return piglin.getHeldItemOffhand().isEmpty();
    }

    @Override
    protected void startExecuting(WorldServer world, E piglin, long gameTime) {
        Brain<EntityPiglin> brain = piglin.getBrain();
        Optional<Integer> time = brain.getMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
        if (!time.isPresent()) {
            brain.setMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, 0);
        } else if (time.get() > this.maxTime) {
            brain.removeMemory(MemoryModuleType.ADMIRING_ITEM);
            brain.removeMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            brain.replaceMemory(MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, true, this.disableTime);
        } else {
            brain.setMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM, time.get() + 1);
        }
    }
}
