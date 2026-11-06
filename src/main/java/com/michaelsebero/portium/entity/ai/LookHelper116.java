package com.michaelsebero.portium.entity.ai;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14's {@code LookController} for Portium's 1.14+ mobs: like 1.12's, except that a mob may keep its pitch while not
 * looking at anything ({@link #shouldResetPitch}), turning from where its head is rather than from level.
 */
public class LookHelper116 extends EntityLookHelper {

    protected final EntityLiving mob;
    protected float deltaLookYaw;
    protected float deltaLookPitch;
    protected boolean isLooking;
    protected double posX;
    protected double posY;
    protected double posZ;

    public LookHelper116(EntityLiving mob) {
        super(mob);
        this.mob = mob;
    }

    @Override
    public void setLookPositionWithEntity(Entity entity, float deltaYaw, float deltaPitch) {
        double eyes = entity instanceof EntityLivingBase ? entity.posY + entity.getEyeHeight()
                : (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0D;
        this.setLookPosition(entity.posX, eyes, entity.posZ, deltaYaw, deltaPitch);
    }

    @Override
    public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.deltaLookYaw = deltaYaw;
        this.deltaLookPitch = deltaPitch;
        this.isLooking = true;
    }

    @Override
    public void onUpdateLook() {
        if (this.shouldResetPitch()) this.mob.rotationPitch = 0.0F;
        if (this.isLooking) {
            this.isLooking = false;
            this.mob.rotationYawHead = clampedRotate(this.mob.rotationYawHead, this.getTargetYaw(), this.deltaLookYaw);
            this.mob.rotationPitch = clampedRotate(this.mob.rotationPitch, this.getTargetPitch(), this.deltaLookPitch);
        } else {
            this.mob.rotationYawHead = clampedRotate(this.mob.rotationYawHead, this.mob.renderYawOffset, 10.0F);
        }
        if (!this.mob.getNavigator().noPath()) {
            float difference = MathHelper.clamp(MathHelper.wrapDegrees(this.mob.renderYawOffset - this.mob.rotationYawHead),
                    -this.mob.getHorizontalFaceSpeed(), this.mob.getHorizontalFaceSpeed());
            this.mob.rotationYawHead = this.mob.renderYawOffset - difference;
        }
    }

    protected boolean shouldResetPitch() {
        return true;
    }

    @Override
    public boolean getIsLooking() {
        return this.isLooking;
    }

    @Override
    public double getLookPosX() {
        return this.posX;
    }

    @Override
    public double getLookPosY() {
        return this.posY;
    }

    @Override
    public double getLookPosZ() {
        return this.posZ;
    }

    protected float getTargetPitch() {
        double dx = this.posX - this.mob.posX;
        double dy = this.posY - (this.mob.posY + this.mob.getEyeHeight());
        double dz = this.posZ - this.mob.posZ;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        return (float) (-(MathHelper.atan2(dy, horizontal) * (180.0D / Math.PI)));
    }

    protected float getTargetYaw() {
        double dx = this.posX - this.mob.posX;
        double dz = this.posZ - this.mob.posZ;
        return (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
    }

    protected static float clampedRotate(float from, float to, float maxDelta) {
        return from + MathHelper.clamp(MathHelper.wrapDegrees(to - from), -maxDelta, maxDelta);
    }
}
