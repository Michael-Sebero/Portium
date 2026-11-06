package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.client.ParticleRendering116;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * What 1.16 mods' particles (and Minecraft 1.16.5's own particle classes they build on) extend in place of 1.16's
 * {@code Particle}: 1.12's particle, which 1.12's particle manager ticks and keeps, drawn by 1.16's {@code render}
 * into a 1.16 vertex builder under its 1.16 render type ({@link ParticleRendering116}). The builder, the camera and
 * the render type are classes of Minecraft 1.16.5's own, taken as objects; a particle's own methods are bridged to
 * these.
 */
@SideOnly(Side.CLIENT)
public abstract class Particle116 extends Particle {

    protected Particle116(WorldClient world, double x, double y, double z) {
        super(world, x, y, z);
    }

    public Particle116(WorldClient world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, motionX, motionY, motionZ);
    }

    /** 1.16's: whether the last move was stopped vertically (by a floor or a ceiling). */
    @Srg("field_228343_B_")
    protected boolean collidedY;

    @Override
    public void move(double x, double y, double z) {
        double before = this.posY;
        super.move(x, y, z);
        this.collidedY = Math.abs(this.posY - before - y) >= 1.0E-5D;
    }

    /** 1.16's: draws the particle into the builder, as the camera sees it. */
    @Srg("func_225606_a_")
    public void render(Object vertexBuilder, Object camera, float partialTicks) {}

    /** 1.16's: how the particle's vertices are drawn (a sheet of sprites, or the mod's own set-up). */
    @Srg("func_217558_b")
    public Object getRenderType() {
        return null;
    }

    @Srg("func_206254_h")
    public int getMaxAge() {
        return this.particleMaxAge;
    }

    @Override
    public final void renderParticle(BufferBuilder buffer, Entity entity, float partialTicks, float rotationX, float rotationZ, float rotationYZ, float rotationXY,
                                     float rotationXZ) {
        ParticleRendering116.render(this, partialTicks);
    }

    /** 1.12's layer for particles that set their own drawing up: 1.16's render types do. */
    @Override
    public final int getFXLayer() {
        return 3;
    }
}
