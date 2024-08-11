package com.michaelsebero.portium.world.waterlog;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;

/**
 * Which cells of one chunk are waterlogged: a 4096-bit set per 16-block section. Writes copy the section they
 * change, so chunk render threads can read without locking.
 */
public final class WaterlogData {

    private static final int SECTIONS = 16;
    private static final int WORDS = 64;

    private volatile long[][] sections = new long[SECTIONS][];

    private static int index(int x, int y, int z) {
        return (y & 15) << 8 | (z & 15) << 4 | (x & 15);
    }

    public boolean get(int x, int y, int z) {
        if (y < 0 || y >= SECTIONS * 16) return false;
        long[] section = this.sections[y >> 4];
        if (section == null) return false;
        int index = index(x, y, z);
        return (section[index >> 6] & 1L << (index & 63)) != 0;
    }

    /** Returns whether the cell changed. */
    public synchronized boolean set(int x, int y, int z, boolean waterlogged) {
        if (y < 0 || y >= SECTIONS * 16 || this.get(x, y, z) == waterlogged) return false;
        long[][] copy = this.sections.clone();
        long[] section = copy[y >> 4] == null ? new long[WORDS] : copy[y >> 4].clone();
        int index = index(x, y, z);
        if (waterlogged) section[index >> 6] |= 1L << (index & 63);
        else section[index >> 6] &= ~(1L << (index & 63));
        copy[y >> 4] = isEmpty(section) ? null : section;
        this.sections = copy;
        return true;
    }

    /** Takes over another set's cells, as when a client receives a chunk's. */
    public synchronized void copyFrom(WaterlogData other) {
        this.sections = other.sections;
    }

    public boolean isEmpty() {
        for (long[] section : this.sections) {
            if (section != null) return false;
        }
        return true;
    }

    private static boolean isEmpty(long[] section) {
        for (long word : section) {
            if (word != 0L) return false;
        }
        return true;
    }

    // ---- Saving, as int arrays: 1.12 can write a long array tag but not read one back ----

    public NBTTagList write() {
        NBTTagList list = new NBTTagList();
        long[][] current = this.sections;
        for (int y = 0; y < SECTIONS; y++) {
            if (current[y] == null) continue;
            int[] ints = new int[WORDS * 2];
            for (int i = 0; i < WORDS; i++) {
                ints[i * 2] = (int) current[y][i];
                ints[i * 2 + 1] = (int) (current[y][i] >>> 32);
            }
            NBTTagCompound section = new NBTTagCompound();
            section.setByte("Y", (byte) y);
            section.setTag("Cells", new NBTTagIntArray(ints));
            list.appendTag(section);
        }
        return list;
    }

    public synchronized void read(NBTTagList list) {
        long[][] loaded = new long[SECTIONS][];
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound section = list.getCompoundTagAt(i);
            int y = section.getByte("Y");
            int[] ints = section.getIntArray("Cells");
            if (y < 0 || y >= SECTIONS || ints.length != WORDS * 2) continue;
            long[] words = new long[WORDS];
            for (int w = 0; w < WORDS; w++) words[w] = ints[w * 2] & 0xFFFFFFFFL | (long) ints[w * 2 + 1] << 32;
            loaded[y] = isEmpty(words) ? null : words;
        }
        this.sections = loaded;
    }

    // ---- Network ----

    public void write(ByteBuf buf) {
        long[][] current = this.sections;
        int mask = 0;
        for (int y = 0; y < SECTIONS; y++) if (current[y] != null) mask |= 1 << y;
        buf.writeShort(mask);
        for (int y = 0; y < SECTIONS; y++) {
            if (current[y] == null) continue;
            for (long word : current[y]) buf.writeLong(word);
        }
    }

    public synchronized void read(ByteBuf buf) {
        long[][] loaded = new long[SECTIONS][];
        int mask = buf.readUnsignedShort();
        for (int y = 0; y < SECTIONS; y++) {
            if ((mask & 1 << y) == 0) continue;
            long[] words = new long[WORDS];
            for (int w = 0; w < WORDS; w++) words[w] = buf.readLong();
            loaded[y] = words;
        }
        this.sections = loaded;
    }
}
