package com.michaelsebero.portium.mixin.modern;

import com.michaelsebero.portium.modern.mc.entity.ai.attributes.GlobalEntityTypeAttributes;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Newer mods' mobs get the attributes of their entity type as they're made (1.16 keeps attributes per type). */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseAttributes116 {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;applyEntityAttributes()V", shift = At.Shift.AFTER))
    private void portium$applyTypeAttributes(CallbackInfo callback) {
        GlobalEntityTypeAttributes.apply((EntityLivingBase) (Object) this);
    }
}
