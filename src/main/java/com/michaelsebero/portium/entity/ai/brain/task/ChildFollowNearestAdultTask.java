package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.RangedInteger;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.world.WorldServer;

/** A baby keeps within a distance range of the nearest adult it sees. */
public class ChildFollowNearestAdultTask<E extends EntityAgeable> extends Task<E> {

    private final RangedInteger distance;
    private final float speed;

    public ChildFollowNearestAdultTask(RangedInteger distance, float speed) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT));
        this.distance = distance;
        this.speed = speed;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        if (!entity.isChild()) return false;
        EntityAgeable adult = adult(entity);
        double distanceSq = entity.getDistanceSq(adult);
        int max = this.distance.getMax() + 1;
        int min = this.distance.getMinInclusive();
        return distanceSq < (double) (max * max) && distanceSq >= (double) (min * min);
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        BrainUtil.setTargetEntity(entity, adult(entity), this.speed, this.distance.getMinInclusive() - 1);
    }

    private static EntityAgeable adult(EntityAgeable entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT).get();
    }
}
