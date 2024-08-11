package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.item.Item$Properties;
import com.michaelsebero.portium.modern.mc.item.ItemUseContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;

/**
 * What a 1.13 mod's {@code Item} subclasses extend instead of 1.12's {@code Item}, and what {@code new Item(...)} in
 * 1.13 code makes: 1.12's item made from 1.13's properties, with 1.13's {@code onItemUse} and names.
 */
public class ModernItem extends Item {

    /** 1.14's food, if the item is one. */
    @javax.annotation.Nullable
    private final FoodValues food;

    public ModernItem(Item$Properties properties) {
        ((IItemPropertyHolder) this).portium$applyItemProperties(properties);
        this.food = properties.getFood();
    }

    @javax.annotation.Nullable
    public FoodValues portium$getFood() {
        return this.food;
    }

    // Eating, as 1.14's items with food do.

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return this.food == null ? super.getMaxItemUseDuration(stack) : this.food.fast ? 16 : 32;
    }

    @Override
    public net.minecraft.item.EnumAction getItemUseAction(ItemStack stack) {
        return this.food == null ? super.getItemUseAction(stack) : net.minecraft.item.EnumAction.EAT;
    }

    @Override
    public net.minecraft.util.ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (this.food == null) return super.onItemRightClick(world, player, hand);
        ItemStack stack = player.getHeldItem(hand);
        if (!player.canEat(this.food.alwaysEdible)) return new net.minecraft.util.ActionResult<>(EnumActionResult.FAIL, stack);
        player.setActiveHand(hand);
        return new net.minecraft.util.ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, net.minecraft.entity.EntityLivingBase entity) {
        if (this.food == null) return super.onItemUseFinish(stack, world, entity);
        ModernItems.eat(this.food, stack, world, entity);
        return stack;
    }

    @Srg("func_195939_a")
    public EnumActionResult onItemUse(ItemUseContext context) {
        return EnumActionResult.PASS;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY,
                                      float hitZ) {
        return onItemUse(ModernItems.context(player, world, pos, hand, facing, hitX, hitY, hitZ));
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
