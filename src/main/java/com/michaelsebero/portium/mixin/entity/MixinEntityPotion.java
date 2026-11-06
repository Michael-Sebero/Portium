package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.block.light.BlockCampfire;
import com.michaelsebero.portium.block.nether.BlockSoulFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.projectile.EntityPotion;
import net.minecraft.init.PotionTypes;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A splash of water puts out lit campfires (1.14) and soul fire (1.16) where it lands, on the block it hit and around
 * the spot in front of it, as 1.16's {@code PotionEntity.extinguishFires} does. 1.12 still puts out its own fire.
 */
@Mixin(EntityPotion.class)
public abstract class MixinEntityPotion {

    @Inject(method = "onImpact", at = @At("HEAD"))
    private void portium$extinguish116(RayTraceResult result, CallbackInfo ci) {
        EntityPotion potion = (EntityPotion) (Object) this;
        if (potion.world.isRemote || result.typeOfHit != RayTraceResult.Type.BLOCK || result.sideHit == null) return;
        if (PotionUtils.getPotionFromItem(potion.getPotion()) != PotionTypes.WATER || !PotionUtils.getEffectsFromStack(potion.getPotion()).isEmpty()) return;
        BlockPos front = result.getBlockPos().offset(result.sideHit);
        this.portium$extinguish(front);
        this.portium$extinguish(result.getBlockPos());
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) this.portium$extinguish(front.offset(side));
    }

    private void portium$extinguish(BlockPos pos) {
        EntityPotion potion = (EntityPotion) (Object) this;
        IBlockState state = potion.world.getBlockState(pos);
        if (state.getBlock() instanceof BlockSoulFire) {
            potion.world.setBlockToAir(pos);
        } else if (BlockCampfire.isLit(state)) {
            potion.world.playEvent(null, 1009, pos, 0);
            BlockCampfire.extinguish(potion.world, pos, state);
            potion.world.setBlockState(pos, state.withProperty(BlockCampfire.LIT, false), 3);
        }
    }
}
