package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.api.IRiptideEntity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Entities using Riptide spin head-first along their path (1.13). */
@Mixin(RenderLivingBase.class)
public abstract class MixinRenderLivingBase {

    @Inject(method = "applyRotations", at = @At("TAIL"))
    private void portium$spinAttackRotation(EntityLivingBase entity, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (entity.deathTime == 0 && IRiptideEntity.of(entity).portium$isSpinAttacking()) {
            GlStateManager.rotate(-90.0F - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate((entity.ticksExisted + partialTicks) * -75.0F, 0.0F, 1.0F, 0.0F);
        }
    }
}
