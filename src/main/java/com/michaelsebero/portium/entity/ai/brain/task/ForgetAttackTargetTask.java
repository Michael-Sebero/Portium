package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Picks an attack target when it has none (1.16's {@code ForgetAttackTargetTask}, despite the name: the one that
 * starts fights).
 */
public class ForgetAttackTargetTask<E extends EntityLiving> extends Task<E> {

    private final Predicate<E> canAttack;
    private final Function<E, Optional<? extends EntityLivingBase>> targetFinder;

    public ForgetAttackTargetTask(Predicate<E> canAttack, Function<E, Optional<? extends EntityLivingBase>> targetFinder) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleStatus.REGISTERED));
        this.canAttack = canAttack;
        this.targetFinder = targetFinder;
    }

    public ForgetAttackTargetTask(Function<E, Optional<? extends EntityLivingBase>> targetFinder) {
        this(entity -> true, targetFinder);
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        if (!this.canAttack.test(entity)) return false;
        Optional<? extends EntityLivingBase> target = this.targetFinder.apply(entity);
        return target.isPresent() && target.get().isEntityAlive();
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        this.targetFinder.apply(entity).ifPresent(target -> {
            BrainUtil.brain(entity).setMemory(MemoryModuleType.ATTACK_TARGET, (EntityLivingBase) target);
            BrainUtil.brain(entity).removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        });
    }
}
