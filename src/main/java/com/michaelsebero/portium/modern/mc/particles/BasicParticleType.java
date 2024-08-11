package com.michaelsebero.portium.modern.mc.particles;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

/** 1.13's {@code net.minecraft.particles.BasicParticleType}: a particle without arguments. */
public class BasicParticleType extends ParticleType<BasicParticleType> implements IParticleData {

    protected BasicParticleType(ResourceLocation id, boolean alwaysShow) {
        super(id, alwaysShow);
    }

    @Override
    @Srg("func_197554_b")
    public ParticleType<BasicParticleType> getType() {
        return this;
    }

    @Override
    @Srg("func_197553_a")
    public void write(PacketBuffer buffer) {}

    @Override
    @Srg("func_197555_a")
    public String getParameters() {
        return getId().toString();
    }
}
