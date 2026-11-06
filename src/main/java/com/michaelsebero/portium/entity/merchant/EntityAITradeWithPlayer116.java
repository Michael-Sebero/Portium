package com.michaelsebero.portium.entity.merchant;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;

/** 1.14's {@code TradeWithPlayerGoal}: a merchant stands still while a nearby customer trades. */
public class EntityAITradeWithPlayer116 extends EntityAIBase {

    private final EntityAbstractVillager116 villager;

    public EntityAITradeWithPlayer116(EntityAbstractVillager116 villager) {
        this.villager = villager;
        this.setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.villager.isEntityAlive() || this.villager.isInWater() || !this.villager.onGround || this.villager.velocityChanged) return false;
        EntityPlayer customer = this.villager.getCustomer();
        return customer != null && this.villager.getDistanceSq(customer) <= 16.0D && customer.openContainer != null;
    }

    @Override
    public void startExecuting() {
        this.villager.getNavigator().clearPath();
    }

    @Override
    public void resetTask() {
        this.villager.setCustomer(null);
    }
}
