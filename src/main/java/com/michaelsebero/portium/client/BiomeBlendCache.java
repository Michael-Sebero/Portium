package com.michaelsebero.portium.client;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Blended biome colours, remembered per block column while a chunk section is built. A blend only depends on a
 * column's x and z, but 1.12 asks once for every tinted face of every block, which at 15x15 means 225 biome lookups
 * each time. Only chunk building is cached: it reads a fixed snapshot of the world, and each build uses a new one.
 */
@SideOnly(Side.CLIENT)
public final class BiomeBlendCache {

    public static final int MISS = -1;

    private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);

    /** The remembered colour for a column, or {@link #MISS}. {@code resolver} is grass, foliage or water. */
    public static int get(IBlockAccess world, BlockPos pos, Object resolver) {
        if (!(world instanceof ChunkCache)) return MISS;
        return CACHE.get().get(world, resolver).get(key(pos));
    }

    public static void put(IBlockAccess world, BlockPos pos, Object resolver, int color) {
        if (!(world instanceof ChunkCache)) return;
        CACHE.get().get(world, resolver).put(key(pos), color);
    }

    private static long key(BlockPos pos) {
        return ((long) pos.getX() << 32) | (pos.getZ() & 0xFFFFFFFFL);
    }

    private static final class Cache {

        private WeakReference<IBlockAccess> world = new WeakReference<>(null);
        private int radius = -1;
        private final Map<Object, Long2IntOpenHashMap> colors = new IdentityHashMap<>(4);

        Long2IntOpenHashMap get(IBlockAccess world, Object resolver) {
            int radius = BiomeBlend.effectiveRadius();
            if (this.world.get() != world || this.radius != radius) {
                this.world = new WeakReference<>(world);
                this.radius = radius;
                this.colors.clear();
            }
            return this.colors.computeIfAbsent(resolver, key -> {
                Long2IntOpenHashMap map = new Long2IntOpenHashMap(512);
                map.defaultReturnValue(MISS);
                return map;
            });
        }
    }

    private BiomeBlendCache() {}
}
