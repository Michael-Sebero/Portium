package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * 1.14's campfire smoke ({@code CampfireParticle}): a big puff that drifts up slowly and fades over its last three
 * seconds. Signal fires (on hay) send it up for about fifteen seconds, other campfires for about four.
 */
public class ParticleCampfireSmoke extends Particle {

    public static final ResourceLocation[] SPRITES = new ResourceLocation[12];

    static {
        for (int i = 0; i < SPRITES.length; i++) SPRITES[i] = new ResourceLocation(Tags.MOD_ID, "particle/big_smoke_" + i);
    }

    public ParticleCampfireSmoke(World world, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, boolean signal) {
        super(world, x, y, z);
        this.multipleParticleScaleBy(3.0F);
        this.setSize(0.25F, 0.25F);
        this.particleMaxAge = signal ? this.rand.nextInt(50) + 280 : this.rand.nextInt(50) + 80;
        this.particleGravity = 3.0E-6F;
        this.motionX = xSpeed;
        this.motionY = ySpeed + (double) (this.rand.nextFloat() / 500.0F);
        this.motionZ = zSpeed;
        this.setAlphaF(signal ? 0.95F : 0.9F);
        ResourceLocation sprite = SPRITES[this.rand.nextInt(SPRITES.length)];
        this.setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(sprite.toString()));
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ < this.particleMaxAge && this.particleAlpha > 0.0F) {
            this.motionX += (double) (this.rand.nextFloat() / 5000.0F * (float) (this.rand.nextBoolean() ? 1 : -1));
            this.motionZ += (double) (this.rand.nextFloat() / 5000.0F * (float) (this.rand.nextBoolean() ? 1 : -1));
            this.motionY -= (double) this.particleGravity;
            this.move(this.motionX, this.motionY, this.motionZ);
            if (this.particleAge >= this.particleMaxAge - 60 && this.particleAlpha > 0.01F) this.particleAlpha -= 0.015F;
        } else {
            this.setExpired();
        }
    }
}
