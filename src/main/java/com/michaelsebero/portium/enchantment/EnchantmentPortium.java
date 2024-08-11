package com.michaelsebero.portium.enchantment;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.item.ItemTrident;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.common.util.EnumHelper;

import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/**
 * The four trident enchantments (1.13), with 1.13's levels, rarities and enchanting costs.
 */
public class EnchantmentPortium extends Enchantment {

    public static final EnumEnchantmentType TRIDENT = EnumHelper.addEnchantmentType("PORTIUM_TRIDENT", item -> item instanceof ItemTrident);

    private final int maxLevel;
    private final IntUnaryOperator minEnchantability;
    private final IntUnaryOperator maxEnchantability;
    private final Predicate<Enchantment> compatible;

    private EnchantmentPortium(String name, Rarity rarity, int maxLevel, IntUnaryOperator min, IntUnaryOperator max, Predicate<Enchantment> compatible) {
        super(rarity, TRIDENT, new EntityEquipmentSlot[] {EntityEquipmentSlot.MAINHAND});
        this.maxLevel = maxLevel;
        this.minEnchantability = min;
        this.maxEnchantability = max;
        this.compatible = compatible;
        this.setName(Tags.MOD_ID + "." + name);
    }

    /** Extra melee and thrown damage against aquatic mobs: 2.5 per level. */
    public static EnchantmentPortium impaling() {
        return new EnchantmentPortium("impaling", Rarity.RARE, 5, l -> 1 + (l - 1) * 8, l -> 1 + (l - 1) * 8 + 20, e -> true);
    }

    /** Thrown tridents fly back to the thrower. */
    public static EnchantmentPortium loyalty() {
        return new EnchantmentPortium("loyalty", Rarity.UNCOMMON, 3, l -> 5 + l * 7, l -> 50, e -> !is(e, "riptide"));
    }

    /** Throwing launches the player instead, only while wet. */
    public static EnchantmentPortium riptide() {
        return new EnchantmentPortium("riptide", Rarity.RARE, 3, l -> 10 + l * 7, l -> 50, e -> !is(e, "loyalty") && !is(e, "channeling"));
    }

    /** Thrown tridents summon lightning on the struck mob during thunderstorms. */
    public static EnchantmentPortium channeling() {
        return new EnchantmentPortium("channeling", Rarity.VERY_RARE, 1, l -> 25, l -> 50, e -> !is(e, "riptide"));
    }

    private static boolean is(Enchantment enchantment, String name) {
        return enchantment.getRegistryName() != null && enchantment.getRegistryName().toString().equals(Tags.MOD_ID + ":" + name);
    }

    @Override
    public int getMaxLevel() {
        return this.maxLevel;
    }

    @Override
    public int getMinEnchantability(int level) {
        return this.minEnchantability.applyAsInt(level);
    }

    @Override
    public int getMaxEnchantability(int level) {
        return this.maxEnchantability.applyAsInt(level);
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        return super.canApplyTogether(other) && this.compatible.test(other);
    }
}
