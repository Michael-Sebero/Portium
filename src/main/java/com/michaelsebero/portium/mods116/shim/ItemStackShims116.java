package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.modern.mc.item.ItemUseContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Forge 1.16's {@code IForgeItemStack} defaults, which 1.16's item stacks have and 1.12's don't: each asks the stack's
 * item through 1.12 Forge's method of the same purpose, as Forge 1.16's own defaults ask the item.
 */
public final class ItemStackShims116 {

    // Comparing

    /**
     * Forge's {@code equals(other, limitTags)}: count, item and either the shared tag or the whole tag. 1.12's metadata
     * counts as part of the item (white and black wool are one 1.12 item) and of the tag (1.16 keeps damage there).
     */
    public static boolean equals(ItemStack stack, ItemStack other, boolean limitTags) {
        if (stack.isEmpty()) return other.isEmpty();
        if (other.isEmpty() || stack.getCount() != other.getCount() || stack.getItem() != other.getItem()) return false;
        if (stack.getMetadata() != other.getMetadata()) return false;
        return limitTags ? areShareTagsEqual(stack, other) : ItemStack.areItemStackTagsEqual(stack, other);
    }

    public static boolean areShareTagsEqual(ItemStack stack, ItemStack other) {
        NBTTagCompound a = getShareTag(stack);
        NBTTagCompound b = getShareTag(other);
        return a == null ? b == null : a.equals(b);
    }

    @Nullable
    public static NBTTagCompound getShareTag(ItemStack stack) {
        return stack.getItem().getNBTShareTag(stack);
    }

    public static void readShareTag(ItemStack stack, @Nullable NBTTagCompound nbt) {
        stack.getItem().readNBTShareTag(stack, nbt);
    }

    // Containers and fuel

    public static ItemStack getContainerItem(ItemStack stack) {
        return stack.getItem().getContainerItem(stack);
    }

    public static boolean hasContainerItem(ItemStack stack) {
        return stack.getItem().hasContainerItem(stack);
    }

    /** By recipe type, which 1.12 doesn't tell apart: the stack's burn time for any furnace. */
    public static int getBurnTime(ItemStack stack, @Nullable Object recipeType) {
        return stack.getItem().getItemBurnTime(stack);
    }

    // Use and breaking

    public static EnumActionResult onItemUseFirst(ItemStack stack, ItemUseContext context) {
        EntityPlayer player = context.getPlayer();
        World world = context.getWorld();
        BlockPos pos = context.getPos();
        Vec3d hit = context.getHitVec();
        Item item = stack.getItem();
        EnumActionResult result = item.onItemUseFirst(player, world, pos, context.getFace(), (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()),
                (float) (hit.z - pos.getZ()), context.getHand());
        if (player != null && result == EnumActionResult.SUCCESS) {
            net.minecraft.stats.StatBase used = StatList.getObjectUseStats(item);
            if (used != null) player.addStat(used);
        }
        return result;
    }

    public static boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
        return !stack.isEmpty() && stack.getItem().onBlockStartBreak(stack, pos, player);
    }

    public static boolean shouldCauseBlockBreakReset(ItemStack stack, ItemStack newStack) {
        return stack.getItem().shouldCauseBlockBreakReset(stack, newStack);
    }

    public static boolean doesSneakBypassUse(ItemStack stack, World world, BlockPos pos, EntityPlayer player) {
        return stack.isEmpty() || stack.getItem().doesSneakBypassUse(stack, world, pos, player);
    }

    public static boolean onEntitySwing(ItemStack stack, EntityLivingBase entity) {
        return stack.getItem().onEntitySwing(entity, stack);
    }

    public static void onUsingTick(ItemStack stack, EntityLivingBase entity, int count) {
        stack.getItem().onUsingTick(stack, entity, count);
    }

    // Enchanting and repairing

    public static boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return stack.getItem().canApplyAtEnchantingTable(stack, enchantment);
    }

    public static int getItemEnchantability(ItemStack stack) {
        return stack.getItem().getItemEnchantability(stack);
    }

    public static boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return stack.getItem().isBookEnchantable(stack, book);
    }

    public static float getXpRepairRatio(ItemStack stack) {
        return stack.getItem().getXpRepairRatio(stack);
    }

    public static boolean isRepairable(ItemStack stack) {
        return stack.getItem().isRepairable();
    }

    // Wearing and holding

    @Nullable
    public static EntityEquipmentSlot getEquipmentSlot(ItemStack stack) {
        return stack.getItem().getEquipmentSlot(stack);
    }

    public static boolean canEquip(ItemStack stack, EntityEquipmentSlot slot, Entity entity) {
        return stack.getItem().isValidArmor(stack, slot, entity);
    }

    public static boolean canDisableShield(ItemStack stack, ItemStack shield, EntityLivingBase entity, EntityLivingBase attacker) {
        return stack.getItem().canDisableShield(stack, shield, entity, attacker);
    }

    public static boolean isShield(ItemStack stack, @Nullable EntityLivingBase entity) {
        return stack.getItem().isShield(stack, entity);
    }

    public static void onArmorTick(ItemStack stack, World world, EntityPlayer player) {
        stack.getItem().onArmorTick(world, player, stack);
    }

    public static void onHorseArmorTick(ItemStack stack, World world, EntityLiving horse) {
        stack.getItem().onHorseArmorTick(world, horse, stack);
    }

    public static boolean canElytraFly(ItemStack stack, EntityLivingBase entity) {
        return stack.getItem() == Items.ELYTRA && ItemElytra.isUsable(stack);
    }

    /** 1.12 wears an elytra down itself while flying. */
    public static boolean elytraFlightTick(ItemStack stack, EntityLivingBase entity, int flightTicks) {
        return false;
    }

    /** 1.16's piglins, which 1.12 doesn't have, take gold ingots and leave gold armour alone. */
    public static boolean isPiglinCurrency(ItemStack stack) {
        return stack.getItem() == Items.GOLD_INGOT;
    }

    public static boolean makesPiglinsNeutral(ItemStack stack, EntityLivingBase wearer) {
        return stack.getItem() instanceof ItemArmor && ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD;
    }

    public static boolean isEnderMask(ItemStack stack, EntityPlayer player, EntityEnderman enderman) {
        return stack.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN);
    }

    // Dropped items

    public static int getEntityLifespan(ItemStack stack, World world) {
        return stack.getItem().getEntityLifespan(stack, world);
    }

    public static boolean onEntityItemUpdate(ItemStack stack, EntityItem entity) {
        return stack.getItem().onEntityItemUpdate(entity);
    }

    public static boolean onDroppedByPlayer(ItemStack stack, EntityPlayer player) {
        return stack.getItem().onDroppedByPlayer(stack, player);
    }

    /** The name shown above the hotbar: 1.12's takes and gives text, so the mod's styling stays only if unchanged. */
    public static ITextComponent getHighlightTip(ItemStack stack, ITextComponent displayName) {
        String name = displayName.getFormattedText();
        String tip = stack.getItem().getHighlightTip(stack, name);
        return name.equals(tip) ? displayName : new TextComponentString(tip);
    }

    private ItemStackShims116() {}
}
