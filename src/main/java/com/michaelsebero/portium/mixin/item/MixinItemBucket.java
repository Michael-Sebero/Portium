package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.advancements.PortiumTriggers;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Filling a bucket with water or lava counts for the {@code filled_bucket} advancement trigger, and pouring water
 * into a block that can hold it waterlogs the block instead of failing (1.13). Players go through
 * {@code WaterlogEvents}; this covers dispensers and anything else that pours a bucket at a position.
 */
@Mixin(ItemBucket.class)
public abstract class MixinItemBucket {

    @Shadow @Final private Block containedBlock;

    @Inject(method = "fillBucket", at = @At("HEAD"))
    private void portium$triggerFilledBucket(ItemStack empty, EntityPlayer player, Item full, CallbackInfoReturnable<ItemStack> cir) {
        if (player instanceof EntityPlayerMP) PortiumTriggers.FILLED_BUCKET.trigger((EntityPlayerMP) player, new ItemStack(full));
    }

    @Inject(method = "tryPlaceContainedLiquid", at = @At("HEAD"), cancellable = true)
    private void portium$waterlog(@Nullable EntityPlayer player, World world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.containedBlock != Blocks.FLOWING_WATER || !Waterlogging.isEnabled()) return;
        if (!Waterlogging.canWaterlog(world.getBlockState(pos)) || Waterlogging.isWaterlogged(world, pos)) return;
        if (world.provider.doesWaterVaporize()) {
            world.playSound(player, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
            for (int i = 0; i < 8; i++) {
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, pos.getX() + Math.random(), pos.getY() + Math.random(), pos.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
            }
        } else if (Waterlogging.setWaterlogged(world, pos, true)) {
            world.playSound(player, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        cir.setReturnValue(true);
    }
}
