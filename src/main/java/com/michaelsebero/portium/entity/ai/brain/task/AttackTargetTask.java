package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;

/** Hits the attack target when it's seen and within reach, then cools down. */
public class AttackTargetTask extends Task<EntityLiving> {

    private final int cooldown;

    public AttackTargetTask(int cooldown) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET,
                MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleStatus.VALUE_ABSENT));
        this.cooldown = cooldown;
    }

    @Override
    protected boolean shouldExecute(WorldServer world, EntityLiving entity) {
        EntityLivingBase target = getAttackTarget(entity);
        return !isRanged(entity) && BrainUtil.isMobVisible(entity, target) && BrainUtil.canAttackTarget(entity, target);
    }

    /** Holding a crossbow it can fire (brain mobs only fire crossbows). */
    private static boolean isRanged(EntityLiving entity) {
        return BrainUtil.isCrossbow(entity.getHeldItemMainhand()) || BrainUtil.isCrossbow(entity.getHeldItemOffhand());
    }

    @Override
    protected void startExecuting(WorldServer world, EntityLiving entity, long gameTime) {
        EntityLivingBase target = getAttackTarget(entity);
        BrainUtil.lookAt(entity, target);
        entity.swingArm(EnumHand.MAIN_HAND);
        entity.attackEntityAsMob(target);
        BrainUtil.brain(entity).replaceMemory(MemoryModuleType.ATTACK_COOLING_DOWN, true, this.cooldown);
    }

    private static EntityLivingBase getAttackTarget(EntityLiving entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }
}
