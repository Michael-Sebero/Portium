package com.michaelsebero.portium.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

/**
 * 1.15's honey drips under full hives (they hang a moment, then fall, honey-coloured) and the nectar that falls slowly
 * from pollen-laden bees. Drawn with 1.12's drip sprites (hanging, falling).
 */
public class ParticleHoneyDrip extends Particle {

    private int hangTime;

    public ParticleHoneyDrip(World world, double x, double y, double z, boolean nectar) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.setSize(0.01F, 0.01F);
        if (nectar) {
            this.particleRed = 0.92F;
            this.particleGreen = 0.782F;
            this.particleBlue = 0.72F;
            this.particleGravity = 0.007F;
            this.particleMaxAge = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
            this.hangTime = 0;
        } else {
            this.particleRed = 0.622F;
            this.particleGreen = 0.508F;
            this.particleBlue = 0.082F;
            this.particleGravity = 0.06F;
            this.particleMaxAge = (int) (64.0D / (Math.random() * 0.8D + 0.2D));
            this.hangTime = 40;
        }
        this.setParticleTextureIndex(this.hangTime > 0 ? 113 : 112);
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY -= this.particleGravity;
        if (this.hangTime-- > 0) {
            this.motionX *= 0.02D;
            this.motionY *= 0.02D;
            this.motionZ *= 0.02D;
            this.setParticleTextureIndex(113);
        } else {
            this.setParticleTextureIndex(112);
        }
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.98D;
        this.motionY *= 0.98D;
        this.motionZ *= 0.98D;
        if (this.particleMaxAge-- <= 0 || this.onGround) this.setExpired();
    }
}
