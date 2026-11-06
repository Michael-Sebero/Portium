package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.SpeedFactors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.15's speed factor: after moving (and touching blocks), motion across a honey block is scaled down. */
@Mixin(Entity.class)
public abstract class MixinEntitySpeedFactor {

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;doBlockCollisions()V", shift = At.Shift.AFTER))
    private void portium$speedFactor(MoverType type, double x, double y, double z, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        float factor = SpeedFactors.speedFactor(self);
        if (factor != 1.0F) {
            self.motionX *= factor;
            self.motionZ *= factor;
        }
    }
}
