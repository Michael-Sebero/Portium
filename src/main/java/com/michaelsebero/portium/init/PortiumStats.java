package com.michaelsebero.portium.init;

import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.util.text.TextComponentTranslation;

/** Statistics added by 1.13. */
public final class PortiumStats {

    /** Ticks since the player last slept (or died), which drives phantom spawning. Shown in the statistics screen. */
    public static final StatBase TIME_SINCE_REST = new StatBasic("stat.timeSinceRest", new TextComponentTranslation("stat.timeSinceRest"),
            StatBase.timeStatType).registerStat();

    /** Food put on a campfire (1.14). */
    public static final StatBase INTERACT_WITH_CAMPFIRE = new StatBasic("stat.interactWithCampfire", new TextComponentTranslation("stat.interactWithCampfire"))
            .registerStat();

    /** Projectiles that hit a target block (1.16). */
    public static final StatBase TARGET_HIT = new StatBasic("stat.targetHit", new TextComponentTranslation("stat.targetHit")).registerStat();

    /** Bells rung by hand or by a projectile (1.14). */
    public static final StatBase BELL_RING = new StatBasic("stat.bellRing", new TextComponentTranslation("stat.bellRing")).registerStat();

    public static final StatBase INTERACT_WITH_LECTERN = new StatBasic("stat.interactWithLectern", new TextComponentTranslation("stat.interactWithLectern"))
            .registerStat();

    public static final StatBase OPEN_BARREL = new StatBasic("stat.openBarrel", new TextComponentTranslation("stat.openBarrel")).registerStat();
    public static final StatBase INTERACT_WITH_SMOKER = new StatBasic("stat.interactWithSmoker", new TextComponentTranslation("stat.interactWithSmoker"))
            .registerStat();
    public static final StatBase INTERACT_WITH_BLAST_FURNACE = new StatBasic("stat.interactWithBlastFurnace",
            new TextComponentTranslation("stat.interactWithBlastFurnace")).registerStat();

    public static final StatBase INTERACT_WITH_GRINDSTONE = new StatBasic("stat.interactWithGrindstone",
            new TextComponentTranslation("stat.interactWithGrindstone")).registerStat();
    public static final StatBase INTERACT_WITH_STONECUTTER = new StatBasic("stat.interactWithStonecutter",
            new TextComponentTranslation("stat.interactWithStonecutter")).registerStat();

    public static final StatBase INTERACT_WITH_LOOM = new StatBasic("stat.interactWithLoom", new TextComponentTranslation("stat.interactWithLoom"))
            .registerStat();
    public static final StatBase INTERACT_WITH_CARTOGRAPHY_TABLE = new StatBasic("stat.interactWithCartographyTable",
            new TextComponentTranslation("stat.interactWithCartographyTable")).registerStat();

    /** Forces the class to load, registering the statistics. */
    public static void init() {}

    private PortiumStats() {}
}
