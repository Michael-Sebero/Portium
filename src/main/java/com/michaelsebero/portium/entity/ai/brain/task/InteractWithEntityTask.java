package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.function.Predicate;

/** Walks up to a seen entity of a kind, remembering it in a memory. */
public class InteractWithEntityTask<E extends EntityLivingBase, T extends EntityLivingBase> extends Task<E> {

    private final int targetDistance;
    private final float speed;
    private final Class<? extends T> type;
    private final int maxDistanceSq;
    private final Predicate<T> targetPredicate;
    private final Predicate<E> selfPredicate;
    private final MemoryModuleType<T> memory;

    public InteractWithEntityTask(Class<? extends T> type, int maxDistance, Predicate<E> selfPredicate, Predicate<T> targetPredicate,
                                  MemoryModuleType<T> memory, float speed, int targetDistance) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT,
                MemoryModuleType.VISIBLE_MOBS, MemoryModuleStatus.VALUE_PRESENT));
        this.type = type;
        this.speed = speed;
        this.maxDistanceSq = maxDistance * maxDistance;
        this.targetDistance = targetDistance;
        this.targetPredicate = targetPredicate;
        this.selfPredicate = selfPredicate;
        this.memory = memory;
    }

    public static <T extends EntityLivingBase> InteractWithEntityTask<EntityLivingBase, T> of(Class<? extends T> type, int maxDistance,
                                                                                               MemoryModuleType<T> memory, float speed, int targetDistance) {
        return new InteractWithEntityTask<>(type, maxDistance, self -> true, target -> true, memory, speed, targetDistance);
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        return this.selfPredicate.test(entity) && BrainUtil.brain(entity).getMemory(MemoryModuleType.VISIBLE_MOBS).get().stream().anyMatch(this::matches);
    }

    @SuppressWarnings("unchecked")
    private boolean matches(EntityLivingBase other) {
        return other.getClass() == this.type && this.targetPredicate.test((T) other);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        Brain<?> brain = BrainUtil.brain(entity);
        brain.getMemory(MemoryModuleType.VISIBLE_MOBS).ifPresent(mobs -> mobs.stream().filter(other -> other.getClass() == this.type)
                .filter(other -> other.getDistanceSq(entity) <= (double) this.maxDistanceSq).filter(other -> this.targetPredicate.test((T) other))
                .findFirst().ifPresent(other -> {
                    brain.setMemory(this.memory, (T) other);
                    brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(other, true));
                    brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityPosWrapper(other, false), this.speed, this.targetDistance));
                }));
    }
}
