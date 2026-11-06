package com.michaelsebero.portium.item;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

/** The banners a map marks (1.13), on {@link net.minecraft.world.storage.MapData}. */
public interface IMapBanners {

    /** Marks the banner at {@code pos}, or unmarks it if it's marked; nothing if there's no banner or it's off the map. */
    void portium$toggleBanner(World world, BlockPos pos);

    /** Unmarks banners that have gone or changed, where the world is loaded. */
    void portium$removeStaleBanners(World world);

    Map<String, MapBanners116.Banner> portium$banners();
}
