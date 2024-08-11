package com.michaelsebero.portium.mixin.entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.api.IPosedPlayer;
import com.michaelsebero.portium.api.IRiptideEntity;
import com.michaelsebero.portium.api.PlayerPose;
import com.michaelsebero.portium.block.water.WaterUtil;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.13 swimming and 1.14 poses for players: the swimming flag, pose-based hitbox and eye height
 * (crouching under low ceilings, crawling through gaps), and steering up and down while swimming.
 */
@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends EntityLivingBase implements IPosedPlayer {

    @Unique
    private static final DataParameter<Boolean> PORTIUM_SWIMMING = EntityDataManager.createKey(EntityPlayer.class, DataSerializers.BOOLEAN);

    @Unique
    private PlayerPose portium$pose = PlayerPose.STANDING;
    @Unique
    private boolean portium$eyesInWater;
    @Unique
    private float portium$swimAnimation;
    @Unique
    private float portium$prevSwimAnimation;

    @Shadow
    public PlayerCapabilities capabilities;

    @Shadow
    public abstract boolean isSpectator();

    @Shadow
    protected abstract void spawnShoulderEntities();

    private MixinEntityPlayer(World world) {
        super(world);
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerData(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_SWIMMING, false);
    }

    // ---- IPosedPlayer ----

    @Override
    public PlayerPose portium$getPose() {
        return this.portium$pose == null ? PlayerPose.STANDING : this.portium$pose;
    }

    @Override
    public boolean portium$isSwimming() {
        return this.dataManager.get(PORTIUM_SWIMMING);
    }

    @Override
    public boolean portium$canSwim() {
        return this.portium$eyesInWater && this.isInWater();
    }

    @Override
    public float portium$getSwimAnimation(float partialTicks) {
        return this.portium$prevSwimAnimation + (this.portium$swimAnimation - this.portium$prevSwimAnimation) * partialTicks;
    }

    // ---- Swimming ----

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void portium$updateSwimming(CallbackInfo ci) {
        this.portium$eyesInWater = this.isInsideOfMaterial(Material.WATER);
        boolean swimming;
        if (!PortiumConfig.mechanics.swimming || this.capabilities.isFlying || this.isSpectator()) {
            swimming = false;
        } else if (this.portium$isSwimming()) {
            swimming = this.isSprinting() && this.isInWater() && !this.isRiding();
        } else {
            swimming = this.isSprinting() && this.portium$canSwim() && !this.isRiding();
        }
        if (swimming != this.portium$isSwimming()) this.dataManager.set(PORTIUM_SWIMMING, swimming);

        this.portium$prevSwimAnimation = this.portium$swimAnimation;
        this.portium$swimAnimation = this.portium$isActuallySwimming()
                ? Math.min(1.0F, this.portium$swimAnimation + 0.09F)
                : Math.max(0.0F, this.portium$swimAnimation - 0.09F);
    }

    /** While swimming, the player rises or dives towards where they look. */
    @Inject(method = "travel", at = @At("HEAD"))
    private void portium$swimTowardsLook(float strafe, float vertical, float forward, CallbackInfo ci) {
        if (this.portium$isSwimming() && !this.isRiding()) {
            double lookY = this.getLookVec().y;
            double factor = lookY < -0.2D ? 0.085D : 0.06D;
            boolean waterAbove = WaterUtil.isWater(this.world.getBlockState(new BlockPos(this.posX, this.posY + 0.9D, this.posZ)));
            if (lookY <= 0.0D || this.isJumping || waterAbove) {
                this.motionY += (lookY - this.motionY) * factor;
            }
        }
    }

    // ---- Poses ----

    @Unique
    private boolean portium$fits(PlayerPose pose) {
        double halfWidth = pose.getWidth() / 2.0D;
        AxisAlignedBB box = new AxisAlignedBB(this.posX - halfWidth, this.posY, this.posZ - halfWidth,
                this.posX + halfWidth, this.posY + pose.getHeight(), this.posZ + halfWidth).shrink(1.0E-7D);
        return this.world.getCollisionBoxes(this, box).isEmpty();
    }

    @Unique
    private PlayerPose portium$choosePose() {
        PlayerPose desired;
        if (this.isElytraFlying()) desired = PlayerPose.FALL_FLYING;
        else if (this.isPlayerSleeping()) desired = PlayerPose.SLEEPING;
        else if (this.portium$isSwimming()) desired = PlayerPose.SWIMMING;
        else if (((IRiptideEntity) this).portium$isSpinAttacking()) desired = PlayerPose.SPIN_ATTACK;
        else if (this.isSneaking() && !this.capabilities.isFlying) desired = PlayerPose.CROUCHING;
        else desired = PlayerPose.STANDING;

        if (this.isSpectator() || this.isRiding() || this.noClip || this.portium$fits(desired)) return desired;
        if (this.portium$fits(PlayerPose.CROUCHING)) return PlayerPose.CROUCHING;
        return PortiumConfig.mechanics.crawling ? PlayerPose.SWIMMING : desired;
    }

    /** Replaces 1.12's fixed sizes with the pose's hitbox. The Forge post-tick event that ended the method still fires. */
    @Inject(method = "updateSize", at = @At("HEAD"), cancellable = true)
    private void portium$sizeFromPose(CallbackInfo ci) {
        PlayerPose pose = this.portium$choosePose();
        this.portium$pose = pose;
        float width = pose.getWidth();
        float height = pose.getHeight();
        if (width != this.width || height != this.height) {
            AxisAlignedBB box = this.getEntityBoundingBox();
            box = new AxisAlignedBB(box.minX, box.minY, box.minZ, box.minX + width, box.minY + height, box.minZ + width);
            if (!this.world.collidesWithAnyBlock(box)) this.setSize(width, height);
        }
        FMLCommonHandler.instance().onPlayerPostTick((EntityPlayer) (Object) this);
        ci.cancel();
    }

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void portium$eyeHeightFromPose(CallbackInfoReturnable<Float> cir) {
        if (this.portium$pose != null) cir.setReturnValue(this.portium$pose.getEyeHeight((EntityPlayer) (Object) this));
    }

    // ---- Shoulder parrots ----

    @WrapWithCondition(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;spawnShoulderEntities()V"))
    private boolean portium$keepParrotsWhenJumping(EntityPlayer self) {
        return !PortiumConfig.mechanics.sneakToDropParrots;
    }

    @Inject(method = "onLivingUpdate", at = @At("TAIL"))
    private void portium$dropParrotsWhenSneaking(CallbackInfo ci) {
        if (PortiumConfig.mechanics.sneakToDropParrots
                && ((!this.world.isRemote && (this.isSneaking() || this.isInWater() || this.isRiding())) || this.capabilities.isFlying)) {
            this.spawnShoulderEntities();
        }
    }
}
