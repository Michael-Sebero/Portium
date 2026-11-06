package com.michaelsebero.portium.entity.merchant;

import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;

/** 1.14's {@code LookAtCustomerGoal}: a trading merchant looks at its customer. */
public class EntityAILookAtCustomer116 extends EntityAIWatchClosest {

    private final EntityAbstractVillager116 villager;

    public EntityAILookAtCustomer116(EntityAbstractVillager116 villager) {
        super(villager, EntityPlayer.class, 8.0F);
        this.villager = villager;
    }

    @Override
    public boolean shouldExecute() {
        if (!this.villager.hasCustomer()) return false;
        this.closestEntity = this.villager.getCustomer();
        return true;
    }
}
