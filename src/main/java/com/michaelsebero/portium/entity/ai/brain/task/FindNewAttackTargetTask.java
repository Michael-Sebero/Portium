package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.WorldServer;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Drops the attack target when it's been out of reach for ten seconds, died, left the world, can't be targeted, or
 * matches a give-up predicate (1.16's {@code FindNewAttackTargetTask}, despite the name).
 */
public class FindNewAttackTargetTask<E extends EntityLiving> extends Task<E> {

    private final Predicate<EntityLivingBase> giveUp;

    public FindNewAttackTargetTask(Predicate<EntityLivingBase> giveUp) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleStatus.REGISTERED));
        this.giveUp = giveUp;
    }

    public FindNewAttackTargetTask() {
        this(target -> false);
    }

    @Override
    protected void startExecuting(WorldServer world, E entity, long gameTime) {
        EntityLivingBase target = BrainUtil.brain(entity).getMemory(MemoryModuleType.ATTACK_TARGET).get();
        if (cantReachForTooLong(entity) || !target.isEntityAlive() || target.world != entity.world
                || !TargetPredicate.canHostileTarget(target) || this.giveUp.test(target)) {
            BrainUtil.brain(entity).removeMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    private static boolean cantReachForTooLong(EntityLivingBase entity) {
        Optional<Long> since = BrainUtil.brain(entity).getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return since.isPresent() && entity.world.getTotalWorldTime() - since.get() > 200L;
    }
}
