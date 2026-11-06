package com.michaelsebero.portium.mods116.host;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.village.PointOfInterestType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * 1.16's point-of-interest manager for 1.16 mods (a bear finding beehives, a mob its nest), answered by looking at the
 * world: the blocks in range whose states belong to the point-of-interest types asked for. Claims on points (who works
 * where) aren't kept; villagers' own points are Portium's (PoiManager116). The manager object is 1.16's class, made
 * without its constructor; each of its methods is redirected here.
 */
public final class PoiGlue {

    private static final Map<Object, World> WORLDS = new WeakHashMap<>();
    private static final Map<World, Object> MANAGERS = new WeakHashMap<>();
    private static final Map<String, CachedScan> CACHE = new HashMap<>();
    private static final int MAX_RADIUS = 48;

    private static final class CachedScan {
        final long time;
        final List<BlockPos> found;

        CachedScan(long time, List<BlockPos> found) {
            this.time = time;
            this.found = found;
        }
    }

    /** {@code serverWorld.getPointOfInterestManager()}. */
    public static synchronized PointOfInterestManager manager(ServerWorld world) {
        Object manager = MANAGERS.get(world);
        if (manager == null) {
            try {
                manager = WorldGenGlue.allocate(PointOfInterestManager.class);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
            MANAGERS.put(world, manager);
            WORLDS.put(manager, world);
        }
        return (PointOfInterestManager) manager;
    }

    private static synchronized World world(Object manager) {
        return WORLDS.get(manager);
    }

    /** The block states of every point-of-interest type the predicate takes. */
    @SuppressWarnings("unchecked")
    private static Set<BlockState> states(Predicate<PointOfInterestType> types) {
        Set<BlockState> states = new HashSet<>();
        try {
            Field field = PointOfInterestType.class.getDeclaredField("field_221075_w");
            field.setAccessible(true);
            for (Object type : ForgeRegistries.POI_TYPES.getValues()) {
                if (types.test((PointOfInterestType) type)) states.addAll((Set<BlockState>) field.get(type));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Collections.emptySet();
        }
        return states;
    }

    /** The points in range (a cube, then by distance), nearest first; a scan is kept two seconds. */
    private static List<BlockPos> scan(Object manager, Predicate<PointOfInterestType> types, BlockPos center, int radius) {
        World world = world(manager);
        if (world == null) return Collections.emptyList();
        int range = Math.min(radius, MAX_RADIUS);
        String key = System.identityHashCode(world) + "|" + System.identityHashCode(types) + "|" + (center.func_177958_n() >> 3) + ","
                + (center.func_177956_o() >> 3) + "," + (center.func_177952_p() >> 3) + "|" + range;
        long now = world.func_82737_E();
        synchronized (CACHE) {
            CachedScan cached = CACHE.get(key);
            if (cached != null && now - cached.time < 40) return cached.found;
            if (CACHE.size() > 512) CACHE.clear();
        }
        Set<BlockState> states = states(types);
        List<BlockPos> found = new ArrayList<>();
        if (!states.isEmpty()) {
            BlockPos.Mutable at = new BlockPos.Mutable();
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    int worldX = center.func_177958_n() + x;
                    int worldZ = center.func_177952_p() + z;
                    if (!world.func_195588_v(new BlockPos(worldX, 0, worldZ))) continue;
                    for (int y = -range; y <= range; y++) {
                        int worldY = center.func_177956_o() + y;
                        if (worldY < 0 || worldY > 255) continue;
                        at.func_181079_c(worldX, worldY, worldZ);
                        if (states.contains(world.func_180495_p(at))) found.add(at.func_185334_h());
                    }
                }
            }
            found.removeIf(pos -> pos.func_177951_i(center) > (double) range * range);
            found.sort((a, b) -> Double.compare(a.func_177951_i(center), b.func_177951_i(center)));
        }
        synchronized (CACHE) {
            CACHE.put(key, new CachedScan(now, found));
        }
        return found;
    }

    // The manager's queries.

    public static Stream<BlockPos> findAll(Object manager, Predicate<PointOfInterestType> types, Predicate<BlockPos> positions, BlockPos center, int radius,
                                           Object status) {
        return scan(manager, types, center, radius).stream().filter(positions);
    }

    public static Optional<BlockPos> find(Object manager, Predicate<PointOfInterestType> types, Predicate<BlockPos> positions, BlockPos center, int radius,
                                          Object status) {
        return findAll(manager, types, positions, center, radius, status).findFirst();
    }

    public static Optional<BlockPos> findClosest(Object manager, Predicate<PointOfInterestType> types, BlockPos center, int radius, Object status) {
        return scan(manager, types, center, radius).stream().findFirst();
    }

    public static Optional<BlockPos> take(Object manager, Predicate<PointOfInterestType> types, Predicate<BlockPos> positions, BlockPos center, int radius) {
        return findAll(manager, types, positions, center, radius, null).findFirst();
    }

    public static Optional<BlockPos> random(Object manager, Predicate<PointOfInterestType> types, Predicate<BlockPos> positions, Object status, BlockPos center,
                                            int radius, Random random) {
        List<BlockPos> all = new ArrayList<>();
        findAll(manager, types, positions, center, radius, status).forEach(all::add);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(random.nextInt(all.size())));
    }

    public static long count(Object manager, Predicate<PointOfInterestType> types, BlockPos center, int radius, Object status) {
        return scan(manager, types, center, radius).size();
    }

    /** Queries for 1.16's point-of-interest records (with claims): none kept here. */
    public static <T> Stream<T> records(Object manager, Predicate<PointOfInterestType> types, BlockPos center, int radius, Object status) {
        return Stream.empty();
    }

    public static boolean hasTypeAt(Object manager, BlockPos pos, Predicate<PointOfInterestType> types) {
        World world = world(manager);
        return world != null && states(types).contains(world.func_180495_p(pos));
    }

    public static Optional<PointOfInterestType> typeAt(Object manager, BlockPos pos) {
        World world = world(manager);
        return world == null ? Optional.empty() : PointOfInterestType.func_221047_b(world.func_180495_p(pos));
    }

    /** Claims and releases: not kept (see the class comment). */
    public static boolean release(Object manager, BlockPos pos) {
        return true;
    }

    private PoiGlue() {}
}
