package com.michaelsebero.portium.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.14's "is this a village" questions: in 1.16.5 worlds, chunk sections to the nearest section with a point of
 * interest a villager holds (a bed, workstation or bell), as 1.14 counts; and 1.12's village collection too.
 */
public final class Villages116 {

    /** 1.14's {@code doesVillageHaveAllSections(pos, sections)}: within that many sections of a village. */
    public static boolean isNearVillage(World world, BlockPos pos, int sections) {
        if (world instanceof net.minecraft.world.WorldServer && com.michaelsebero.portium.entity.villager.VillagerEvents116.isActive(world)
                && com.michaelsebero.portium.world.poi.PoiManager116.get((net.minecraft.world.WorldServer) world).sectionsToVillage(pos, 7) <= sections) {
            return true;
        }
        return sections <= 6 && world.getVillageCollection() != null && world.getVillageCollection().getNearestVillage(pos, sections * 16) != null;
    }

    /** 1.14's {@code isVillage}: within one section of a village. */
    public static boolean isVillage(World world, BlockPos pos) {
        return isNearVillage(world, pos, 1);
    }

    /** 1.14's meeting point (the bell) within a distance: 1.12's village centre, the nearest village's. */
    @javax.annotation.Nullable
    public static BlockPos meetingPoint(World world, BlockPos pos, int distance) {
        if (world.getVillageCollection() == null) return null;
        net.minecraft.village.Village village = world.getVillageCollection().getNearestVillage(pos, distance);
        if (village == null || village.getCenter().distanceSq(pos) > (double) distance * distance) return null;
        return village.getCenter();
    }

    private Villages116() {}
}
