package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.IShieldBlockReaction;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.14: an attacker blocked by a shield reacts its own way (1.14's {@code constructKnockBackVector}); a ravager is stunned or flings,
 * a hoglin or zoglin flings the blocker. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseShieldBlock {

    @Inject(method = "blockUsingShield", at = @At("HEAD"), cancellable = true)
    private void portium$ravagerBlocked(EntityLivingBase attacker, CallbackInfo ci) {
        if (attacker instanceof IShieldBlockReaction) {
            ((IShieldBlockReaction) attacker).onBlockedByShield((EntityLivingBase) (Object) this);
            ci.cancel();
        }
    }
}
