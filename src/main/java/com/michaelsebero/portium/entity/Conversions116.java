package com.michaelsebero.portium.entity;

import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * 1.16's {@code MobEntity.func_233656_b_} (convertTo): replaces a mob by another of a different kind in place, keeping
 * its name, AI switch, persistence, age, invulnerability and (if asked) its equipment with drop chances.
 */
public final class Conversions116 {

    @Nullable
    public static <T extends EntityLiving> T convertTo(EntityLiving from, Function<net.minecraft.world.World, T> factory, boolean keepEquipment) {
        if (from.isDead) return null;
        T to = factory.apply(from.world);
        to.copyLocationAndAnglesFrom(from);
        boolean child = from.isChild();
        if (to instanceof EntityZombie) {
            ((EntityZombie) to).setChild(child);
        } else if (to instanceof EntityAgeable && child) {
            ((EntityAgeable) to).setGrowingAge(-24000);
        } else if (to instanceof IChildSettable) {
            ((IChildSettable) to).setChild116(child);
        }
        to.setNoAI(from.isAIDisabled());
        if (from.hasCustomName()) {
            to.setCustomNameTag(from.getCustomNameTag());
            to.setAlwaysRenderNameTag(from.getAlwaysRenderNameTag());
        }
        if (from.isNoDespawnRequired()) to.enablePersistence();
        to.setEntityInvulnerable(from.getIsInvulnerable());
        if (keepEquipment) {
            to.setCanPickUpLoot(from.canPickUpLoot());
            for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
                ItemStack stack = from.getItemStackFromSlot(slot);
                if (!stack.isEmpty()) {
                    to.setItemStackToSlot(slot, stack.copy());
                    to.setDropChance(slot, DropChances.get(from, slot));
                    stack.setCount(0);
                }
            }
        }
        from.world.spawnEntity(to);
        if (from.isRiding()) {
            net.minecraft.entity.Entity mount = from.getRidingEntity();
            from.dismountRidingEntity();
            to.startRiding(mount, true);
        }
        from.setDead();
        return to;
    }

    /** A mob whose baby flag isn't 1.12's ageing (piglins). */
    public interface IChildSettable {
        void setChild116(boolean child);
    }

    private Conversions116() {}
}
