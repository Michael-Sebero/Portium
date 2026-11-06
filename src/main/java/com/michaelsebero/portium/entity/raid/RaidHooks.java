package com.michaelsebero.portium.entity.raid;

import net.minecraft.world.World;

/** Where raider code asks about raids by id: 1.14's raids in 1.16.5 worlds ({@link RaidManager116}). */
public final class RaidHooks {

    public static boolean exists(World world, int raidId) {
        RaidManager116 manager = RaidManager116.get(world);
        return manager != null && manager.getRaid(raidId) != null;
    }

    public static boolean isActive(World world, int raidId) {
        RaidManager116 manager = RaidManager116.get(world);
        Raid116 raid = manager == null ? null : manager.getRaid(raidId);
        return raid != null && raid.isActive() && !raid.isOver() && !raid.isStopped();
    }

    private RaidHooks() {}
}
