package com.michaelsebero.portium.item.netherite;

import com.google.common.collect.Multimap;
import com.michaelsebero.portium.item.IFireproofItem;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemSword;

/**
 * Netherite's tools (1.16), on 1.12's tool classes with netherite's material, which give 1.16's numbers: sword 8
 * damage at 1.6 attacks a second, pickaxe 6 at 1.2, axe 10 at 1.0, shovel 6.5 at 1.0. The hoe is 1.16's (1 damage at
 * 4 a second), where 1.12's hoe would add its material's damage to its speed. All survive fire and lava.
 */
public final class NetheriteTools {

    public static final class Sword extends ItemSword implements IFireproofItem {

        public Sword() {
            super(NetheriteMaterials.TOOLS);
        }
    }

    public static final class Pickaxe extends ItemPickaxe implements IFireproofItem {

        public Pickaxe() {
            super(NetheriteMaterials.TOOLS);
        }
    }

    public static final class Axe extends ItemAxe implements IFireproofItem {

        public Axe() {
            super(NetheriteMaterials.TOOLS, 5.0F, -3.0F);
        }
    }

    public static final class Shovel extends ItemSpade implements IFireproofItem {

        public Shovel() {
            super(NetheriteMaterials.TOOLS);
        }
    }

    public static final class Hoe extends ItemHoe implements IFireproofItem {

        public Hoe() {
            super(NetheriteMaterials.TOOLS);
            this.setCreativeTab(CreativeTabs.TOOLS);
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);
            if (slot == EntityEquipmentSlot.MAINHAND) {
                modifiers.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());
                modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", 0.0D, 0));
            }
            return modifiers;
        }
    }

    private NetheriteTools() {}
}
