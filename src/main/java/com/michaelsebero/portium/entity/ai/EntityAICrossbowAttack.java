package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.entity.ICrossbowUser;
import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.item.ItemCrossbow;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.EnumHand;

/**
 * 1.14's {@code RangedCrossbowAttackGoal}: close in until the target is within range and seen for a moment, load the
 * crossbow, hold it loaded for a second or two, then fire when the target is in sight.
 */
public class EntityAICrossbowAttack<T extends EntityMob & IRangedAttackMob & ICrossbowUser> extends EntityAIBase {

    private enum State { UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK }

    private final T shooter;
    private State state = State.UNCHARGED;
    private final double speed;
    private final float followDistanceSq;
    private int seeTime;
    private int holdTime;
    private int repathCooldown;

    public EntityAICrossbowAttack(T shooter, double speed, float followDistance) {
        this.shooter = shooter;
        this.speed = speed;
        this.followDistanceSq = followDistance * followDistance;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        return this.hasValidTarget() && this.isHoldingCrossbow();
    }

    private boolean isHoldingCrossbow() {
        return this.shooter.getHeldItemMainhand().getItem() instanceof ItemCrossbow || this.shooter.getHeldItemOffhand().getItem() instanceof ItemCrossbow;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.hasValidTarget() && (this.shouldExecute() || !this.shooter.getNavigator().noPath()) && this.isHoldingCrossbow();
    }

    private boolean hasValidTarget() {
        return this.shooter.getAttackTarget() != null && this.shooter.getAttackTarget().isEntityAlive();
    }

    @Override
    public void resetTask() {
        if (this.shooter instanceof IRaider) ((IRaider) this.shooter).portium$setAggressive(false);
        this.shooter.setAttackTarget(null);
        this.seeTime = 0;
        if (this.shooter.isHandActive()) {
            this.shooter.resetActiveHand();
            this.shooter.setCharging(false);
            ItemCrossbow.setCharged(this.shooter.getActiveItemStack(), false);
        }
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = this.shooter.getAttackTarget();
        if (target == null) return;
        boolean canSee = this.shooter.getEntitySenses().canSee(target);
        boolean sawBefore = this.seeTime > 0;
        if (canSee != sawBefore) this.seeTime = 0;
        if (canSee) ++this.seeTime;
        else --this.seeTime;
        double distanceSq = this.shooter.getDistanceSq(target);
        boolean approach = (distanceSq > this.followDistanceSq || this.seeTime < 5) && this.holdTime == 0;
        if (approach) {
            if (--this.repathCooldown <= 0) {
                this.shooter.getNavigator().tryMoveToEntityLiving(target, this.state == State.UNCHARGED ? this.speed : this.speed * 0.5D);
                this.repathCooldown = 20 + this.shooter.getRNG().nextInt(21);
            }
        } else {
            this.repathCooldown = 0;
            this.shooter.getNavigator().clearPath();
        }
        this.shooter.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        if (this.state == State.UNCHARGED) {
            if (!approach) {
                this.shooter.setActiveHand(this.crossbowHand());
                this.state = State.CHARGING;
                this.shooter.setCharging(true);
            }
        } else if (this.state == State.CHARGING) {
            if (!this.shooter.isHandActive()) this.state = State.UNCHARGED;
            if (this.shooter.getItemInUseMaxCount() >= ItemCrossbow.getChargeTime(this.shooter.getActiveItemStack())) {
                this.shooter.stopActiveHand();
                this.state = State.CHARGED;
                this.holdTime = 20 + this.shooter.getRNG().nextInt(20);
                this.shooter.setCharging(false);
            }
        } else if (this.state == State.CHARGED) {
            if (--this.holdTime == 0) this.state = State.READY_TO_ATTACK;
        } else if (this.state == State.READY_TO_ATTACK && canSee) {
            this.shooter.attackEntityWithRangedAttack(target, 1.0F);
            ItemCrossbow.setCharged(this.shooter.getHeldItem(this.crossbowHand()), false);
            this.state = State.UNCHARGED;
        }
    }

    private EnumHand crossbowHand() {
        return this.shooter.getHeldItemMainhand().getItem() instanceof ItemCrossbow ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
    }
}
