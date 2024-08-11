package com.michaelsebero.portium.compat;

import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.mixin.accessor.NBTTagLongArrayAccessor;
import com.michaelsebero.portium.world.waterlog.WaterlogData;
import com.michaelsebero.portium.world.waterlog.WaterlogSync;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Rewrites a chunk saved by 1.13 to 1.16 into 1.12's format. 1.13 stores each section as a palette of named block states and
 * a packed array of palette indices; 1.12 as numeric ids and metadata. Biomes go from ints to bytes, the heightmap is
 * recomputed, entities, block entities and scheduled ticks are converted, and waterlogged blocks become Portium's
 * waterlog data. Chunks the newer version hadn't finished generating are dropped, so 1.12 generates them afresh.
 * 1.15 keeps biomes in three dimensions and 1.16 packs block indices so none runs across two longs.
 */
public final class LegacyChunkNbt {

    /** One 1.13 palette entry, converted once per section. */
    private static final class Entry {

        final String name;
        final Map<String, String> properties;
        final IBlockState state;
        final boolean waterlogged;

        Entry(NBTTagCompound tag) {
            this.name = tag.getString("Name");
            this.properties = new HashMap<>();
            NBTTagCompound props = tag.getCompoundTag("Properties");
            for (String key : props.getKeySet()) this.properties.put(key, props.getString(key));
            this.state = LegacyBlockStates.convert(this.name, this.properties);
            // Portium's own water blocks keep their water in the block state.
            this.waterlogged = "true".equals(this.properties.get("waterlogged")) && !(this.state.getBlock() instanceof IWaterloggable)
                    && Waterlogging.canWaterlog(this.state);
        }

        /** Whether 1.12 needs a block entity built from this block's name or properties. */
        boolean describesBlockEntity() {
            String path = this.name.substring(this.name.indexOf(':') + 1);
            return path.endsWith("_bed") || path.endsWith("_banner") || path.endsWith("_skull") || path.endsWith("_head")
                    || path.equals("flower_pot") || path.startsWith("potted_") || path.equals("note_block");
        }
    }

