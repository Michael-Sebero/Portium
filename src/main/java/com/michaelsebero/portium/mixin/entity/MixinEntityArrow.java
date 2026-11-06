package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.EntityTrident;
import net.minecraft.entity.projectile.EntityArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Tridents keep their speed under water (1.13 water drag 0.99), where arrows slow to a stop; 1.16's stuck-arrow margin. */
@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow {

    @ModifyConstant(method = "onUpdate", constant = @Constant(floatValue = 0.6F))
    private float portium$tridentWaterDrag(float drag) {
        return (Object) this instanceof EntityTrident ? 0.99F : drag;
    }

    /**
     * An arrow stuck in a block whose state changes (a target block powering) stays stuck while the block still
     * touches it: 1.16 looks 0.06 around it, 1.12 only 0.05, which an arrow pulled back 0.05 from a top face misses,
     * so it fell and hit again.
     */
    @ModifyConstant(method = "onUpdate", constant = @Constant(doubleValue = 0.05D))
    private double portium$stuckMargin(double margin) {
        return 0.06D;
    }
}
