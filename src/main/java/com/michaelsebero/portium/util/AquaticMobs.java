package com.michaelsebero.portium.util;

import com.michaelsebero.portium.entity.EntityWaterCreature;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import com.michaelsebero.portium.init.PortiumEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.item.ItemStack;

/**
 * 1.13 introduced an "aquatic" creature type that Impaling deals extra damage to and pufferfish leave alone. 1.12 has
 * no such type; it covers squid (and modded water mobs), guardians, Portium's fish and dolphins, and turtles.
 */
public final class AquaticMobs {

    public static boolean isAquatic(Entity entity) {
        return entity instanceof EntityWaterMob || entity instanceof EntityGuardian || entity instanceof EntityWaterCreature
                || entity instanceof EntityTurtle;
    }

    public static float impalingBonus(ItemStack weapon, Entity target) {
        if (!isAquatic(target)) return 0.0F;
        return EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.IMPALING, weapon) * 2.5F;
    }

    private AquaticMobs() {}
}
