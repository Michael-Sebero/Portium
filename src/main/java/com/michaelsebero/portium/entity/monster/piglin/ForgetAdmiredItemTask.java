package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import net.minecraft.world.WorldServer;

import net.minecraft.entity.item.EntityItem;

import java.util.Optional;

/** Stops admiring when the item it was walking to is gone or out of reach. */
public class ForgetAdmiredItemTask<E extends EntityPiglin> extends Task<E> {

    private final int maxDistance;

    public ForgetAdmiredItemTask(int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.ADMIRING_ITEM, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleStatus.REGISTERED));
        this.maxDistance = maxDistance;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E piglin) {
        if (!piglin.getHeldItemOffhand().isEmpty()) return false;
        Optional<EntityItem> item = piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
        return !item.isPresent() || item.get().getDistanceSq(piglin) >= (double) (this.maxDistance * this.maxDistance);
    }

    @Override
    protected void startExecuting(WorldServer world, E piglin, long gameTime) {
        piglin.getBrain().removeMemory(MemoryModuleType.ADMIRING_ITEM);
    }
}
