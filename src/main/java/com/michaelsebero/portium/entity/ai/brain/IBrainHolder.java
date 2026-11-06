package com.michaelsebero.portium.entity.ai.brain;

import net.minecraft.item.ItemStack;

/** An entity run by a 1.14 brain. */
public interface IBrainHolder {

    Brain<?> getBrain();

    /** 1.16's {@code MobEntity.wantsToPickUp}, for the wanted-items sensor. */
    default boolean wantsToPickUp(ItemStack stack) {
        return false;
    }
}
