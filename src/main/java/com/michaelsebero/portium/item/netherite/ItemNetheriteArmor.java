package com.michaelsebero.portium.item.netherite;

import com.google.common.collect.Multimap;
import com.michaelsebero.portium.item.IFireproofItem;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.UUID;

/**
 * Netherite armour (1.16): diamond's protection and toughness 3, and 0.1 knockback resistance per piece (all four
 * resist 40% of knockback). Survives fire and lava.
 */
public class ItemNetheriteArmor extends ItemArmor implements IFireproofItem {

    private static final UUID[] SLOT_IDS = ObfuscationReflectionHelper.getPrivateValue(ItemArmor.class, null, "field_185084_n");

    public ItemNetheriteArmor(EntityEquipmentSlot slot) {
        super(NetheriteMaterials.ARMOR, 0, slot);
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);
        if (slot == this.armorType) {
            modifiers.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                    new AttributeModifier(SLOT_IDS[slot.getIndex()], "Armor knockback resistance", 0.1D, 0));
        }
        return modifiers;
    }
}
