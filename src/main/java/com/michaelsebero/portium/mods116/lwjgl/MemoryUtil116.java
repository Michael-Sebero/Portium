package com.michaelsebero.portium.mods116.lwjgl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

/**
 * LWJGL 3's {@code MemoryUtil} for 1.16 mods, which 1.12's LWJGL 2 lacks: buffers are direct, in the platform's byte
 * order, as LWJGL 3 makes them, and freed by the garbage collector (freeing does nothing). Render libraries allocate
 * their staging buffers with it (Flywheel's light volumes).
 */
public final class MemoryUtil116 {

    public static ByteBuffer memAlloc(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static ByteBuffer memCalloc(int size) {
        return memAlloc(size);
    }

    public static ByteBuffer memCalloc(int count, int size) {
        return memAlloc(count * size);
    }

    public static IntBuffer memAllocInt(int count) {
        return memAlloc(count * 4).asIntBuffer();
    }

    public static FloatBuffer memAllocFloat(int count) {
        return memAlloc(count * 4).asFloatBuffer();
    }

    public static void memFree(Buffer buffer) {}

    /** A buffer of the new size holding the old one's contents up to it, at the old position (as far as it reaches). */
    public static ByteBuffer memRealloc(ByteBuffer old, int size) {
        ByteBuffer grown = memAlloc(size);
        if (old == null) return grown;
        ByteBuffer copy = old.duplicate();
        copy.clear();
        copy.limit(Math.min(copy.capacity(), size));
        grown.put(copy);
        grown.clear();
        grown.position(Math.min(old.position(), size));
        return grown;
    }

    /** The first {@code length} bytes from the buffer's position, as ASCII. */
    public static String memASCII(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        ByteBuffer view = buffer.duplicate();
        view.get(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    public static String memASCII(ByteBuffer buffer) {
        return memASCII(buffer, buffer.remaining());
    }

    public static String memUTF8(ByteBuffer buffer, int length) {
        byte[] bytes = new byte[length];
        ByteBuffer view = buffer.duplicate();
        view.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static ByteBuffer memUTF8(CharSequence text, boolean nullTerminated) {
        byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = memAlloc(bytes.length + (nullTerminated ? 1 : 0));
        buffer.put(bytes);
        if (nullTerminated) buffer.put((byte) 0);
        buffer.flip();
        return buffer;
    }

    public static ByteBuffer memUTF8(CharSequence text) {
        return memUTF8(text, true);
    }

    public static ByteBuffer memASCII(CharSequence text, boolean nullTerminated) {
        byte[] bytes = text.toString().getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = memAlloc(bytes.length + (nullTerminated ? 1 : 0));
        buffer.put(bytes);
        if (nullTerminated) buffer.put((byte) 0);
        buffer.flip();
        return buffer;
    }

    private MemoryUtil116() {}
}
