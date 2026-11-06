package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

/** Walks to the nearest wanted item within a distance. */
public class PickupWantedItemTask<E extends EntityLivingBase> extends Task<E> {

    private final Predicate<E> predicate;
    private final int maxDistance;
    private final float speed;

    public PickupWantedItemTask(float speed, boolean overrideWalkTarget, int maxDistance) {
        this(entity -> true, speed, overrideWalkTarget, maxDistance);
    }

    public PickupWantedItemTask(Predicate<E> predicate, float speed, boolean overrideWalkTarget, int maxDistance) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.WALK_TARGET,
                overrideWalkTarget ? MemoryModuleStatus.REGISTERED : MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
                MemoryModuleStatus.VALUE_PRESENT));
        this.predicate = predicate;
        this.maxDistance = maxDistance;
        this.speed = speed;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        return this.predicate.test(entity) && item(entity).getDistanceSq(entity) < (double) (this.maxDistance * this.maxDistance);
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        BrainUtil.setTargetEntity(entity, item(entity), this.speed, 0);
    }

    private static EntityItem item(EntityLivingBase entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).get();
    }
}
