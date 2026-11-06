package com.michaelsebero.portium.entity.merchant;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * 1.14's trader llama: led by a wandering trader, it spits at whatever hurts the trader, can't be ridden while led by
 * it, and leaves with it (or on its own after two days) unless tamed, ridden or leashed by someone else.
 */
public class EntityTraderLlama extends EntityLlama {

    private int despawnDelay = 47999;

    public EntityTraderLlama(World world) {
        super(world);
    }

    /** 1.12's llama breeding, but the young is a trader llama (1.14's {@code createChild()} override). */
    @Override
    public EntityLlama createChild(EntityAgeable ageable) {
        EntityLlama child = new EntityTraderLlama(this.world);
        this.setOffspringAttributes(ageable, child);
        EntityLlama other = (EntityLlama) ageable;
        int strength = this.rand.nextInt(Math.max(this.getStrength(), other.getStrength())) + 1;
        if (this.rand.nextFloat() < 0.03F) ++strength;
        ((com.michaelsebero.portium.mixin.accessor.EntityLlamaAccessor) child).portium$setStrength(strength);
        child.setVariant(this.rand.nextBoolean() ? this.getVariant() : other.getVariant());
        return child;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("DespawnDelay", this.despawnDelay);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (tag.hasKey("DespawnDelay", 99)) this.despawnDelay = tag.getInteger("DespawnDelay");
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.addTask(1, new EntityAIPanic(this, 2.0D));
        this.targetTasks.addTask(1, new FollowTraderGoal(this));
    }

    @Override
    protected void mountTo(EntityPlayer player) {
        if (!(this.getLeashHolder() instanceof EntityWanderingTrader)) super.mountTo(player);
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (!this.world.isRemote) this.tryDespawn();
    }

    private void tryDespawn() {
        if (!this.canLeave()) return;
        this.despawnDelay = this.isLeashedToTrader() ? ((EntityWanderingTrader) this.getLeashHolder()).getDespawnDelay() - 1 : this.despawnDelay - 1;
        if (this.despawnDelay <= 0) {
            this.clearLeashed(true, false);
            this.setDead();
        }
    }

    private boolean canLeave() {
        return !this.isTame() && !this.isLeashedToStranger() && !this.isBeingRidden();
    }

    private boolean isLeashedToTrader() {
        return this.getLeashHolder() instanceof EntityWanderingTrader;
    }

    private boolean isLeashedToStranger() {
        return this.getLeashed() && !this.isLeashedToTrader();
    }

    /** Wandering traders spawn theirs grown (1.14 sets the age for the EVENT spawn reason). */
    public void makeAdult() {
        this.setGrowingAge(0);
    }

    /** 1.14's {@code FollowTraderGoal}: attacks what last hurt the trader leading it. */
    static class FollowTraderGoal extends EntityAITarget {

        private final EntityLlama llama;
        private EntityLivingBase attacker;
        private int timestamp;

        FollowTraderGoal(EntityLlama llama) {
            super(llama, false);
            this.llama = llama;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.llama.getLeashed()) return false;
            Entity holder = this.llama.getLeashHolder();
            if (!(holder instanceof EntityWanderingTrader)) return false;
            EntityWanderingTrader trader = (EntityWanderingTrader) holder;
            this.attacker = trader.getRevengeTarget();
            return trader.getRevengeTimer() != this.timestamp && this.isSuitableTarget(this.attacker, false);
        }

        @Override
        public void startExecuting() {
            this.taskOwner.setAttackTarget(this.attacker);
            Entity holder = this.llama.getLeashHolder();
            if (holder instanceof EntityWanderingTrader) this.timestamp = ((EntityWanderingTrader) holder).getRevengeTimer();
            super.startExecuting();
        }
    }
}
