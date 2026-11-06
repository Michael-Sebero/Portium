package com.michaelsebero.portium.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;

/**
 * 1.16's Soul Speed: boots that run fast on soul sand and soul soil (and don't sink into soul sand), wearing a little
 * as they do. A treasure enchantment: only from piglin bartering and bastion chests.
 */
public class EnchantmentSoulSpeed extends Enchantment {

    public EnchantmentSoulSpeed() {
        super(Rarity.VERY_RARE, EnumEnchantmentType.ARMOR_FEET, new EntityEquipmentSlot[] {EntityEquipmentSlot.FEET});
        this.setName(com.michaelsebero.portium.Tags.MOD_ID + ".soul_speed");
    }

    @Override
    public int getMinEnchantability(int level) {
        return level * 10;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return this.getMinEnchantability(level) + 15;
    }

    @Override
    public boolean isTreasureEnchantment() {
        return true;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
