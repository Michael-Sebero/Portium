package com.michaelsebero.portium.entity.ai.brain.task;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.entity.ICrossbowUser;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.item.ItemCrossbow;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.util.EnumHand;
import net.minecraft.world.WorldServer;

/** Loads, holds and fires a crossbow at the attack target while it's seen and in range. */
public class ShootTargetTask<E extends EntityLiving & ICrossbowUser & IRangedAttackMob> extends Task<E> {

    private int holdTime;
    private State state = State.UNCHARGED;

    public ShootTargetTask() {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryModuleStatus.VALUE_PRESENT), 1200);
    }

    @Override
    protected boolean shouldExecute(WorldServer world, E entity) {
        EntityLivingBase target = target(entity);
        return holdsCrossbow(entity) && BrainUtil.isMobVisible(entity, target) && BrainUtil.canFireAtTarget(entity, target, 0);
    }

    @Override
    protected boolean shouldContinueExecuting(WorldServer world, E entity, long gameTime) {
        return BrainUtil.brain(entity).hasMemory(MemoryModuleType.ATTACK_TARGET) && this.shouldExecute(world, entity);
    }

    @Override
    protected void updateTask(WorldServer world, E entity, long gameTime) {
        EntityLivingBase target = target(entity);
        BrainUtil.brain(entity).setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(target, true));
        this.attack(entity, target);
    }

    @Override
    protected void resetTask(WorldServer world, E entity, long gameTime) {
        if (entity.isHandActive()) entity.resetActiveHand();
        if (holdsCrossbow(entity)) {
            entity.setCharging(false);
            ItemCrossbow.setCharged(entity.getActiveItemStack(), false);
        }
    }

    private void attack(E entity, EntityLivingBase target) {
        if (this.state == State.UNCHARGED) {
            entity.setActiveHand(crossbowHand(entity));
            this.state = State.CHARGING;
            entity.setCharging(true);
        } else if (this.state == State.CHARGING) {
            if (!entity.isHandActive()) this.state = State.UNCHARGED;
            if (entity.getItemInUseMaxCount() >= ItemCrossbow.getChargeTime(entity.getActiveItemStack())) {
                entity.stopActiveHand();
                this.state = State.CHARGED;
                this.holdTime = 20 + entity.getRNG().nextInt(20);
                entity.setCharging(false);
            }
        } else if (this.state == State.CHARGED) {
            if (--this.holdTime == 0) this.state = State.READY_TO_ATTACK;
        } else if (this.state == State.READY_TO_ATTACK) {
            entity.attackEntityWithRangedAttack(target, 1.0F);
            ItemCrossbow.setCharged(entity.getHeldItem(crossbowHand(entity)), false);
            this.state = State.UNCHARGED;
        }
    }

    private static boolean holdsCrossbow(EntityLiving entity) {
        return BrainUtil.isCrossbow(entity.getHeldItemMainhand()) || BrainUtil.isCrossbow(entity.getHeldItemOffhand());
    }

    private static EnumHand crossbowHand(EntityLiving entity) {
        return BrainUtil.isCrossbow(entity.getHeldItemMainhand()) ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
    }

    private static EntityLivingBase target(EntityLivingBase entity) {
        return BrainUtil.brain(entity).getMemory(MemoryModuleType.ATTACK_TARGET).get();
    }

    private enum State {
        UNCHARGED,
        CHARGING,
        CHARGED,
        READY_TO_ATTACK
    }
}
