package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.SpeedFactors;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.15's jump factor: jumping off a honey block goes half as high. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseJumpFactor {

    @Inject(method = "getJumpUpwardsMotion", at = @At("RETURN"), cancellable = true)
    private void portium$jumpFactor(CallbackInfoReturnable<Float> cir) {
        float factor = SpeedFactors.jumpFactor((EntityLivingBase) (Object) this);
        if (factor != 1.0F) cir.setReturnValue(cir.getReturnValue() * factor);
    }
}
