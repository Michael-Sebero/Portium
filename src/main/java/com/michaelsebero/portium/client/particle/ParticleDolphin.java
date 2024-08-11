package com.michaelsebero.portium.client.particle;

import net.minecraft.client.particle.ParticleSuspendedTown;
import net.minecraft.world.World;

/** The blue speck trail dolphins leave in the water (1.13). */
public class ParticleDolphin extends ParticleSuspendedTown {

    public ParticleDolphin(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.setRBGColorF(0.3F, 0.5F, 1.0F);
        this.setAlphaF(1.0F - world.rand.nextFloat() * 0.7F);
        this.setMaxAge(this.particleMaxAge / 2);
    }
}
