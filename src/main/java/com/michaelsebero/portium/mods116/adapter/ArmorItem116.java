package com.michaelsebero.portium.mods116.adapter;

import com.google.common.collect.Multimap;
import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.adapter.IItemPropertyHolder;
import com.michaelsebero.portium.modern.mc.item.Item$Properties;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a 1.16 mod's armor extends, and what {@code new ArmorItem(...)} in 1.16 code makes: 1.12's armor, so 1.12 lets
 * it be worn (by right click, in the armor slots, by dispensers and commands), draws it with 1.12's armor layer and
 * counts its protection. 1.16 describes armor with an {@code IArmorMaterial}, a class of Minecraft 1.16.5's own here,
 * taken as an object; Portium makes a 1.12 material of the same values for it (1.12's own materials, which 1.16's
 * vanilla ones are here, are used as they are). 1.16's knockback resistance becomes an attribute, as in 1.16.
 */
public class ArmorItem116 extends ItemArmor {

    private static final Map<Object, ArmorMaterial> MATERIALS = new ConcurrentHashMap<>();

    /** 1.16's material: a mod's (or 1.12's, for vanilla's). */
    @Srg("field_200882_e")
    protected final Object material;
    @Srg("field_234655_c_")
    protected final float knockbackResistance;

    public ArmorItem116(Object material, EntityEquipmentSlot slot, Item$Properties properties) {
        super(legacyMaterial(material), 0, slot);
        this.material = material;
        this.knockbackResistance = material instanceof ArmorMaterial ? 0.0F : number(material, "func_230304_f_").floatValue();
        ((IItemPropertyHolder) this).portium$applyItemProperties(properties);
        // 1.16 gives armor its material's durability unless its properties set one, and a damageable item stacks to one.
        if (properties.getMaxDamage() <= 0) setMaxDamage(durability(material, slot));
        this.maxStackSize = 1;
    }

    /** 1.16's: the material. */
    @Srg("func_200880_d")
    public Object getArmorMaterial116() {
        return this.material;
    }

    @Srg("func_200881_e")
    public int getDamageReduceAmount() {
        return this.damageReduceAmount;
    }

    @Srg("func_234657_f_")
    public float getToughness() {
        return this.toughness;
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        if (this.material instanceof ArmorMaterial) return super.getIsRepairable(toRepair, repair);
        Object ingredient = call(this.material, "func_200898_c");
        return ingredient instanceof Ingredient && ((Ingredient) ingredient).apply(repair) || super.getIsRepairable(toRepair, repair);
    }

    @Override
    public int getItemEnchantability() {
        return this.material instanceof ArmorMaterial ? super.getItemEnchantability() : number(this.material, "func_200900_a").intValue();
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);
        if (slot == this.armorType && this.knockbackResistance > 0) {
            UUID[] ids = ObfuscationReflectionHelper.getPrivateValue(ItemArmor.class, null, "field_185084_n");
            modifiers.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                    new AttributeModifier(ids[slot.getIndex()], "Armor knockback resistance", this.knockbackResistance, 0));
        }
        return modifiers;
    }

    /** 1.16's key, from the registry name, as Portium's other 1.16 items have. */
    @Override
    public String getTranslationKey() {
        return com.michaelsebero.portium.modern.adapter.ModernItems.translationKey(this, super.getTranslationKey());
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getTranslationKey();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal(getTranslationKey(stack)).trim();
    }

    // 1.16's materials as 1.12's

    private static ArmorMaterial legacyMaterial(Object material) {
        if (material instanceof ArmorMaterial) return (ArmorMaterial) material;
        return MATERIALS.computeIfAbsent(material, ArmorItem116::makeMaterial);
    }

    private static ArmorMaterial makeMaterial(Object material) {
        int[] reductions = new int[4];
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            if (slot.getSlotType() == EntityEquipmentSlot.Type.ARMOR) reductions[slot.getIndex()] = number(material, "func_200902_b", slot).intValue();
        }
        String name = name(material);
        Object sound = call(material, "func_200899_b");
        // Durability is per item (see the constructor); 1.12's factor is left at none.
        ArmorMaterial made = EnumHelper.addArmorMaterial("PORTIUM_" + name.toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z0-9_]", "_"), name, 0, reductions,
                number(material, "func_200900_a").intValue(), sound instanceof SoundEvent ? (SoundEvent) sound : null, number(material, "func_200901_e").floatValue());
        if (made == null) throw new IllegalStateException("Couldn't make a 1.12 armor material for " + name);
        return made;
    }

    private static int durability(Object material, EntityEquipmentSlot slot) {
        return material instanceof ArmorMaterial ? ((ArmorMaterial) material).getDurability(slot) : number(material, "func_200896_a", slot).intValue();
    }

    /** The material's name (its texture's), which 1.16 gives only on clients: the enum constant's on servers. */
    private static String name(Object material) {
        Object name = null;
        try {
            name = call(material, "func_200897_d");
        } catch (IllegalStateException e) {
            // A server's copy of the material lacks it.
        }
        if (name == null) name = material instanceof Enum ? ((Enum<?>) material).name().toLowerCase(java.util.Locale.ROOT) : material.toString();
        return name.toString();
    }

    private static Number number(Object material, String srg, Object... arguments) {
        Object value = call(material, srg, arguments);
        return value instanceof Number ? (Number) value : 0;
    }

    @Nullable
    private static Object call(Object material, String srg, Object... arguments) {
        for (Method method : material.getClass().getMethods()) {
            if (!method.getName().equals(srg) || method.getParameterCount() != arguments.length) continue;
            try {
                method.setAccessible(true);
                return method.invoke(material, arguments);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't read " + srg + " of armor material " + material, e);
            }
        }
        throw new IllegalStateException("Armor material " + material + " has no " + srg);
    }
}
