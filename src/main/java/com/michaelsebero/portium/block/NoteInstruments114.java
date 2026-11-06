package com.michaelsebero.portium.block;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.SoundEvent;

import javax.annotation.Nullable;

/**
 * 1.14's six new note block instruments, chosen by the block under the note block before 1.12's material rules: iron
 * xylophone (iron block), cow bell (soul sand), didgeridoo (pumpkin), bit (emerald block), banjo (hay bale) and pling
 * (glowstone). They travel as block event ids after 1.12's ten instruments.
 */
public final class NoteInstruments114 {

    /** 1.12's instruments are block event ids 0 to 9; these follow. */
    public static final int FIRST = 10;
    private static final SoundEvent[] SOUNDS = {PortiumSounds.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, PortiumSounds.BLOCK_NOTE_BLOCK_COW_BELL,
            PortiumSounds.BLOCK_NOTE_BLOCK_DIDGERIDOO, PortiumSounds.BLOCK_NOTE_BLOCK_BIT, PortiumSounds.BLOCK_NOTE_BLOCK_BANJO,
            PortiumSounds.BLOCK_NOTE_BLOCK_PLING};

    /** The block event id for a 1.14 instrument played over {@code below}, or -1 for 1.12's own rules. */
    public static int idFor(Block below) {
        if (below == Blocks.IRON_BLOCK) return FIRST;
        if (below == Blocks.SOUL_SAND) return FIRST + 1;
        if (below == (PortiumBlocks.PUMPKIN != null ? PortiumBlocks.PUMPKIN : Blocks.PUMPKIN)) return FIRST + 2;
        if (below == Blocks.EMERALD_BLOCK) return FIRST + 3;
        if (below == Blocks.HAY_BLOCK) return FIRST + 4;
        if (below == Blocks.GLOWSTONE) return FIRST + 5;
        return -1;
    }

    @Nullable
    public static SoundEvent sound(int id) {
        return id >= FIRST && id < FIRST + SOUNDS.length ? SOUNDS[id - FIRST] : null;
    }

    private NoteInstruments114() {}
}
