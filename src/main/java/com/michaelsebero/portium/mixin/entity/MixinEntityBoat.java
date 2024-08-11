package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.api.IRockingBoat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Boats on top of a bubble column rock for three seconds, then get launched (upward column) or dragged under
 * and emptied (whirlpool), as in 1.13.
 */
@Mixin(EntityBoat.class)
public abstract class MixinEntityBoat extends Entity implements IRockingBoat {

    @Unique
    private static final DataParameter<Integer> PORTIUM_ROCKING_TICKS = EntityDataManager.createKey(EntityBoat.class, DataSerializers.VARINT);

    @Unique
    private boolean portium$rocking;
    @Unique
    private boolean portium$downwards;
    @Unique
    private float portium$rockingIntensity;
    @Unique
    private float portium$rockingAngle;
    @Unique
    private float portium$prevRockingAngle;

    private MixinEntityBoat(World world) {
        super(world);
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerData(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_ROCKING_TICKS, 0);
    }

    @Override
    public void portium$onBubbleColumnSurface(boolean drag) {
        if (!this.world.isRemote) {
            this.portium$rocking = true;
            this.portium$downwards = drag;
            if (this.dataManager.get(PORTIUM_ROCKING_TICKS) == 0) this.dataManager.set(PORTIUM_ROCKING_TICKS, 60);
        }
        this.world.spawnParticle(EnumParticleTypes.WATER_SPLASH, this.posX + this.rand.nextFloat(), this.posY + 0.7D, this.posZ + this.rand.nextFloat(), 0.0D, 0.0D, 0.0D);
        if (this.rand.nextInt(20) == 0) {
            this.world.playSound(this.posX, this.posY, this.posZ, this.getSplashSound(), this.getSoundCategory(), 1.0F, 0.8F + 0.4F * this.rand.nextFloat(), false);
        }
    }

    @Override
    public float portium$getRockingAngle(float partialTicks) {
        return this.portium$prevRockingAngle + (this.portium$rockingAngle - this.portium$prevRockingAngle) * partialTicks;
    }

    @Inject(method = "onUpdate", at = @At("TAIL"))
    private void portium$updateRocking(CallbackInfo ci) {
        if (this.world.isRemote) {
            int ticks = this.dataManager.get(PORTIUM_ROCKING_TICKS);
            this.portium$rockingIntensity = MathHelper.clamp(this.portium$rockingIntensity + (ticks > 0 ? 0.05F : -0.1F), 0.0F, 1.0F);
            this.portium$prevRockingAngle = this.portium$rockingAngle;
            this.portium$rockingAngle = 10.0F * (float) Math.sin(0.5F * this.world.getTotalWorldTime()) * this.portium$rockingIntensity;
            return;
        }
        if (!this.portium$rocking) this.dataManager.set(PORTIUM_ROCKING_TICKS, 0);
        int ticks = this.dataManager.get(PORTIUM_ROCKING_TICKS);
        if (ticks > 0) {
            --ticks;
            this.dataManager.set(PORTIUM_ROCKING_TICKS, ticks);
            if (ticks == 0) {
                if (this.portium$downwards) {
                    this.motionY -= 0.7D;
                    this.removePassengers();
                } else {
                    this.motionY = this.portium$isPassengerOfType(EntityPlayer.class) ? 2.7D : 0.6D;
                }
            }
            this.portium$rocking = false;
        }
    }

    @Unique
    private boolean portium$isPassengerOfType(Class<? extends Entity> type) {
        for (Entity passenger : this.getPassengers()) {
            if (type.isInstance(passenger)) return true;
        }
        return false;
    }
}
