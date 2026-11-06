package com.michaelsebero.portium.entity.raid;

import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.BannerPattern;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

/**
 * 1.14's illager banner ({@code Raid.createIllagerBanner}): what patrol and raid leaders wear. Its name is stored as
 * 1.13+ JSON text, which {@code MixinItemStackJsonName} shows translated.
 */
public final class OminousBanner {

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Items.BANNER, 1, EnumDyeColor.WHITE.getDyeDamage());
        NBTTagList patterns = new NBTTagList();
        add(patterns, BannerPattern.RHOMBUS_MIDDLE, EnumDyeColor.CYAN);
        add(patterns, BannerPattern.STRIPE_BOTTOM, EnumDyeColor.SILVER);
        add(patterns, BannerPattern.STRIPE_CENTER, EnumDyeColor.GRAY);
        add(patterns, BannerPattern.BORDER, EnumDyeColor.SILVER);
        add(patterns, BannerPattern.STRIPE_MIDDLE, EnumDyeColor.BLACK);
        add(patterns, BannerPattern.HALF_HORIZONTAL, EnumDyeColor.SILVER);
        add(patterns, BannerPattern.CIRCLE_MIDDLE, EnumDyeColor.SILVER);
        add(patterns, BannerPattern.BORDER, EnumDyeColor.BLACK);
        stack.getOrCreateSubCompound("BlockEntityTag").setTag("Patterns", patterns);
        stack.getTagCompound().setInteger("HideFlags", 32);
        ITextComponent name = new TextComponentTranslation("block.minecraft.ominous_banner");
        name.getStyle().setColor(TextFormatting.GOLD);
        stack.setStackDisplayName(ITextComponent.Serializer.componentToJson(name));
        return stack;
    }

    public static boolean is(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.BANNER && ItemStack.areItemStacksEqual(stack, create());
    }

    private static void add(NBTTagList patterns, BannerPattern pattern, EnumDyeColor color) {
        NBTTagCompound layer = new NBTTagCompound();
        layer.setString("Pattern", pattern.getHashname());
        layer.setInteger("Color", color.getDyeDamage());
        patterns.appendTag(layer);
    }

    private OminousBanner() {}
}
