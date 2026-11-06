package com.michaelsebero.portium.entity.passive.bee;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.MathHelper;

/**
 * 1.15's {@code FlyingMovementController}: 1.12's fly helper with a pitch turn limit and, for hovering fliers (bees),
 * no gravity even when not moving.
 */
public class FlyHelper116 extends EntityMoveHelper {

    private final int maxPitchChange;
    private final boolean hovers;

    public FlyHelper116(EntityLiving entity, int maxPitchChange, boolean hovers) {
        super(entity);
        this.maxPitchChange = maxPitchChange;
        this.hovers = hovers;
    }

    @Override
    public void onUpdateMoveHelper() {
        if (this.action == Action.MOVE_TO) {
            this.action = Action.WAIT;
            this.entity.setNoGravity(true);
            double dx = this.posX - this.entity.posX;
            double dy = this.posY - this.entity.posY;
            double dz = this.posZ - this.entity.posZ;
            if (dx * dx + dy * dy + dz * dz < 2.500000277905201E-7D) {
                this.entity.setMoveVertical(0.0F);
                this.entity.setMoveForward(0.0F);
                return;
            }
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
            this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, yaw, 90.0F);
            float speed = (float) (this.speed * (this.entity.onGround
                    ? this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue()
                    : this.entity.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED).getAttributeValue()));
            this.entity.setAIMoveSpeed(speed);
            double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
            float pitch = (float) (-(MathHelper.atan2(dy, horizontal) * (180D / Math.PI)));
            this.entity.rotationPitch = this.limitAngle(this.entity.rotationPitch, pitch, (float) this.maxPitchChange);
            this.entity.setMoveVertical(dy > 0.0D ? speed : -speed);
        } else {
            if (!this.hovers) this.entity.setNoGravity(false);
            this.entity.setMoveVertical(0.0F);
            this.entity.setMoveForward(0.0F);
        }
    }
}
