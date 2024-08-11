package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.adapter.IItemPropertyHolder;
import com.michaelsebero.portium.modern.adapter.ModernItems;
import com.michaelsebero.portium.modern.mc.item.Item$Properties;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

import java.util.function.Supplier;

/**
 * What a 1.16 mod's {@code BucketItem} subclasses extend, and what {@code new BucketItem(fluid, properties)} makes: a
 * 1.12 bucket holding a 1.16 fluid, which Forge 1.16's {@code getFluid} returns. It doesn't pick up fluids as 1.12's
 * empty bucket would.
 */
public class BucketItem116 extends ItemBucket {

    /** The 1.16 fluid's supplier (Minecraft 1.16.5's own fluid classes, which 1.12 lacks). */
    public final Supplier<?> portium$fluid;

    public BucketItem116(Supplier<?> fluid, Item$Properties properties) {
        super(Blocks.AIR);
        this.portium$fluid = fluid;
        ((IItemPropertyHolder) this).portium$applyItemProperties(properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        return new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
    }

    @Override
    public String getTranslationKey() {
        return ModernItems.translationKey(this, super.getTranslationKey());
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getTranslationKey();
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal(getTranslationKey(stack)).trim();
    }
}