    /** Converts a 1.13 chunk's root compound; the result has no {@code Level} tag if 1.12 should generate it anew. */
    public static NBTTagCompound convert(NBTTagCompound root) {
        NBTTagCompound result = new NBTTagCompound();
        NBTTagCompound level = root.getCompoundTag("Level");
        String status = level.getString("Status");
        if (status.startsWith("minecraft:")) status = status.substring("minecraft:".length());
        // 1.13 calls a finished chunk postprocessed (or fullchunk); 1.14 on, full.
        if (!"postprocessed".equals(status) && !"fullchunk".equals(status) && !"full".equals(status)) return result;
        boolean padded = root.getInteger("DataVersion") >= ModernSaves.FIRST_PADDED_STATES;
        int chunkX = level.getInteger("xPos");
        int chunkZ = level.getInteger("zPos");
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setInteger("xPos", chunkX);
        legacy.setInteger("zPos", chunkZ);
        legacy.setLong("LastUpdate", level.getLong("LastUpdate"));
        legacy.setLong("InhabitedTime", level.getLong("InhabitedTime"));
        legacy.setByte("V", (byte) 1);
        legacy.setBoolean("TerrainPopulated", true);

        ExtendedBlockStorage[] storages = new ExtendedBlockStorage[16];
        Map<Integer, Entry> described = new HashMap<>();
        WaterlogData waterlog = new WaterlogData();
        NBTTagList sections = level.getTagList("Sections", 10);
        NBTTagList legacySections = new NBTTagList();
        boolean lit = true;
        for (int i = 0; i < sections.tagCount(); i++) {
            NBTTagCompound section = sections.getCompoundTagAt(i);
            int sectionY = section.getByte("Y");
            if (sectionY < 0 || sectionY > 15 || !section.hasKey("Palette", 9)) continue;
            NBTTagList paletteTag = section.getTagList("Palette", 10);
            Entry[] palette = new Entry[paletteTag.tagCount()];
            for (int p = 0; p < palette.length; p++) palette[p] = new Entry(paletteTag.getCompoundTagAt(p));
            long[] packed = longs(section.getTag("BlockStates"));
            ExtendedBlockStorage storage = new ExtendedBlockStorage(sectionY << 4, true);
            // At least four bits an index, more as the palette needs.
            int bits = packed.length == 0 ? 0 : Math.max(4, 32 - Integer.numberOfLeadingZeros(Math.max(palette.length - 1, 1)));
            if (!padded && packed.length > 0) bits = packed.length * 64 / 4096;
            boolean empty = true;
            for (int index = 0; index < 4096; index++) {
                int value = bits == 0 ? 0 : padded ? readPadded(packed, bits, index) : read(packed, bits, index);
                Entry entry = value < palette.length ? palette[value] : null;
                if (entry == null) continue;
                int x = index & 15;
                int y = index >> 8;
                int z = index >> 4 & 15;
                if (entry.state.getBlock() != Blocks.AIR) {
                    storage.set(x, y, z, entry.state);
                    empty = false;
                }
                if (entry.waterlogged) waterlog.set(x, (sectionY << 4) + y, z, true);
                if (entry.describesBlockEntity()) described.put(packedPos(x, (sectionY << 4) + y, z), entry);
            }
            byte[] blockLight = section.getByteArray("BlockLight");
            byte[] skyLight = section.getByteArray("SkyLight");
            if (blockLight.length != 2048 || skyLight.length != 2048) lit = false;
            if (empty) continue;
            storages[sectionY] = storage;
            NBTTagCompound legacySection = new NBTTagCompound();
            legacySection.setByte("Y", (byte) sectionY);
            byte[] blocks = new byte[4096];
            NibbleArray data = new NibbleArray();
            NibbleArray add = storage.getData().getDataForNBT(blocks, data);
            legacySection.setByteArray("Blocks", blocks);
            legacySection.setByteArray("Data", data.getData());
            if (add != null) legacySection.setByteArray("Add", add.getData());
            legacySection.setByteArray("BlockLight", blockLight.length == 2048 ? blockLight : new byte[2048]);
            legacySection.setByteArray("SkyLight", skyLight.length == 2048 ? skyLight : full(2048));
            legacySections.appendTag(legacySection);
        }
        legacy.setTag("Sections", legacySections);
        // Sections 1.13 never lit are relit by 1.12.
        legacy.setBoolean("LightPopulated", lit);
        legacy.setIntArray("HeightMap", heightMap(storages));
        legacy.setByteArray("Biomes", biomes(level.getIntArray("Biomes")));

        NBTTagList entities = new NBTTagList();
        NBTTagList modernEntities = level.getTagList("Entities", 10);
        for (int i = 0; i < modernEntities.tagCount(); i++) entities.appendTag(LegacyEntityNbt.convert(modernEntities.getCompoundTagAt(i)));
        legacy.setTag("Entities", entities);

        NBTTagList blockEntities = new NBTTagList();
        NBTTagList modernBlockEntities = level.getTagList("TileEntities", 10);
        for (int i = 0; i < modernBlockEntities.tagCount(); i++) {
            NBTTagCompound tag = modernBlockEntities.getCompoundTagAt(i);
            Entry entry = described.remove(packedPos(tag.getInteger("x") & 15, tag.getInteger("y"), tag.getInteger("z") & 15));
            blockEntities.appendTag(LegacyBlockEntityNbt.convert(tag, entry == null ? null : entry.name, entry == null ? new HashMap<>() : entry.properties));
        }
        // Flower pots and note blocks have no block entity in 1.13; 1.12 keeps their plant and note in one.
        for (Map.Entry<Integer, Entry> left : described.entrySet()) {
            int pos = left.getKey();
            int x = (chunkX << 4) + (pos & 15);
            int z = (chunkZ << 4) + (pos >> 4 & 15);
            int y = pos >> 8;
            Entry entry = left.getValue();
            NBTTagCompound made = LegacyBlockEntityNbt.flowerPot(entry.name, x, y, z);
            if (made == null) made = LegacyBlockEntityNbt.noteBlock(entry.name, entry.properties, x, y, z);
            if (made != null) blockEntities.appendTag(made);
        }
        legacy.setTag("TileEntities", blockEntities);

        NBTTagList ticks = new NBTTagList();
        convertTicks(level.getTagList("TileTicks", 10), ticks);
        convertTicks(level.getTagList("LiquidTicks", 10), ticks);
        if (!ticks.isEmpty()) legacy.setTag("TileTicks", ticks);

        if (!waterlog.isEmpty()) legacy.setTag(WaterlogSync.TAG, waterlog.write());
        result.setTag("Level", legacy);
        return result;
    }

