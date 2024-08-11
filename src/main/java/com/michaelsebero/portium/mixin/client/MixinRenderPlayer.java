package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.api.IPosedPlayer;
import com.michaelsebero.portium.client.PortiumRenderState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tilts swimming and crawling players horizontal (1.13), and keeps those poses off the first-person arm. */
@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer {

    @Inject(method = "applyRotations(Lnet/minecraft/client/entity/AbstractClientPlayer;FFF)V", at = @At("TAIL"))
    private void portium$swimRotation(AbstractClientPlayer player, float ageInTicks, float rotationYaw, float partialTicks, CallbackInfo ci) {
        if (player.isPlayerSleeping() || player.isElytraFlying()) return;
        IPosedPlayer posed = IPosedPlayer.of(player);
        float swim = posed.portium$getSwimAnimation(partialTicks);
        if (swim <= 0.0F) return;
        float target = player.isInWater() ? -90.0F - player.rotationPitch : -90.0F;
        GlStateManager.rotate(swim * target, 1.0F, 0.0F, 0.0F);
        if (posed.portium$isActuallySwimming()) GlStateManager.translate(0.0F, -1.0F, 0.3F);
    }

    @Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("HEAD"))
    private void portium$beginFirstPersonArm(AbstractClientPlayer player, CallbackInfo ci) {
        PortiumRenderState.renderingFirstPersonArm = true;
    }

    @Inject(method = {"renderRightArm", "renderLeftArm"}, at = @At("RETURN"))
    private void portium$endFirstPersonArm(AbstractClientPlayer player, CallbackInfo ci) {
        PortiumRenderState.renderingFirstPersonArm = false;
    }
}
