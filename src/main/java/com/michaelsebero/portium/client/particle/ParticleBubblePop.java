package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/** The five-frame bubble pop at the water surface (1.13). Frames are stitched into the block atlas. */
public class ParticleBubblePop extends Particle {

    public static final ResourceLocation[] FRAMES = new ResourceLocation[5];

    static {
        for (int i = 0; i < FRAMES.length; i++) FRAMES[i] = new ResourceLocation(Tags.MOD_ID, "particle/bubble_pop_" + i);
    }

    public ParticleBubblePop(World world, double x, double y, double z) {
        super(world, x, y, z);
        this.particleMaxAge = 4;
        this.particleGravity = 0.008F;
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        this.updateFrame();
    }

    private void updateFrame() {
        int frame = Math.min(FRAMES.length - 1, this.particleAge * FRAMES.length / (this.particleMaxAge + 1));
        TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(FRAMES[frame].toString());
        this.setParticleTexture(sprite);
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
        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }
        this.motionY -= this.particleGravity;
        this.move(this.motionX, this.motionY, this.motionZ);
        this.updateFrame();
    }
}
