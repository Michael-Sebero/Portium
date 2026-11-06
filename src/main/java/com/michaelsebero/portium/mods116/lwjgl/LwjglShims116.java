package com.michaelsebero.portium.mods116.lwjgl;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/** LWJGL 3 calls of 1.16 mods (Flywheel's, Create's) that LWJGL 2 has in another shape. */
public final class LwjglShims116 {

    private static final int INFO_LOG_LENGTH = 35716;

    private LwjglShims116() {}

    private static FloatBuffer floats(float[] values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values);
        buffer.flip();
        return buffer;
    }

    public static void glUniform4fv(int location, float[] values) {
        GL20.glUniform4(location, floats(values));
    }

    public static void glUniformMatrix4fv(int location, boolean transpose, float[] values) {
        GL20.glUniformMatrix4(location, transpose, floats(values));
    }

    public static String glGetShaderInfoLog(int shader) {
        return GL20.glGetShaderInfoLog(shader, Math.max(1, GL20.glGetShaderi(shader, INFO_LOG_LENGTH)));
    }

    public static String glGetProgramInfoLog(int program) {
        return GL20.glGetProgramInfoLog(program, Math.max(1, GL20.glGetProgrami(program, INFO_LOG_LENGTH)));
    }

    public static ByteBuffer glMapBuffer(int target, int access) {
        return GL15.glMapBuffer(target, access, null);
    }

    /** {@code glfwGetKey(window, key)}: 1 while the key (a GLFW key code) is down. */
    public static int glfwGetKey(long window, int key) {
        int lwjgl = com.michaelsebero.portium.modern.client.ModernKeys.toLwjgl(key);
        return lwjgl > 0 && Keyboard.isKeyDown(lwjgl) ? 1 : 0;
    }

    /** {@code glfwGetMouseButton(window, button)}. */
    public static int glfwGetMouseButton(long window, int button) {
        return Mouse.isButtonDown(button) ? 1 : 0;
    }
}
