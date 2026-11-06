package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.mixin.accessor.EntityLivingAccessor;
import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.EntityEquipmentSlot;

/** A mob's equipment drop chance for a slot (1.12 keeps them private). */
public final class DropChances {

    public static float get(EntityLiving mob, EntityEquipmentSlot slot) {
        EntityLivingAccessor accessor = (EntityLivingAccessor) mob;
        float[] chances = slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? accessor.portium$getHandDropChances() : accessor.portium$getArmorDropChances();
        return chances[slot.getIndex()];
    }

    private DropChances() {}
}
