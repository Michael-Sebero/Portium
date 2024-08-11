package com.michaelsebero.portium.mods116.lwjgl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * LWJGL 3's {@code MemoryStack} for 1.16 mods: a frame of short-lived buffers, here simply allocated (and collected),
 * so pushing and popping frames does nothing. One per thread, as LWJGL 3 keeps them.
 */
public final class MemoryStack116 implements AutoCloseable {

    private static final ThreadLocal<MemoryStack116> STACKS = ThreadLocal.withInitial(MemoryStack116::new);

    public static MemoryStack116 stackGet() {
        return STACKS.get();
    }

    public static MemoryStack116 stackPush() {
        return stackGet();
    }

    public MemoryStack116 push() {
        return this;
    }

    public MemoryStack116 pop() {
        return this;
    }

    public ByteBuffer malloc(int size) {
        return MemoryUtil116.memAlloc(size);
    }

    public ByteBuffer calloc(int size) {
        return MemoryUtil116.memAlloc(size);
    }

    public IntBuffer mallocInt(int count) {
        return MemoryUtil116.memAllocInt(count);
    }

    public FloatBuffer mallocFloat(int count) {
        return MemoryUtil116.memAllocFloat(count);
    }

    public int getPointer() {
        return 0;
    }

    public void setPointer(int pointer) {}

    @Override
    public void close() {}
}
