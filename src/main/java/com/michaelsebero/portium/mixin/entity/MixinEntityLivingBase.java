package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.api.IRiptideEntity;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumPotions;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Living entity changes from 1.13: the Riptide spin attack, Slow Falling, the new water physics,
 * gradual air refill, and Conduit Power acting as water breathing and haste.
 */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity implements IRiptideEntity {

    @Unique
    private static final DataParameter<Boolean> PORTIUM_SPIN_ATTACK = EntityDataManager.createKey(EntityLivingBase.class, DataSerializers.BOOLEAN);

    @Unique
    private int portium$spinTicks;
    @Unique
    private AxisAlignedBB portium$boxBeforeTravel;

    @Shadow
    public abstract boolean isPotionActive(Potion potion);

    @Shadow
    @Nullable
    public abstract PotionEffect getActivePotionEffect(Potion potion);

    @Shadow
    protected abstract float getWaterSlowDown();

    @Shadow
    public abstract EnumCreatureAttribute getCreatureAttribute();

    private MixinEntityLivingBase(World world) {
        super(world);
    }

    /** Undead mobs never run out of air (1.13), so zombies live to turn into drowned and drowned don't drown. */
    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void portium$undeadBreatheUnderwater(CallbackInfoReturnable<Boolean> cir) {
        if (PortiumConfig.mechanics.undeadBreatheUnderwater && this.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD) cir.setReturnValue(true);
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerData(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_SPIN_ATTACK, false);
    }

    // ---- Riptide ----

    @Override
    public void portium$startSpinAttack(int ticks) {
        this.portium$spinTicks = ticks;
        if (!this.world.isRemote) this.dataManager.set(PORTIUM_SPIN_ATTACK, true);
    }

    @Override
    public boolean portium$isSpinAttacking() {
        return this.dataManager.get(PORTIUM_SPIN_ATTACK) || this.portium$spinTicks > 0;
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V"))
    private void portium$rememberBoxBeforeTravel(CallbackInfo ci) {
        this.portium$boxBeforeTravel = this.getEntityBoundingBox();
    }

    @Inject(method = "onLivingUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;travel(FFF)V", shift = At.Shift.AFTER))
    private void portium$updateSpinAttack(CallbackInfo ci) {
        if (this.portium$spinTicks > 0) {
            --this.portium$spinTicks;
            AxisAlignedBB swept = this.portium$boxBeforeTravel.union(this.getEntityBoundingBox());
            List<Entity> touching = this.world.getEntitiesWithinAABBExcludingEntity(this, swept);
            if (!touching.isEmpty()) {
                for (Entity entity : touching) {
                    if (entity instanceof EntityLivingBase) {
                        this.portium$spinAttack((EntityLivingBase) entity);
                        this.portium$spinTicks = 0;
                        this.motionX *= -0.2D;
                        this.motionY *= -0.2D;
                        this.motionZ *= -0.2D;
                        break;
                    }
                }
            } else if (this.collidedHorizontally) {
                this.portium$spinTicks = 0;
            }
        }
        if (!this.world.isRemote && this.portium$spinTicks <= 0 && this.dataManager.get(PORTIUM_SPIN_ATTACK)) {
            this.dataManager.set(PORTIUM_SPIN_ATTACK, false);
        }
    }

    @Unique
    private void portium$spinAttack(EntityLivingBase target) {
        if (!this.world.isRemote && (Object) this instanceof EntityPlayer) {
            ((EntityPlayer) (Object) this).attackTargetEntityWithCurrentItem(target);
        }
    }

    // ---- Slow Falling and 1.13 water physics ----

    @Unique
    private boolean portium$isSlowFalling() {
        return this.motionY <= 0.0D && this.isPotionActive(PortiumPotions.SLOW_FALLING);
    }

    @Unique
    private double portium$gravity() {
        return this.portium$isSlowFalling() ? 0.01D : 0.08D;
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void portium$slowFallingSafety(float strafe, float vertical, float forward, CallbackInfo ci) {
        if (this.portium$isSlowFalling()) this.fallDistance = 0.0F;
    }

    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.08D))
    private double portium$gravityOnLand(double original) {
        return this.portium$gravity();
    }

    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = -0.08D))
    private double portium$gravityWhileGliding(double original) {
        return -this.portium$gravity();
    }

    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.02D, ordinal = 0))
    private double portium$gravityInLava(double original) {
        return this.portium$gravity() / 4.0D;
    }

    /** 1.13 water pulls down at a sixteenth of normal gravity, and not at all while swimming. */
    @ModifyConstant(method = "travel", constant = @Constant(doubleValue = 0.02D, ordinal = 1))
    private double portium$gravityInWater(double original) {
        if (!PortiumConfig.mechanics.swimming) return original;
        if (this.isSprinting()) return 0.0D;
        return this.portium$gravity() / 16.0D;
    }

    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;getWaterSlowDown()F"))
    private float portium$waterDrag(EntityLivingBase self) {
        if (this.isPotionActive(PortiumPotions.DOLPHINS_GRACE)) return 0.96F;
        if (PortiumConfig.mechanics.swimming && this.isSprinting()) return 0.9F;
        return this.getWaterSlowDown();
    }

    // ---- Air ----

    /** Upward bubble columns give air, like being above water (1.13). */
    @Redirect(method = "onEntityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInsideOfMaterial(Lnet/minecraft/block/material/Material;)Z"))
    private boolean portium$breathesInBubbleColumns(EntityLivingBase self, Material material) {
        if (!self.isInsideOfMaterial(material)) return false;
        if (!PortiumConfig.mechanics.gradualAirRefill) return true;
        BlockPos eyes = new BlockPos(self.posX, self.posY + self.getEyeHeight(), self.posZ);
        return this.world.getBlockState(eyes).getBlock() != PortiumBlocks.BUBBLE_COLUMN;
    }

    /** Air comes back four points per tick instead of instantly (1.13). */
    @ModifyConstant(method = "onEntityUpdate", constant = @Constant(intValue = 300))
    private int portium$refillAirGradually(int full) {
        if (!PortiumConfig.mechanics.gradualAirRefill) return full;
        return Math.min(Math.max(this.getAir(), 0) + 4, full);
    }

    /** Conduit Power lets entities breathe underwater. */
    @Redirect(method = "onEntityUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"))
    private boolean portium$conduitBreathing(EntityLivingBase self, Potion potion) {
        return self.isPotionActive(potion) || (potion == MobEffects.WATER_BREATHING && self.isPotionActive(PortiumPotions.CONDUIT_POWER));
    }

    /** Conduit Power speeds up arm swings like Haste. */
    @Inject(method = "getArmSwingAnimationEnd", at = @At("RETURN"), cancellable = true)
    private void portium$conduitSwingSpeed(CallbackInfoReturnable<Integer> cir) {
        if (!this.isPotionActive(MobEffects.HASTE)) {
            PotionEffect conduit = this.getActivePotionEffect(PortiumPotions.CONDUIT_POWER);
            if (conduit != null) cir.setReturnValue(6 - (1 + conduit.getAmplifier()));
        }
    }
}
