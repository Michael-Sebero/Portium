package com.michaelsebero.portium.compat;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * Rewrites item NBT written for 1.13+ into 1.12's layout: durability moved into the stack's damage, enchantments keyed
 * by name rather than number, display names (and 1.14+ lore) stored as JSON text, and block ids in
 * {@code CanDestroy}/{@code CanPlaceOn} flattened.
 */
public final class LegacyItemNbt {

    /**
     * Rewrites a whole item stack tag ({@code {id:"minecraft:red_wool",Count:1b,tag:{...}}}) into 1.12's form,
     * leaving tags that are already 1.12-style alone.
     */
    public static NBTTagCompound convertStack(NBTTagCompound stack) {
        if (!stack.hasKey("id", 8)) return stack;
        LegacyItem item = LegacyItems.resolve(stack.getString("id"));
        if (item == null) return stack;
        ItemStack converted = item.createStack(Math.max(1, stack.getByte("Count")));
        if (stack.hasKey("Damage", 99)) converted.setItemDamage(stack.getShort("Damage"));
        apply(converted, stack.getCompoundTag("tag"));
        NBTTagCompound result = converted.writeToNBT(new NBTTagCompound());
        if (stack.hasKey("Slot")) result.setByte("Slot", stack.getByte("Slot"));
        return result;
    }

    /** Applies 1.13-style NBT to a stack. */
    public static void apply(ItemStack stack, @Nullable NBTTagCompound modern) {
        if (modern == null || modern.isEmpty()) return;
        NBTTagCompound tag = modern.copy();
        if (tag.hasKey("Damage", 99)) {
            if (stack.isItemStackDamageable()) stack.setItemDamage(tag.getInteger("Damage"));
            tag.removeTag("Damage");
        }
        convertEnchantments(tag, "Enchantments", "ench");
        convertEnchantments(tag, "StoredEnchantments", "StoredEnchantments");
        if (tag.hasKey("display", 10)) {
            NBTTagCompound display = tag.getCompoundTag("display");
            if (display.hasKey("Name", 8)) display.setString("Name", textToLegacy(display.getString("Name")));
            if (display.hasKey("Lore", 9)) {
                NBTTagList lore = display.getTagList("Lore", 8);
                NBTTagList converted = new NBTTagList();
                for (int i = 0; i < lore.tagCount(); i++) converted.appendTag(new NBTTagString(textToLegacy(lore.getStringTagAt(i))));
                display.setTag("Lore", converted);
            }
        }
        convertBlockList(tag, "CanDestroy");
        convertBlockList(tag, "CanPlaceOn");
        if (!tag.isEmpty()) {
            NBTTagCompound existing = stack.getTagCompound();
            if (existing == null) stack.setTagCompound(tag);
            else existing.merge(tag);
        }
    }

    private static void convertEnchantments(NBTTagCompound tag, String modernKey, String legacyKey) {
        if (!tag.hasKey(modernKey, 9)) return;
        NBTTagList modern = tag.getTagList(modernKey, 10);
        NBTTagList legacy = new NBTTagList();
        for (int i = 0; i < modern.tagCount(); i++) {
            NBTTagCompound entry = modern.getCompoundTagAt(i);
            Enchantment enchantment = entry.hasKey("id", 8) ? Enchantment.getEnchantmentByLocation(entry.getString("id")) : Enchantment.getEnchantmentByID(entry.getShort("id"));
            if (enchantment == null) continue;
            NBTTagCompound converted = new NBTTagCompound();
            converted.setShort("id", (short) Enchantment.getEnchantmentID(enchantment));
            converted.setShort("lvl", entry.getShort("lvl"));
            legacy.appendTag(converted);
        }
        tag.removeTag(modernKey);
        if (!legacy.isEmpty()) tag.setTag(legacyKey, legacy);
    }

    private static void convertBlockList(NBTTagCompound tag, String key) {
        if (!tag.hasKey(key, 9)) return;
        NBTTagList modern = tag.getTagList(key, 8);
        NBTTagList legacy = new NBTTagList();
        for (int i = 0; i < modern.tagCount(); i++) {
            String id = modern.getStringTagAt(i);
            if (id.startsWith("#")) continue;
            LegacyBlock block = LegacyBlockStates.resolve(id);
            if (block == null) continue;
            ResourceLocation name = Block.REGISTRY.getNameForObject(block.getBlock());
            legacy.appendTag(new NBTTagString(name.toString()));
        }
        tag.setTag(key, legacy);
    }

    /** JSON text becomes 1.12's section-sign formatted text; anything that isn't JSON is kept as it is. */
    public static String textToLegacy(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty() || "{[\"".indexOf(trimmed.charAt(0)) < 0) return text;
        try {
            ITextComponent component = ITextComponent.Serializer.fromJsonLenient(trimmed);
            return component == null ? text : component.getFormattedText();
        } catch (RuntimeException e) {
            return text;
        }
    }

    private LegacyItemNbt() {}
}
