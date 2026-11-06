package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/** 1.16's soul particle: a decelerating wisp, half again as big, animated through eleven frames over its life (soul speed, soul fire). */
public class ParticleSoul extends Particle {

    public static final ResourceLocation[] SPRITES = new ResourceLocation[11];

    static {
        for (int i = 0; i < SPRITES.length; ++i) SPRITES[i] = new ResourceLocation(Tags.MOD_ID, "particle/soul_" + i);
    }

    public ParticleSoul(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.motionX = this.motionX * 0.009999999776482582D + xSpeed;
        this.motionY = this.motionY * 0.009999999776482582D + ySpeed;
        this.motionZ = this.motionZ * 0.009999999776482582D + zSpeed;
        this.posX += (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
        this.posY += (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
        this.posZ += (double) ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.05F);
        this.particleMaxAge = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        this.particleScale *= 1.5F;
        this.particleAlpha = 1.0F;
        this.selectSprite();
    }

    private void selectSprite() {
        int frame = this.particleAge * (SPRITES.length - 1) / Math.max(this.particleMaxAge, 1);
        this.setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(SPRITES[Math.min(frame, SPRITES.length - 1)].toString()));
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public boolean shouldDisableDepth() {
        return true;
    }

    @Override
    public void move(double x, double y, double z) {
        this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
        this.resetPositionToBB();
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
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.9599999785423279D;
        this.motionY *= 0.9599999785423279D;
        this.motionZ *= 0.9599999785423279D;
        if (this.onGround) {
            this.motionX *= 0.699999988079071D;
            this.motionZ *= 0.699999988079071D;
        }
        this.selectSprite();
    }
}
