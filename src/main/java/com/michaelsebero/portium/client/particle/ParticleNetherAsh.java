package com.michaelsebero.portium.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Ash drifting down in soul sand valleys (dark grey) and basalt deltas (white ash, pale lilac): 1.16's AshParticle and
 * WhiteAshParticle, rising particles that sink, grow in over their first moments and speed up sideways once they
 * stop falling.
 */
public class ParticleNetherAsh extends Particle {

    private final double fall;

    private ParticleNetherAsh(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, float shade, double fall) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.fall = fall;
        this.motionX *= 0.1D;
        this.motionY *= -0.1D;
        this.motionZ *= 0.1D;
        this.motionX += xSpeed;
        this.motionY += ySpeed;
        this.motionZ += zSpeed;
        float color = world.rand.nextFloat() * shade;
        this.particleRed = color;
        this.particleGreen = color;
        this.particleBlue = color;
        this.particleScale *= 0.75F;
        this.particleMaxAge = (int) (20.0D / ((double) world.rand.nextFloat() * 0.8D + 0.2D));
        this.particleMaxAge = Math.max(this.particleMaxAge, 1);
        this.setParticleTextureIndex(0);
        this.canCollide = false;
    }

    /** Soul sand valley ash. */
    public static ParticleNetherAsh ash(World world, double x, double y, double z) {
        return new ParticleNetherAsh(world, x, y, z, 0.0D, 0.0D, 0.0D, 0.5F, -0.004D);
    }

    /** Basalt delta ash, blown down and sideways. */
    public static ParticleNetherAsh whiteAsh(World world, double x, double y, double z) {
        Random random = world.rand;
        double xSpeed = (double) random.nextFloat() * -1.9D * (double) random.nextFloat() * 0.1D;
        double ySpeed = (double) random.nextFloat() * -0.5D * (double) random.nextFloat() * 0.1D * 5.0D;
        double zSpeed = (double) random.nextFloat() * -1.9D * (double) random.nextFloat() * 0.1D;
        ParticleNetherAsh particle = new ParticleNetherAsh(world, x, y, z, xSpeed, ySpeed, zSpeed, 0.0F, -5.0E-4D);
        particle.setRBGColorF(0.7294118F, 0.69411767F, 0.7607843F);
        return particle;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX, float rotationZ, float rotationYZ,
                               float rotationXY, float rotationXZ) {
        float scale = this.particleScale;
        this.particleScale = scale * MathHelper.clamp(((float) this.particleAge + partialTicks) / (float) this.particleMaxAge * 32.0F, 0.0F, 1.0F);
        super.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
        this.particleScale = scale;
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
        this.motionY += this.fall;
        this.move(this.motionX, this.motionY, this.motionZ);
        if (this.posY == this.prevPosY) {
            this.motionX *= 1.1D;
            this.motionZ *= 1.1D;
        }
        this.motionX *= 0.96D;
        this.motionY *= 0.96D;
        this.motionZ *= 0.96D;
        if (this.onGround) {
            this.motionX *= 0.7D;
            this.motionZ *= 0.7D;
        }
    }
}
