package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * 1.14's composter particle: a white glint that hangs where it appears for a few ticks (1.14's SuspendedTownParticle,
 * as its composter factory sets it up). 1.12's suspended particle picks its sprite from the particle sheet, so this
 * repeats its movement with the glint sprite from the block atlas.
 */
public class ParticleComposter extends Particle {

    public static final ResourceLocation SPRITE = new ResourceLocation(Tags.MOD_ID, "particle/glint");

    public ParticleComposter(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
        super(world, x, y, z, xSpeed, ySpeed, zSpeed);
        this.setSize(0.02F, 0.02F);
        this.particleScale *= this.rand.nextFloat() * 0.6F + 0.5F;
        this.motionX *= 0.019999999552965164D;
        this.motionY *= 0.019999999552965164D;
        this.motionZ *= 0.019999999552965164D;
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        this.particleMaxAge = 3 + world.rand.nextInt(5);
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
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleMaxAge-- <= 0) {
            this.setExpired();
            return;
        }
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.99D;
        this.motionY *= 0.99D;
        this.motionZ *= 0.99D;
    }
}
