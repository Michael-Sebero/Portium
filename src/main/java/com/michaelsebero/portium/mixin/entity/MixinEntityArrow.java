package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.EntityTrident;
import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Tridents keep their speed under water (1.13 water drag 0.99), where arrows slow to a stop. */
@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow {

    @ModifyConstant(method = "onUpdate", constant = @Constant(floatValue = 0.6F))
    private float portium$tridentWaterDrag(float drag) {
        return (Object) this instanceof EntityTrident ? 0.99F : drag;
    }
}
