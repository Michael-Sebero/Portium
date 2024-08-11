package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.mc.item.ItemUseContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** What Portium's item adapters share. */
public final class ModernItems {

    /** 1.12's onItemUse arguments as 1.13's context. */
    public static ItemUseContext context(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY,
                                         float hitZ) {
        return new ItemUseContext(world, player, hand, player.getHeldItem(hand), pos, facing, hitX, hitY, hitZ, false);
    }

    /** 1.13 names items item.<namespace>.<path> and translates that key as it is. */
    public static String translationKey(Item item, String fallback) {
        ResourceLocation name = item.getRegistryName();
        return name == null ? fallback : "item." + name.getNamespace() + "." + name.getPath().replace('/', '.');
    }

    private ModernItems() {}

    /** What eating food does, as 1.14 has it: feeds a player, gives the effects by chance, uses the item up. */
    public static void eat(FoodValues food, ItemStack stack, World world, net.minecraft.entity.EntityLivingBase entity) {
        world.playSound(null, entity.posX, entity.posY, entity.posZ, net.minecraft.init.SoundEvents.ENTITY_PLAYER_BURP,
                net.minecraft.util.SoundCategory.PLAYERS, 0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            player.getFoodStats().addStats(food.hunger, food.saturation);
            player.addStat(net.minecraft.stats.StatList.getObjectUseStats(stack.getItem()));
            if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
                net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger((net.minecraft.entity.player.EntityPlayerMP) player, stack);
            }
        }
        if (!world.isRemote) {
            for (int i = 0; i < food.effects.size(); i++) {
                if (world.rand.nextFloat() < food.chances.get(i)) entity.addPotionEffect(new net.minecraft.potion.PotionEffect((net.minecraft.potion.PotionEffect) food.effects.get(i)));
            }
        }
        if (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isCreativeMode) stack.shrink(1);
    }
}
