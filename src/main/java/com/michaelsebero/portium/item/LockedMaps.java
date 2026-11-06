package com.michaelsebero.portium.item;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;

/** 1.14's map locking: a copy of the map under a new id, locked, so it stays as it was drawn. */
public final class LockedMaps {

    public static void lock(World world, ItemStack stack) {
        if (world.isRemote) return;
        MapData old = Items.FILLED_MAP.getMapData(stack, world);
        if (old == null) return;
        int id = world.getUniqueDataId("map");
        MapData copy = new MapData("map_" + id);
        copy.xCenter = old.xCenter;
        copy.zCenter = old.zCenter;
        copy.scale = old.scale;
        copy.dimension = old.dimension;
        copy.trackingPosition = old.trackingPosition;
        copy.unlimitedTracking = old.unlimitedTracking;
        System.arraycopy(old.colors, 0, copy.colors, 0, copy.colors.length);
        copy.mapDecorations.putAll(old.mapDecorations);
        ((IMapBanners) copy).portium$banners().putAll(((IMapBanners) old).portium$banners());
        ((ILockableMap) copy).portium$setLocked(true);
        copy.markDirty();
        world.setData(copy.mapName, copy);
        stack.setItemDamage(id);
    }

    public static boolean isLocked(MapData data) {
        return data != null && ((ILockableMap) data).portium$isLocked();
    }

    private LockedMaps() {}
}
