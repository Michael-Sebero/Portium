package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.compat.LegacyBlock;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumEnchantments;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.proxy.ParticleKind;
import com.michaelsebero.portium.Portium;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/** 1.16's Soul Speed: the speed boost on soul sand and soul soil, the boots wearing, and the soul wisps. */
public final class SoulSpeed {

    private static final UUID BOOST_ID = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
    private static final ResourceLocation BLOCKS_TAG = new ResourceLocation("soul_speed_blocks");

    public static int level(EntityLivingBase entity) {
        return PortiumEnchantments.SOUL_SPEED.getRegistryName() == null ? 0 : EnchantmentHelper.getMaxEnchantmentLevel(PortiumEnchantments.SOUL_SPEED, entity);
    }

    /** Standing on soul sand or soul soil (the {@code soul_speed_blocks} tag). */
    public static boolean onSoulSpeedBlock(EntityLivingBase entity) {
        IBlockState state = entity.world.getBlockState(underneath(entity));
        DataPackManager packs = DataPackManager.get();
        Tag<LegacyBlock> tag = packs == null ? null : packs.getTags().getBlocks().get(BLOCKS_TAG);
        if (tag != null) return tag.anyMatch(block -> block.test(state));
        Block block = state.getBlock();
        return block == Blocks.SOUL_SAND || block == PortiumBlocks.SOUL_SOIL && block != null;
    }

    /** Doesn't sink into soul sand. */
    public static boolean ignoresSoulSand(net.minecraft.entity.Entity entity) {
        return entity instanceof EntityLivingBase && level((EntityLivingBase) entity) > 0;
    }

    /** On moving to a new block (1.16's {@code frostWalk}): drop the boost when off the ground, then re-add it if on a soul block. */
    public static void onChangedBlock(EntityLivingBase entity) {
        IBlockState below = entity.world.getBlockState(underneath(entity));
        if (below.getMaterial() != Material.AIR || entity.isElytraFlying()) removeBoost(entity);
        addBoost(entity);
    }

    /** On landing. */
    public static void onLanded(EntityLivingBase entity) {
        removeBoost(entity);
        addBoost(entity);
    }

    private static void removeBoost(EntityLivingBase entity) {
        IAttributeInstance speed = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(BOOST_ID) != null) speed.removeModifier(BOOST_ID);
    }

    private static void addBoost(EntityLivingBase entity) {
        if (entity.world.getBlockState(underneath(entity)).getMaterial() == Material.AIR) return;
        int level = level(entity);
        if (level <= 0 || !onSoulSpeedBlock(entity)) return;
        IAttributeInstance speed = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (speed.getModifier(BOOST_ID) == null) {
            speed.applyModifier(new AttributeModifier(BOOST_ID, "Soul speed boost", 0.03F * (1.0F + level * 0.35F), 0).setSaved(false));
        }
        if (entity.getRNG().nextFloat() < 0.04F) entity.getItemStackFromSlot(EntityEquipmentSlot.FEET).damageItem(1, entity);
    }

    /** Every fifth tick while moving on a soul block: a soul wisp, sometimes with a sound. */
    public static void tickEffects(EntityLivingBase entity) {
        if (entity.ticksExisted % 5 != 0 || entity.motionX == 0.0D || entity.motionZ == 0.0D) return;
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator()) return;
        if (level(entity) <= 0 || !onSoulSpeedBlock(entity)) return;
        if (entity.world.isRemote) {
            Portium.proxy.spawnParticle(ParticleKind.SOUL, entity.world, entity.posX + (entity.getRNG().nextDouble() - 0.5D) * entity.width,
                    entity.posY + 0.1D, entity.posZ + (entity.getRNG().nextDouble() - 0.5D) * entity.width, entity.motionX * -0.2D, 0.1D, entity.motionZ * -0.2D);
        }
        float volume = entity.getRNG().nextFloat() * 0.4F + entity.getRNG().nextFloat() > 0.9F ? 0.6F : 0.0F;
        entity.playSound(PortiumSounds.PARTICLE_SOUL_ESCAPE, volume, 0.6F + entity.getRNG().nextFloat() * 0.4F);
    }

    private static BlockPos underneath(EntityLivingBase entity) {
        return new BlockPos(entity.posX, entity.getEntityBoundingBox().minY - 0.5000001D, entity.posZ);
    }

    private SoulSpeed() {}
}
