package com.michaelsebero.portium.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.api.IPosedPlayer;
import com.michaelsebero.portium.api.PlayerPose;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client movement for swimming and poses: sprinting starts in water and isn't broken by bumping into things while
 * swimming (1.13), crawling and forced crouching slow the player like sneaking (1.14), and elytra can be opened
 * while rising (1.15).
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {

    @Shadow
    public MovementInput movementInput;

    private MixinEntityPlayerSP(World world, GameProfile profile) {
        super(world, profile);
    }

    /** Sneak-speed movement whenever the body is low, whether or not sneak is held. */
    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;onInputUpdate(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovementInput;)V", shift = At.Shift.AFTER, remap = false))
    private void portium$poseMovementSpeed(CallbackInfo ci) {
        PlayerPose pose = IPosedPlayer.of(this).portium$getPose();
        boolean forcedDown = pose == PlayerPose.CROUCHING || (pose == PlayerPose.SWIMMING && !this.isInWater());
        if (this.capabilities.isFlying) return;
        if (forcedDown && !this.movementInput.sneak) {
            this.movementInput.moveStrafe *= 0.3F;
            this.movementInput.moveForward *= 0.3F;
        } else if (!forcedDown && this.movementInput.sneak && IPosedPlayer.of(this).portium$isSwimming()) {
            // Holding sneak while swimming steers down instead of slowing down.
            this.movementInput.moveStrafe /= 0.3F;
            this.movementInput.moveForward /= 0.3F;
        }
        // Sneaking in water sinks faster (1.13).
        if (PortiumConfig.mechanics.swimming && this.isInWater() && this.movementInput.sneak) this.motionY -= 0.04F;
    }

    /** Double-tapping forward starts sprinting under water too. */
    @ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;onGround:Z", ordinal = 0))
    private boolean portium$sprintStartsInWater(boolean onGround) {
        return onGround || (PortiumConfig.mechanics.swimming && IPosedPlayer.of(this).portium$canSwim());
    }

    /** Swimming players keep sprinting when they brush against blocks. */
    @ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;collidedHorizontally:Z"))
    private boolean portium$keepSwimmingAlongWalls(boolean collided) {
        return collided && !IPosedPlayer.of(this).portium$isSwimming();
    }

    /** Sprinting on the water surface stops, as in 1.13, unless already swimming. */
    @Inject(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerCapabilities;allowFlying:Z", ordinal = 0))
    private void portium$stopSprintingOnSurface(CallbackInfo ci) {
        IPosedPlayer posed = IPosedPlayer.of(this);
        if (PortiumConfig.mechanics.swimming && this.isSprinting() && !this.capabilities.isFlying
                && this.isInWater() && !posed.portium$canSwim() && !posed.portium$isSwimming()) {
            this.setSprinting(false);
        }
        if (posed.portium$isSwimming() && !this.isInWater()) this.setSprinting(false);
    }

    /** Elytra can open while the player is still moving up (1.15). */
    @ModifyExpressionValue(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;motionY:D", ordinal = 0))
    private double portium$allowRisingTakeoff(double motionY) {
        return PortiumConfig.mechanics.easyElytraTakeoff ? -1.0D : motionY;
    }
}
