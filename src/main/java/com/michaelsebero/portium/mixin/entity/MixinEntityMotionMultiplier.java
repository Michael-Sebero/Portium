package com.michaelsebero.portium.mixin.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.entity.IMotionMultiplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 1.14's motion multiplier, the generalised cobweb: a block an entity is in (a sweet berry bush) scales its next move
 * and stops its motion. 1.12 has only the cobweb's fixed one.
 */
@Mixin(Entity.class)
public abstract class MixinEntityMotionMultiplier implements IMotionMultiplier {

    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;

    @Unique
    @Nullable
    private Vec3d portium$motionMultiplier;
    @Unique
    private boolean portium$multiplied;

    @Override
    public void portium$setMotionMultiplier(Vec3d multiplier) {
        this.portium$motionMultiplier = multiplier;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double portium$scaleX(double x, @Local(argsOnly = true) MoverType type) {
        return this.portium$applies(type) ? x * this.portium$motionMultiplier.x : x;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double portium$scaleY(double y, @Local(argsOnly = true) MoverType type) {
        return this.portium$applies(type) ? y * this.portium$motionMultiplier.y : y;
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double portium$scaleZ(double z, @Local(argsOnly = true) MoverType type) {
        if (!this.portium$applies(type)) return z;
        this.portium$multiplied = true;
        return z * this.portium$motionMultiplier.z;
    }

    @Unique
    private boolean portium$applies(MoverType type) {
        return this.portium$motionMultiplier != null && type != MoverType.PISTON;
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void portium$clearMultiplier(MoverType type, double x, double y, double z, CallbackInfo ci) {
        if (!this.portium$multiplied) return;
        this.portium$multiplied = false;
        this.portium$motionMultiplier = null;
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
    }
}
