package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.SoulSpeed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.16's Soul Speed hooks: the boost is updated on moving to a new block and on landing; the wisps show while running. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseSoulSpeed {

    @Inject(method = "frostWalk", at = @At("TAIL"))
    private void portium$soulSpeedOnMove(BlockPos pos, CallbackInfo ci) {
        SoulSpeed.onChangedBlock((EntityLivingBase) (Object) this);
    }

    @Inject(method = "updateFallState", at = @At("HEAD"))
    private void portium$soulSpeedOnLanding(double y, boolean onGround, IBlockState state, BlockPos pos, CallbackInfo ci) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (!self.world.isRemote && onGround && self.fallDistance > 0.0F) SoulSpeed.onLanded(self);
    }

    @Inject(method = "onEntityUpdate", at = @At("TAIL"))
    private void portium$soulSpeedEffects(CallbackInfo ci) {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        if (self.isEntityAlive()) SoulSpeed.tickEffects(self);
    }
}
