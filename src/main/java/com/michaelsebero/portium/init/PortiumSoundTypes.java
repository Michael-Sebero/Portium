package com.michaelsebero.portium.init;

import net.minecraft.block.SoundType;

public final class PortiumSoundTypes {

    public static final SoundType CORAL = new SoundType(1.0F, 1.0F, PortiumSounds.BLOCK_CORAL_BLOCK_BREAK, PortiumSounds.BLOCK_CORAL_BLOCK_STEP,
            PortiumSounds.BLOCK_CORAL_BLOCK_PLACE, PortiumSounds.BLOCK_CORAL_BLOCK_HIT, PortiumSounds.BLOCK_CORAL_BLOCK_FALL);

    public static final SoundType WET_GRASS = new SoundType(1.0F, 1.0F, PortiumSounds.BLOCK_WET_GRASS_BREAK, PortiumSounds.BLOCK_WET_GRASS_STEP,
            PortiumSounds.BLOCK_WET_GRASS_PLACE, PortiumSounds.BLOCK_WET_GRASS_HIT, PortiumSounds.BLOCK_WET_GRASS_FALL);

    // 1.16's, whose events come whole from 1.16.5's sounds.json (see tools/genres.py).
    public static final SoundType NYLIUM = v116("nylium");
    public static final SoundType HYPHAE = v116("stem");
    public static final SoundType WART = v116("wart_block");
    public static final SoundType SHROOMLIGHT = v116("shroomlight");
    public static final SoundType FUNGUS = v116("fungus");
    public static final SoundType ROOT = v116("roots");
    public static final SoundType NETHER_VINE = v116("weeping_vines");
    public static final SoundType NETHER_SPROUT = v116("nether_sprouts");
    public static final SoundType SOUL_SOIL = v116("soul_soil");
    public static final SoundType SOUL_SAND = v116("soul_sand");
    public static final SoundType NETHERRACK = v116("netherrack");
    public static final SoundType BASALT = v116("basalt");
    public static final SoundType ANCIENT_DEBRIS = v116("ancient_debris");
    public static final SoundType NETHER_GOLD = v116("nether_gold_ore");
    public static final SoundType GILDED_BLACKSTONE = v116("gilded_blackstone");
    public static final SoundType NETHER_ORE = v116("nether_ore");

    /** A sound type whose five events are {@code block.<name>.break/step/place/hit/fall}, as 1.16 names them. */
    private static SoundType v116(String name) {
        String prefix = "block." + name + ".";
        return new SoundType(1.0F, 1.0F, PortiumSounds.create(prefix + "break"), PortiumSounds.create(prefix + "step"),
                PortiumSounds.create(prefix + "place"), PortiumSounds.create(prefix + "hit"), PortiumSounds.create(prefix + "fall"));
    }

    /** Loads the class, making its sound events. */
    static void init() {}

    private PortiumSoundTypes() {}
}
