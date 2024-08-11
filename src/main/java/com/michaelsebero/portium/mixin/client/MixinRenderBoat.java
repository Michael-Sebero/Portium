package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.api.IRockingBoat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.entity.item.EntityBoat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderBoat.class)
public abstract class MixinRenderBoat {

    @Inject(method = "setupRotation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;scale(FFF)V"))
    private void portium$rock(EntityBoat boat, float entityYaw, float partialTicks, CallbackInfo ci) {
        float angle = ((IRockingBoat) boat).portium$getRockingAngle(partialTicks);
        if (Math.abs(angle) > 1.0E-5F) GlStateManager.rotate(angle, 1.0F, 0.0F, 1.0F);
    }
}
