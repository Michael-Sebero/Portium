package com.michaelsebero.portium.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Spores floating in crimson (pink) and warped (blue) forests: 1.16's UnderwaterParticle as its spore factories make
 * it, tiny specks drifting in a straight line until they fade.
 */
public class ParticleNetherSpore extends Particle {

    private ParticleNetherSpore(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(world, x, y - 0.125D, z, xSpeed, ySpeed, zSpeed);
        this.setSize(0.01F, 0.01F);
        this.particleScale *= this.rand.nextFloat() * 0.6F + 0.6F;
        this.particleMaxAge = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
        this.canCollide = false;
        this.setParticleTextureIndex(0);
    }

    public static ParticleNetherSpore crimson(World world, double x, double y, double z) {
        Random random = world.rand;
        double xSpeed = random.nextGaussian() * 9.999999974752427E-7D;
        double ySpeed = random.nextGaussian() * 9.999999747378752E-5D;
        double zSpeed = random.nextGaussian() * 9.999999974752427E-7D;
        ParticleNetherSpore particle = new ParticleNetherSpore(world, x, y, z, xSpeed, ySpeed, zSpeed);
        particle.setRBGColorF(0.9F, 0.4F, 0.5F);
        return particle;
    }

    public static ParticleNetherSpore warped(World world, double x, double y, double z) {
        double ySpeed = (double) world.rand.nextFloat() * -1.9D * (double) world.rand.nextFloat() * 0.1D;
        ParticleNetherSpore particle = new ParticleNetherSpore(world, x, y, z, 0.0D, ySpeed, 0.0D);
        particle.setRBGColorF(0.1F, 0.1F, 0.3F);
        particle.setSize(0.001F, 0.001F);
        return particle;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleMaxAge-- <= 0) {
            this.setExpired();
            return;
        }
        this.move(this.motionX, this.motionY, this.motionZ);
    }
}
