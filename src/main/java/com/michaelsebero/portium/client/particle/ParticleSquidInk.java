package com.michaelsebero.portium.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Squid ink (1.13): a large black puff using the smoke frames. It travels once by its launch speed, then hangs in
 * water or sinks slowly through air, fading over its second half.
 */
public class ParticleSquidInk extends Particle {

    public ParticleSquidInk(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z);
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.particleScale = 5.0F;
        this.setRBGColorF(0.0F, 0.0F, 0.0F);
        this.setAlphaF(1.0F);
        this.particleMaxAge = (int) (this.particleScale * 12.0F / (Math.random() * 0.8F + 0.2F));
        this.canCollide = false;
        this.setParticleTextureIndex(7);
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }
        if (this.particleAge > this.particleMaxAge / 2) this.setAlphaF(1.0F - ((float) this.particleAge - this.particleMaxAge / 2) / this.particleMaxAge);
        this.move(this.motionX, this.motionY, this.motionZ);
        // No air drag: motion stops after each step, then only the sinking below applies.
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.setParticleTextureIndex(7 - this.particleAge * 8 / this.particleMaxAge);
        if (this.world.isAirBlock(new BlockPos(this.posX, this.posY, this.posZ))) this.motionY -= 0.008D;
        this.motionY *= 0.92D;
    }
}
