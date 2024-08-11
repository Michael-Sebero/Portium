package com.michaelsebero.portium.modern.state;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.nbt.NBTTagIntArray;

/**
 * A chunk's full states of 1.13 blocks with more than 16 states: for each such cell, which of its block's states it
 * holds. 1.12 saves 16 states per block in its metadata; the rest go here, keyed by cell.
 */
public final class ModernStateData {

    private final Int2IntOpenHashMap states = new Int2IntOpenHashMap();

    public ModernStateData() {
        this.states.defaultReturnValue(-1);
    }

    static int key(int x, int y, int z) {
        return (x & 15) << 12 | (z & 15) << 8 | (y & 255);
    }

    /** The state index at a cell (world coordinates), or -1 if none is kept. */
    public synchronized int get(int x, int y, int z) {
        return this.states.get(key(x, y, z));
    }

    /** Returns the index kept before, or -1. */
    public synchronized int put(int x, int y, int z, int index) {
        return this.states.put(key(x, y, z), index);
    }

    public synchronized void remove(int x, int y, int z) {
        this.states.remove(key(x, y, z));
    }

    public synchronized boolean isEmpty() {
        return this.states.isEmpty();
    }

    public synchronized void copyFrom(ModernStateData other) {
        this.states.clear();
        synchronized (other) {
            this.states.putAll(other.states);
        }
    }

    public synchronized NBTTagIntArray write() {
        int[] pairs = new int[this.states.size() * 2];
        int i = 0;
        for (Int2IntMap.Entry entry : this.states.int2IntEntrySet()) {
            pairs[i++] = entry.getIntKey();
            pairs[i++] = entry.getIntValue();
        }
        return new NBTTagIntArray(pairs);
    }

    public synchronized void read(int[] pairs) {
        this.states.clear();
        for (int i = 0; i + 1 < pairs.length; i += 2) this.states.put(pairs[i], pairs[i + 1]);
    }

    public synchronized void write(ByteBuf buf) {
        buf.writeInt(this.states.size());
        for (Int2IntMap.Entry entry : this.states.int2IntEntrySet()) {
            buf.writeInt(entry.getIntKey());
            buf.writeInt(entry.getIntValue());
        }
    }

    public synchronized void read(ByteBuf buf) {
        this.states.clear();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) this.states.put(buf.readInt(), buf.readInt());
    }
}
