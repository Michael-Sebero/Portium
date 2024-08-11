package com.michaelsebero.portium.client.particle;

import com.michaelsebero.portium.block.water.WaterUtil;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** A bubble swirling down a whirlpool bubble column (1.13). */
public class ParticleCurrentDown extends Particle {

    private float angle;

    public ParticleCurrentDown(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.setParticleTextureIndex(32);
        this.particleMaxAge = (int) (Math.random() * 60.0) + 30;
        this.canCollide = false;
        this.motionX = 0.0;
        this.motionY = -0.05;
        this.motionZ = 0.0;
        this.setSize(0.01F, 0.01F);
        this.particleGravity = 0.002F;
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
        this.motionX += 0.6F * MathHelper.cos(this.angle);
        this.motionZ += 0.6F * MathHelper.sin(this.angle);
        this.motionX *= 0.07;
        this.motionZ *= 0.07;
        this.move(this.motionX, this.motionY, this.motionZ);
        if (!WaterUtil.isWater(this.world.getBlockState(new BlockPos(this.posX, this.posY, this.posZ)))) this.setExpired();
        this.angle += 0.08F;
    }
}
