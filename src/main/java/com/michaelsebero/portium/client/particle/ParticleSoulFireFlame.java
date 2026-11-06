package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * 1.16's soul fire flame: a flame (1.16's {@code FlameParticle}, which moves, shrinks and brightens like 1.12's) drawn
 * with the blue sprite, stitched into the block atlas. 1.12's own flame can't be reused: it picks its sprite from the
 * particle sheet in its constructor.
 */
public class ParticleSoulFireFlame extends Particle {

    public static final ResourceLocation SPRITE = new ResourceLocation(Tags.MOD_ID, "particle/soul_fire_flame");

    private final float flameScale;

    public ParticleSoulFireFlame(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.motionX = this.motionX * 0.009999999776482582D + xSpeed;
        this.motionY = this.motionY * 0.009999999776482582D + ySpeed;
        this.motionZ = this.motionZ * 0.009999999776482582D + zSpeed;
        this.posX += (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
        this.posY += (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
        this.posZ += (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
        this.flameScale = this.particleScale;
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        this.setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(SPRITE.toString()));
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void move(double x, double y, double z) {
        this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
        this.resetPositionToBB();
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX, float rotationZ, float rotationYZ,
                               float rotationXY, float rotationXZ) {
        float age = ((float) this.particleAge + partialTicks) / (float) this.particleMaxAge;
        this.particleScale = this.flameScale * (1.0F - age * age * 0.5F);
        super.renderParticle(buffer, entity, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        float age = MathHelper.clamp(((float) this.particleAge + partialTicks) / (float) this.particleMaxAge, 0.0F, 1.0F);
        int light = super.getBrightnessForRender(partialTicks);
        int blockLight = Math.min(240, (light & 255) + (int) (age * 15.0F * 16.0F));
        return blockLight | (light >> 16 & 255) << 16;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge) this.setExpired();
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9599999785423279D;
        this.motionY *= 0.9599999785423279D;
        this.motionZ *= 0.9599999785423279D;
        if (this.onGround) {
            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
        }
    }
}
