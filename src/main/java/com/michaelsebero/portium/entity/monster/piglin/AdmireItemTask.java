package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import net.minecraft.world.WorldServer;

import net.minecraft.entity.item.EntityItem;

/** Sees a loved item lying about: starts admiring (walking to it) for a while. */
public class AdmireItemTask<E extends EntityPiglin> extends Task<E> {

    private final int duration;

    public AdmireItemTask(int duration) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.ADMIRING_ITEM,
                MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.ADMIRING_DISABLED, MemoryModuleStatus.VALUE_ABSENT,
                MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM, MemoryModuleStatus.VALUE_ABSENT));
        this.duration = duration;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E piglin) {
        EntityItem item = piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).get();
        return PiglinTasks.isLovedItem(item.getItem());
    }

    @Override
    protected void startExecuting(WorldServer world, E piglin, long gameTime) {
        piglin.getBrain().replaceMemory(MemoryModuleType.ADMIRING_ITEM, true, this.duration);
    }
}
