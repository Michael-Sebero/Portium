package com.michaelsebero.portium.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** A banner pattern (1.14): put in a loom, it unlocks its pattern for the banner. */
public class ItemBannerPattern extends Item {

    private final String hash;
    private final EnumRarity rarity;

    public ItemBannerPattern(String hash, EnumRarity rarity) {
        this.hash = hash;
        this.rarity = rarity;
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.MISC);
    }

    /** The pattern's index in 1.16's order ({@link BannerPatterns116#ORDER}). */
    public int getPatternIndex() {
        return BannerPatterns116.indexOf(this.hash);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return this.rarity;
    }

    @SuppressWarnings("deprecation")
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + I18n.translateToLocal(this.getTranslationKey() + ".desc"));
    }
}
