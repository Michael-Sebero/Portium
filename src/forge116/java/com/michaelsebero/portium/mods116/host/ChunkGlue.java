package com.michaelsebero.portium.mods116.host;

import net.minecraft.world.chunk.ChunkStatus;

import java.util.EnumSet;

/** 1.16's chunk statuses, onto Portium's (1.12 generates chunks whole, so there are no steps to add). */
public final class ChunkGlue {

    /**
     * {@code new ChunkStatus(...)}: a mod's own step, which 1.12 has nowhere to put; it's the full status. (The step's
     * workers are typed loosely: their interfaces aren't public in the Minecraft 1.16 this compiles against.)
     */
    public static ChunkStatus status(String name, ChunkStatus parent, int range, EnumSet<?> heightmaps, ChunkStatus.Type type,
                                     Object generation, Object loading) {
        return ChunkStatus.field_222617_m;
    }

    private ChunkGlue() {}
}
