package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

/** Picks a seen entity of a kind, close by, to interact with and look at. */
public class FindInteractionAndLookTargetTask extends Task<EntityLivingBase> {

    private final Class<? extends Entity> type;
    private final int maxDistanceSq;
    private final Predicate<EntityLivingBase> targetPredicate;
    private final Predicate<EntityLivingBase> selfPredicate;

    public FindInteractionAndLookTargetTask(Class<? extends Entity> type, int maxDistance, Predicate<EntityLivingBase> selfPredicate,
                                            Predicate<EntityLivingBase> targetPredicate) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryModuleStatus.VALUE_ABSENT,
                MemoryModuleType.VISIBLE_MOBS, MemoryModuleStatus.VALUE_PRESENT));
        this.type = type;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.targetPredicate = targetPredicate;
        this.selfPredicate = selfPredicate;
    }

    public FindInteractionAndLookTargetTask(Class<? extends Entity> type, int maxDistance) {
        this(type, maxDistance, self -> true, target -> true);
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityLivingBase entity) {
        return this.selfPredicate.test(entity) && BrainUtil.brain(entity).getMemory(MemoryModuleType.VISIBLE_MOBS).get().stream().anyMatch(this::matches);
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        brain.getMemory(MemoryModuleType.VISIBLE_MOBS).ifPresent(mobs -> mobs.stream()
                .filter(other -> other.getDistanceSq(entity) <= (double) this.maxDistanceSq).filter(this::matches).findFirst()
                .ifPresent(other -> {
                    brain.setMemory(MemoryModuleType.INTERACTION_TARGET, other);
                    brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(other, true));
                }));
    }

    private boolean matches(EntityLivingBase other) {
        return this.type.isInstance(other) && other.getClass() == this.type && this.targetPredicate.test(other);
    }
}
