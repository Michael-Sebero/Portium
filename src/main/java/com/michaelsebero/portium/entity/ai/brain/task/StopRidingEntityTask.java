package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.function.BiPredicate;

/** Gets off (or gives up on) a mount that's dead, far, or matches a predicate. */
public class StopRidingEntityTask<E extends EntityLivingBase> extends Task<E> {

    private final int maxDistance;
    private final BiPredicate<E, Entity> dismountPredicate;

    public StopRidingEntityTask(int maxDistance, BiPredicate<E, Entity> dismountPredicate) {
        super(ImmutableMap.of(MemoryModuleType.RIDE_TARGET, MemoryModuleStatus.REGISTERED));
        this.maxDistance = maxDistance;
        this.dismountPredicate = dismountPredicate;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        Entity riding = entity.getRidingEntity();
        Entity target = BrainUtil.brain(entity).getMemory(MemoryModuleType.RIDE_TARGET).orElse(null);
        if (riding == null && target == null) return false;
        Entity mount = riding == null ? target : riding;
        return !this.isValid(entity, mount) || this.dismountPredicate.test(entity, mount);
    }

    private boolean isValid(E entity, Entity mount) {
        return mount.isEntityAlive() && mount.getDistanceSq(entity) < (double) (this.maxDistance * this.maxDistance) && mount.world == entity.world;
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        entity.dismountRidingEntity();
        BrainUtil.brain(entity).removeMemory(MemoryModuleType.RIDE_TARGET);
    }
}
