package com.michaelsebero.portium.world.poi;

import com.michaelsebero.portium.Tags;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * 1.14's point-of-interest manager for one world: where the workstations, bed heads and bells are, and which of them
 * villagers have taken. Points are found by scanning a chunk section the first time anything asks about it, and kept
 * up to date as blocks change (MixinChunkPoi); scans are dropped when chunks unload. Who holds which point is saved
 * with the world, so unloaded villagers keep their beds and job sites.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class PoiManager116 {

    private static final Map<World, PoiManager116> MANAGERS = new WeakHashMap<>();

    private final WorldServer world;
    /** Scanned sections, by packed section position; each maps its points to their type. */
    private final Map<Long, Map<BlockPos, PoiType116>> sections = new HashMap<>();
    private final Claims claims;

    private PoiManager116(WorldServer world) {
        this.world = world;
        Claims loaded = (Claims) world.getPerWorldStorage().getOrLoadData(Claims.class, Claims.NAME);
        if (loaded == null) {
            loaded = new Claims(Claims.NAME);
            world.getPerWorldStorage().setData(Claims.NAME, loaded);
        }
        this.claims = loaded;
    }

    public static PoiManager116 get(WorldServer world) {
        return MANAGERS.computeIfAbsent(world, w -> new PoiManager116((WorldServer) w));
    }

    @Nullable
    private static PoiManager116 existing(World world) {
        return MANAGERS.get(world);
    }

    // ---- Queries ----

    /** The type of point at a position, if there is one (scanning its section if needed). */
    @Nullable
    public PoiType116 getType(BlockPos pos) {
        Map<BlockPos, PoiType116> section = section(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        return section == null ? null : section.get(pos);
    }

    public boolean exists(BlockPos pos, Predicate<PoiType116> type) {
        PoiType116 found = getType(pos);
        return found != null && type.test(found);
    }

    /** Points of a type within a distance of a position (sphere), nearest first; only free ones if asked. */
    public List<BlockPos> find(Predicate<PoiType116> type, BlockPos origin, int distance, boolean freeOnly) {
        List<BlockPos> found = new ArrayList<>();
        double max = (double) distance * distance;
        int minX = (origin.getX() - distance) >> 4, maxX = (origin.getX() + distance) >> 4;
        int minY = Math.max(0, (origin.getY() - distance) >> 4), maxY = Math.min(15, (origin.getY() + distance) >> 4);
        int minZ = (origin.getZ() - distance) >> 4, maxZ = (origin.getZ() + distance) >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                for (int cy = minY; cy <= maxY; cy++) {
                    Map<BlockPos, PoiType116> section = section(cx, cy, cz);
                    if (section == null || section.isEmpty()) continue;
                    for (Map.Entry<BlockPos, PoiType116> entry : section.entrySet()) {
                        if (!type.test(entry.getValue()) || entry.getKey().distanceSq(origin) > max) continue;
                        if (freeOnly && !hasSpace(entry.getKey(), entry.getValue())) continue;
                        found.add(entry.getKey());
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distanceSq(origin)));
        return found;
    }

    public Optional<BlockPos> findNearest(Predicate<PoiType116> type, BlockPos origin, int distance, boolean freeOnly) {
        List<BlockPos> found = find(type, origin, distance, freeOnly);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    /** Takes the nearest free point that also passes a test, for a holder; the point, or empty. */
    public Optional<BlockPos> take(Predicate<PoiType116> type, Predicate<BlockPos> test, BlockPos origin, int distance, UUID holder) {
        for (BlockPos pos : find(type, origin, distance, true)) {
            if (test.test(pos)) {
                claim(pos, holder);
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    /** Whether a point can take another holder. */
    public boolean hasSpace(BlockPos pos, PoiType116 type) {
        List<UUID> holders = this.claims.holders.get(pos);
        return holders == null || holders.size() < type.getMaxTickets();
    }

    /** Whether a holder holds a point. */
    public boolean holds(BlockPos pos, UUID holder) {
        List<UUID> holders = this.claims.holders.get(pos);
        return holders != null && holders.contains(holder);
    }

    /** Whether anyone holds a point. */
    public boolean isTaken(BlockPos pos) {
        List<UUID> holders = this.claims.holders.get(pos);
        return holders != null && !holders.isEmpty();
    }

    /** Records a holder for a point (again, harmlessly, when one already holds it). */
    public void claim(BlockPos pos, UUID holder) {
        List<UUID> holders = this.claims.holders.computeIfAbsent(pos.toImmutable(), key -> new ArrayList<>());
        if (!holders.contains(holder)) {
            holders.add(holder);
            this.claims.markDirty();
        }
    }

    /** Gives a point back. */
    public void release(BlockPos pos, UUID holder) {
        List<UUID> holders = this.claims.holders.get(pos);
        if (holders != null && holders.remove(holder)) {
            if (holders.isEmpty()) this.claims.holders.remove(pos);
            this.claims.markDirty();
        }
    }

    /**
     * 1.14's sections-to-village count: how many chunk sections (in the largest axis) a position is from a section
     * holding a taken point (a bed, workstation or bell some villager uses), up to a limit.
     */
    public int sectionsToVillage(BlockPos pos, int limit) {
        int sx = pos.getX() >> 4, sy = pos.getY() >> 4, sz = pos.getZ() >> 4;
        int best = Integer.MAX_VALUE;
        for (BlockPos held : this.claims.holders.keySet()) {
            int d = Math.max(Math.abs((held.getX() >> 4) - sx), Math.max(Math.abs((held.getY() >> 4) - sy), Math.abs((held.getZ() >> 4) - sz)));
            if (d < best) best = d;
        }
        return Math.min(best, limit);
    }

    // ---- Sections ----

    private static long key(int cx, int cy, int cz) {
        return ((long) cx & 0x3FFFFFL) << 42 | ((long) cz & 0x3FFFFFL) << 20 | (cy & 0xFFFFFL);
    }

    /** A section's points, scanned on first use; null if its chunk isn't loaded. */
    @Nullable
    private Map<BlockPos, PoiType116> section(int cx, int cy, int cz) {
        if (cy < 0 || cy > 15) return null;
        long key = key(cx, cy, cz);
        Map<BlockPos, PoiType116> section = this.sections.get(key);
        if (section != null) return section;
        if (!this.world.getChunkProvider().chunkExists(cx, cz)) return null;
        Chunk chunk = this.world.getChunk(cx, cz);
        section = new HashMap<>();
        ExtendedBlockStorage storage = chunk.getBlockStorageArray()[cy];
        if (storage != null && !storage.isEmpty()) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        IBlockState state = storage.get(x, y, z);
                        PoiType116 type = PoiType116.of(state);
                        if (type != null) section.put(new BlockPos((cx << 4) + x, (cy << 4) + y, (cz << 4) + z), type);
                    }
                }
            }
        }
        this.sections.put(key, section);
        return section;
    }

    /** A block changed in a loaded chunk: its section's points follow, if it was scanned. */
    public static void onBlockChanged(World world, BlockPos pos, @Nullable IBlockState old, IBlockState now) {
        if (world.isRemote) return;
        PoiManager116 manager = existing(world);
        if (manager == null) return;
        Map<BlockPos, PoiType116> section = manager.sections.get(key(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4));
        if (section == null) return;
        PoiType116 type = PoiType116.of(now);
        if (type != null) {
            section.put(pos.toImmutable(), type);
        } else if (section.remove(pos) != null && manager.claims.holders.remove(pos) != null) {
            manager.claims.markDirty();
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        PoiManager116 manager = existing(event.getWorld());
        if (manager == null) return;
        ChunkPos pos = event.getChunk().getPos();
        for (int cy = 0; cy < 16; cy++) manager.sections.remove(key(pos.x, cy, pos.z));
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        MANAGERS.remove(event.getWorld());
    }

    /** Who holds which point, saved with the world as portium_poi.dat. */
    public static final class Claims extends WorldSavedData {

        static final String NAME = "portium_poi";

        final Map<BlockPos, List<UUID>> holders = new HashMap<>();

        public Claims(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            this.holders.clear();
            NBTTagList list = tag.getTagList("Claims", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                BlockPos pos = BlockPos.fromLong(entry.getLong("Pos"));
                NBTTagList ids = entry.getTagList("Holders", 10);
                List<UUID> holders = new ArrayList<>();
                for (int j = 0; j < ids.tagCount(); j++) holders.add(ids.getCompoundTagAt(j).getUniqueId("Id"));
                if (!holders.isEmpty()) this.holders.put(pos, holders);
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound tag) {
            NBTTagList list = new NBTTagList();
            for (Map.Entry<BlockPos, List<UUID>> entry : this.holders.entrySet()) {
                NBTTagCompound claim = new NBTTagCompound();
                claim.setLong("Pos", entry.getKey().toLong());
                NBTTagList ids = new NBTTagList();
                for (UUID id : entry.getValue()) {
                    NBTTagCompound holder = new NBTTagCompound();
                    holder.setUniqueId("Id", id);
                    ids.appendTag(holder);
                }
                claim.setTag("Holders", ids);
                list.appendTag(claim);
            }
            tag.setTag("Claims", list);
            return tag;
        }
    }
}
