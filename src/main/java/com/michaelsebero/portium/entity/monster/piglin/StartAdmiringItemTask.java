package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import net.minecraft.world.WorldServer;

import net.minecraft.init.Items;

/** Done admiring what's in its off hand (the memory ran out): barter or keep it. */
public class StartAdmiringItemTask<E extends EntityPiglin> extends Task<E> {

    public StartAdmiringItemTask() {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryModuleStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E piglin) {
        return !piglin.getHeldItemOffhand().isEmpty() && piglin.getHeldItemOffhand().getItem() != Items.SHIELD;
    }

    @Override
    protected void startExecuting(WorldServer world, E piglin, long gameTime) {
        PiglinTasks.stopHoldingOffHandItem(piglin, true);
    }
}
