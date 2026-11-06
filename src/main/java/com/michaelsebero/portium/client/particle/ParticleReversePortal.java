package com.michaelsebero.portium.client.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * 1.16's reverse portal particle, rising from charged respawn anchors (and crying obsidian's tears' kin): a portal
 * speck half again as big, that speeds up along its motion over three seconds instead of falling into a portal.
 */
public class ParticleReversePortal extends Particle {

    private final float baseScale;

    public ParticleReversePortal(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        // 1.16's PortalParticle, as 1.12's ParticlePortal sets it up.
        this.motionX = xSpeed;
        this.motionY = ySpeed;
        this.motionZ = zSpeed;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        float shade = this.rand.nextFloat() * 0.6F + 0.4F;
        this.particleScale = (this.rand.nextFloat() * 0.2F + 0.5F) * 1.5F;
        this.baseScale = this.particleScale;
        this.particleRed = shade * 0.9F;
        this.particleGreen = shade * 0.3F;
        this.particleBlue = shade;
        this.particleMaxAge = (int) (Math.random() * 2.0D) + 60;
        this.setParticleTextureIndex((int) (Math.random() * 8.0D));
    }

    @Override
    public void move(double x, double y, double z) {
        this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
        this.resetPositionToBB();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX, float rotationZ, float rotationYZ,
                               float rotationXY, float rotationXZ) {
        this.particleScale = this.baseScale * (1.0F - ((float) this.particleAge + partialTicks) / ((float) this.particleMaxAge * 1.5F));
        super.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        int light = super.getBrightnessForRender(partialTicks);
        float age = (float) this.particleAge / (float) this.particleMaxAge;
        age *= age;
        age *= age;
        int sky = Math.min(240, (light >> 16 & 255) + (int) (age * 15.0F * 16.0F));
        return (light & 255) | sky << 16;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        } else {
            float progress = (float) this.particleAge / (float) this.particleMaxAge;
            this.posX += this.motionX * (double) progress;
            this.posY += this.motionY * (double) progress;
            this.posZ += this.motionZ * (double) progress;
        }
    }
}
