package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** A bubble rising through an upward bubble column (1.13), popping when it reaches the surface. */
public class ParticleBubbleColumnUp extends Particle {

    public ParticleBubbleColumnUp(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        this.setParticleTextureIndex(32);
        this.setSize(0.02F, 0.02F);
        this.particleScale *= this.rand.nextFloat() * 0.6F + 0.2F;
        this.motionX = vx * 0.2 + (Math.random() * 2.0 - 1.0) * 0.02;
        this.motionY = vy * 0.2 + (Math.random() * 2.0 - 1.0) * 0.02;
        this.motionZ = vz * 0.2 + (Math.random() * 2.0 - 1.0) * 0.02;
        this.particleMaxAge = (int) (40.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY += 0.005;
        if (this.particleMaxAge-- <= 0) {
            this.setExpired();
            return;
        }
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.85;
        this.motionY *= 0.85;
        this.motionZ *= 0.85;
        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        if (!WaterUtil.isWater(this.world.getBlockState(pos))) {
            this.setExpired();
            if (this.world.isAirBlock(pos)) {
                Minecraft.getMinecraft().effectRenderer.addEffect(new ParticleBubblePop(this.world, this.posX, this.posY, this.posZ));
                if (this.rand.nextInt(4) == 0) {
                    this.world.playSound(this.posX, this.posY, this.posZ, PortiumSounds.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.BLOCKS, 0.1F + this.rand.nextFloat() * 0.1F, 0.9F + this.rand.nextFloat() * 0.15F, false);
                }
            }
        }
    }
}
