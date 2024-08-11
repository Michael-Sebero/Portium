package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItemFrame;
import net.minecraft.entity.item.EntityItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tilts item frames on floors and ceilings to lie flat (1.13); wall frames have no pitch. */
@Mixin(RenderItemFrame.class)
public abstract class MixinRenderItemFrame {

    @Inject(method = "doRender(Lnet/minecraft/entity/item/EntityItemFrame;DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;rotate(FFFF)V", ordinal = 0))
    private void portium$pitch(EntityItemFrame frame, double x, double y, double z, float yaw, float partialTicks, CallbackInfo ci) {
        GlStateManager.rotate(frame.rotationPitch, 1.0F, 0.0F, 0.0F);
    }
}
