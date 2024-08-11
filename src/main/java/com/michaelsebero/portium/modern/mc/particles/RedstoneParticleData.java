package com.michaelsebero.portium.modern.mc.particles;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.init.Particles;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;

import java.util.Locale;

/** 1.13's {@code net.minecraft.particles.RedstoneParticleData}: coloured dust. */
public class RedstoneParticleData implements IParticleData {

    @Srg("field_197564_a")
    public static final RedstoneParticleData REDSTONE_DUST = new RedstoneParticleData(1.0F, 0.0F, 0.0F, 1.0F);

    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public RedstoneParticleData(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = MathHelper.clamp(alpha, 0.01F, 4.0F);
    }

    @Override
    @Srg("func_197554_b")
    public ParticleType<RedstoneParticleData> getType() {
        return Particles.DUST;
    }

    @Srg("func_197562_c")
    public float getRed() {
        return this.red;
    }

    @Srg("func_197563_d")
    public float getGreen() {
        return this.green;
    }

    @Srg("func_197561_e")
    public float getBlue() {
        return this.blue;
    }

    @Srg("func_197560_f")
    public float getAlpha() {
        return this.alpha;
    }

    @Override
    @Srg("func_197553_a")
    public void write(PacketBuffer buffer) {
        buffer.writeFloat(this.red);
        buffer.writeFloat(this.green);
        buffer.writeFloat(this.blue);
        buffer.writeFloat(this.alpha);
    }

    @Override
    @Srg("func_197555_a")
    public String getParameters() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", getType().getId(), this.red, this.green, this.blue, this.alpha);
    }
}