    private static long[] longs(@Nullable NBTBase tag) {
        return tag instanceof NBTTagLongArray ? ((NBTTagLongArrayAccessor) tag).portium$getData() : new long[0];
    }

    /** One index from 1.13's packing, where values run on across long boundaries. */
    private static int read(long[] packed, int bits, int index) {
        long bit = (long) index * bits;
        int first = (int) (bit >> 6);
        int last = (int) (((long) (index + 1) * bits - 1) >> 6);
        int offset = (int) (bit & 63);
        long mask = (1L << bits) - 1L;
        if (first >= packed.length) return 0;
        long value = packed[first] >>> offset;
        if (first != last && last < packed.length) value |= packed[last] << (64 - offset);
        return (int) (value & mask);
    }

    /** One index from 1.16's packing, where each long holds as many whole values as fit, low bits first. */
    private static int readPadded(long[] packed, int bits, int index) {
        int perLong = 64 / bits;
        int word = index / perLong;
        if (word >= packed.length) return 0;
        int offset = (index % perLong) * bits;
        return (int) (packed[word] >>> offset & ((1L << bits) - 1L));
    }

    private static int packedPos(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    private static byte[] full(int length) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) 0xFF);
        return bytes;
    }

    /** 1.12's heightmap: for each column, one above its highest block that light can't pass straight through. */
    private static int[] heightMap(ExtendedBlockStorage[] storages) {
        int[] heights = new int[256];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                search:
                for (int section = 15; section >= 0; section--) {
                    ExtendedBlockStorage storage = storages[section];
                    if (storage == null) continue;
                    for (int y = 15; y >= 0; y--) {
                        if (storage.get(x, y, z).getLightOpacity() > 0) {
                            heights[z << 4 | x] = (section << 4) + y + 1;
                            break search;
                        }
                    }
                }
            }
        }
        return heights;
    }

    /** 1.12's column biomes: 1.13's are per column too; 1.15's per four-block cube, read here at sea level. */
    private static byte[] biomes(int[] modern) {
        byte[] legacy = new byte[256];
        if (modern.length != 256 && modern.length != 1024) {
            Arrays.fill(legacy, (byte) -1);
            return legacy;
        }
        Map<Integer, Integer> cache = new HashMap<>();
        for (int i = 0; i < 256; i++) {
            int id = modern.length == 256 ? modern[i] : modern[CUBE_AT_SEA_LEVEL << 4 | (i >> 4 & 15) >> 2 << 2 | (i & 15) >> 2];
            legacy[i] = (byte) (int) cache.computeIfAbsent(id, value -> Biome.getIdForBiome(LegacyBiomes.fromModernId(value)));
        }
        return legacy;
    }

    /** The layer of 1.15's biome cubes at height 64. */
    private static final int CUBE_AT_SEA_LEVEL = 64 >> 2;

    /** Scheduled block and fluid updates; 1.12 schedules fluid updates on the flowing block. */
    private static void convertTicks(NBTTagList modern, NBTTagList legacy) {
        for (int i = 0; i < modern.tagCount(); i++) {
            NBTTagCompound tick = modern.getCompoundTagAt(i).copy();
            String name = tick.getString("i");
            String path = name.substring(name.indexOf(':') + 1);
            Block block;
            if (path.equals("water") || path.equals("flowing_water")) block = Blocks.FLOWING_WATER;
            else if (path.equals("lava") || path.equals("flowing_lava")) block = Blocks.FLOWING_LAVA;
            else block = LegacyBlockStates.convert(name, new HashMap<>()).getBlock();
            if (block == Blocks.AIR) continue;
            tick.setString("i", Block.REGISTRY.getNameForObject(block).toString());
            legacy.appendTag(tick);
        }
    }

    private LegacyChunkNbt() {}
}
