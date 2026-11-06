package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.api.ISpeedFactorBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

/** 1.15's {@code Entity.getSpeedFactor}/{@code getJumpFactor}, for blocks that are {@link ISpeedFactorBlock}s. */
public final class SpeedFactors {

    public static float speedFactor(Entity entity) {
        if (entity instanceof EntityPlayer && (((EntityPlayer) entity).capabilities.isFlying || ((EntityPlayer) entity).isElytraFlying())) return 1.0F;
        Block in = entity.world.getBlockState(new BlockPos(entity)).getBlock();
        float factor = in instanceof ISpeedFactorBlock ? ((ISpeedFactorBlock) in).getSpeedFactor() : 1.0F;
        if (in == Blocks.WATER || in == Blocks.FLOWING_WATER || factor != 1.0F) return factor;
        Block under = entity.world.getBlockState(underneath(entity)).getBlock();
        return under instanceof ISpeedFactorBlock ? ((ISpeedFactorBlock) under).getSpeedFactor() : 1.0F;
    }

    public static float jumpFactor(Entity entity) {
        Block in = entity.world.getBlockState(new BlockPos(entity)).getBlock();
        float factor = in instanceof ISpeedFactorBlock ? ((ISpeedFactorBlock) in).getJumpFactor() : 1.0F;
        if (factor != 1.0F) return factor;
        Block under = entity.world.getBlockState(underneath(entity)).getBlock();
        return under instanceof ISpeedFactorBlock ? ((ISpeedFactorBlock) under).getJumpFactor() : 1.0F;
    }

    private static BlockPos underneath(Entity entity) {
        return new BlockPos(entity.posX, entity.getEntityBoundingBox().minY - 0.5000001D, entity.posZ);
    }

    private SpeedFactors() {}
}
