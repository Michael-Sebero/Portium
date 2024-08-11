package com.michaelsebero.portium.init;

import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.util.text.TextComponentTranslation;

/** Statistics added by 1.13. */
public final class PortiumStats {

    /** Ticks since the player last slept (or died), which drives phantom spawning. Shown in the statistics screen. */
    public static final StatBase TIME_SINCE_REST = new StatBasic("stat.timeSinceRest", new TextComponentTranslation("stat.timeSinceRest"),
            StatBase.timeStatType).registerStat();

    /** Forces the class to load, registering the statistics. */
    public static void init() {}

    private PortiumStats() {}
}
