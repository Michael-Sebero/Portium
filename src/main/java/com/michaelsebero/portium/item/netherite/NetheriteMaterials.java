package com.michaelsebero.portium.item.netherite;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;

/**
 * Netherite's tool and armour materials, with 1.16's values: tools harvest level 4, 2031 uses, speed 9, damage bonus 4,
 * enchantability 15; armour durability factor 37, protection 3/6/8/3, toughness 3, enchantability 15 (and 0.1
 * knockback resistance per piece, which 1.12's materials don't carry; see ItemNetheriteArmor). Both repaired with
 * netherite ingots.
 */
public final class NetheriteMaterials {

    public static final Item.ToolMaterial TOOLS = EnumHelper.addToolMaterial("PORTIUM_NETHERITE", 4, 2031, 9.0F, 4.0F, 15);
    public static final ItemArmor.ArmorMaterial ARMOR = EnumHelper.addArmorMaterial("PORTIUM_NETHERITE", Tags.MOD_ID + ":netherite", 37,
            new int[] {3, 6, 8, 3}, 15, PortiumSounds.ITEM_ARMOR_EQUIP_NETHERITE, 3.0F);

    /** Once the ingot exists. */
    public static void setRepairItem(Item ingot) {
        TOOLS.setRepairItem(new ItemStack(ingot));
        ARMOR.setRepairItem(new ItemStack(ingot));
    }

    private NetheriteMaterials() {}
}
