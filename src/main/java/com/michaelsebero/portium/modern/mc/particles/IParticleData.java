package com.michaelsebero.portium.modern.mc.particles;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.network.PacketBuffer;

/** 1.13's {@code net.minecraft.particles.IParticleData}: a particle type with its arguments. */
public interface IParticleData {

    @Srg("func_197554_b")
    ParticleType<?> getType();

    @Srg("func_197553_a")
    void write(PacketBuffer buffer);

    /** The particle as {@code /particle} would take it. */
    @Srg("func_197555_a")
    String getParameters();
}
