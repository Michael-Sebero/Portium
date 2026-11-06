package com.michaelsebero.portium.mods116.host;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Direction;

/** Codecs 1.16 keeps on Minecraft's classes that 1.12's same classes lack. */
public final class CodecGlue {

    private static final Codec<Direction.Axis> AXIS = Codec.STRING.comapFlatMap(name -> {
        Direction.Axis axis = Direction.Axis.func_176717_a(name);
        return axis == null ? DataResult.error("Unknown axis " + name) : DataResult.success(axis);
    }, Direction.Axis::func_176719_a);

    /** {@code Direction.Axis.CODEC}. */
    public static Codec<Direction.Axis> axis() {
        return AXIS;
    }

    private CodecGlue() {}
}
