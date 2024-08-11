package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.client.CameraState;
import com.michaelsebero.portium.init.PortiumPotions;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    /** Smooth camera height changes (1.14). */
    @Redirect(method = "orientCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEyeHeight()F"))
    private float portium$smoothEyeHeight(Entity entity, float partialTicks) {
        return CameraState.getCameraEyeHeight(entity, partialTicks);
    }

    /** Conduit Power brightens the world while the player is under water, like Night Vision (1.13). */
    @Redirect(method = "updateLightmap", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean portium$conduitVision(EntityPlayerSP player, Potion potion) {
        return player.isPotionActive(potion) || this.portium$hasConduitVision(player);
    }

    @Inject(method = "getNightVisionBrightness", at = @At("HEAD"), cancellable = true)
    private void portium$conduitVisionBrightness(EntityLivingBase entity, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!entity.isPotionActive(net.minecraft.init.MobEffects.NIGHT_VISION) && this.portium$hasConduitVision(entity)) {
            cir.setReturnValue(Math.max(0.6F, CameraState.getWaterVision()));
        }
    }

    private boolean portium$hasConduitVision(EntityLivingBase entity) {
        return entity.isPotionActive(PortiumPotions.CONDUIT_POWER) && entity.isInsideOfMaterial(Material.WATER);
    }

    /** Optionally turn off the camera pitch bobbing while walking. */
    @Redirect(method = "applyBobbing", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;cameraPitch:F"))
    private float portium$noPitchBobbing(EntityPlayer player) {
        return PortiumConfig.visuals.disablePitchBobbing ? 0.0F : player.cameraPitch;
    }

    @Redirect(method = "applyBobbing", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;prevCameraPitch:F"))
    private float portium$noPrevPitchBobbing(EntityPlayer player) {
        return PortiumConfig.visuals.disablePitchBobbing ? 0.0F : player.prevCameraPitch;
    }
}
