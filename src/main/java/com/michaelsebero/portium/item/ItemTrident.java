package com.michaelsebero.portium.item;

import com.google.common.collect.Multimap;
import com.michaelsebero.portium.api.IRiptideEntity;
import com.michaelsebero.portium.entity.EntityTrident;
import com.michaelsebero.portium.init.PortiumEnchantments;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * The trident (1.13): a 9-damage melee weapon that is thrown by holding and releasing use.
 * With Riptide it launches the player instead, but only while wet.
 */
public class ItemTrident extends Item {

    /** Minimum use time before releasing throws. */
    private static final int MIN_CHARGE = 10;

    public ItemTrident() {
        this.setMaxStackSize(1);
        this.setMaxDamage(250);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.addPropertyOverride(new ResourceLocation("throwing"),
                (stack, world, entity) -> entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack ? 1.0F : 0.0F);
    }

    /** The use animation is drawn by Portium's renderers, so vanilla shouldn't add one. */
    @Nonnull
    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.NONE;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItemDamage() >= stack.getMaxDamage() - 1) return new ActionResult<>(EnumActionResult.FAIL, stack);
        if (EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.RIPTIDE, stack) > 0 && !player.isWet()) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity, int timeLeft) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;
        if (this.getMaxItemUseDuration(stack) - timeLeft < MIN_CHARGE) return;

        int riptide = EnchantmentHelper.getEnchantmentLevel(PortiumEnchantments.RIPTIDE, stack);
        if (riptide > 0 && !player.isWet()) return;

        if (!world.isRemote) {
            stack.damageItem(1, player);
            if (riptide == 0) {
                EntityTrident trident = new EntityTrident(world, player, stack);
                trident.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, 2.5F, 1.0F);
                if (player.capabilities.isCreativeMode) trident.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
                world.spawnEntity(trident);
                if (!player.capabilities.isCreativeMode) player.inventory.deleteStack(stack);
            }
        }
        player.addStat(StatList.getObjectUseStats(this));

        SoundEvent sound = PortiumSounds.ITEM_TRIDENT_THROW;
        if (riptide > 0) {
            launch(player, riptide);
            sound = riptide >= 3 ? PortiumSounds.ITEM_TRIDENT_RIPTIDE_3 : riptide == 2 ? PortiumSounds.ITEM_TRIDENT_RIPTIDE_2 : PortiumSounds.ITEM_TRIDENT_RIPTIDE_1;
        }
        world.playSound(null, player.posX, player.posY, player.posZ, sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    private static void launch(EntityPlayer player, int riptide) {
        float yaw = player.rotationYaw * 0.017453292F;
        float pitch = player.rotationPitch * 0.017453292F;
        float x = -MathHelper.sin(yaw) * MathHelper.cos(pitch);
        float y = -MathHelper.sin(pitch);
        float z = MathHelper.cos(yaw) * MathHelper.cos(pitch);
        float length = MathHelper.sqrt(x * x + y * y + z * z);
        float speed = 3.0F * ((1.0F + riptide) / 4.0F);
        player.addVelocity(x * speed / length, y * speed / length, z * speed / length);
        IRiptideEntity.of(player).portium$startSpinAttack(20);
        if (player.onGround) player.move(MoverType.SELF, 0.0D, 1.1999999F, 0.0D);
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
        if (state.getBlockHardness(world, pos) != 0.0F) stack.damageItem(2, entity);
        return true;
    }

    @Nonnull
    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> modifiers = super.getAttributeModifiers(slot, stack);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", 8.0D, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.9D, 0));
        }
        return modifiers;
    }

    @Override
    public int getItemEnchantability() {
        return 1;
    }
}
