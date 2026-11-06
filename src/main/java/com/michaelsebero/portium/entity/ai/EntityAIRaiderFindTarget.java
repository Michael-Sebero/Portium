package com.michaelsebero.portium.entity.ai;

import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.Raiders116;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 1.14's {@code AbstractRaiderEntity.FindTargetGoal}: a patrolling raider that spots a target stops and stares until
 * it comes within range, then it and every raider within 8 blocks turn on it together.
 */
public class EntityAIRaiderFindTarget extends EntityAIBase {

    private final EntityLiving raider;
    private final float rangeSq;

    public EntityAIRaiderFindTarget(EntityLiving raider, float range) {
        this.raider = raider;
        this.rangeSq = range * range;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase revenge = this.raider.getRevengeTarget();
        return !Raiders116.hasRaid(this.raider) && IRaider.state(this.raider).isPatrolling() && this.raider.getAttackTarget() != null
                && !((IRaider) this.raider).portium$isAggressive() && !(revenge instanceof EntityPlayer);
    }

    @Override
    public void startExecuting() {
        this.raider.getNavigator().clearPath();
        for (EntityLiving other : Raiders116.raidersNear(this.raider, this.raider.getEntityBoundingBox().grow(8.0D), 8.0D)) {
            other.setAttackTarget(this.raider.getAttackTarget());
        }
    }

    @Override
    public void resetTask() {
        EntityLivingBase target = this.raider.getAttackTarget();
        if (target == null) return;
        for (EntityLiving other : Raiders116.raidersNear(this.raider, this.raider.getEntityBoundingBox().grow(8.0D), 8.0D)) {
            other.setAttackTarget(target);
            ((IRaider) other).portium$setAggressive(true);
        }
        ((IRaider) this.raider).portium$setAggressive(true);
    }

    @Override
    public void updateTask() {
        EntityLivingBase target = this.raider.getAttackTarget();
        if (target == null) return;
        if (this.raider.getDistanceSq(target) > this.rangeSq) {
            this.raider.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (this.raider.getRNG().nextInt(50) == 0) this.raider.playLivingSound();
        } else {
            ((IRaider) this.raider).portium$setAggressive(true);
        }
    }
}
