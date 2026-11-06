package com.michaelsebero.portium.mods116.shim;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.ShaderLoader;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 1.16's shader programs of mods' own (Botania's): its {@code ShaderLinkHelper} and {@code ShaderLoader} are static
 * and take any {@code IShaderManager}; 1.12's link only its own shader managers. Done with 1.12's GL helpers.
 */
public final class ShaderShims116 {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    private static final int VERTEX = 35633;
    private static final int FRAGMENT = 35632;
    private static final int COMPILE_STATUS = 35713;
    private static final int LINK_STATUS = 35714;

    private ShaderShims116() {}

    /** 1.16's {@code ShaderLinkHelper.createProgram()}. */
    public static int createProgram() {
        return OpenGlHelper.glCreateProgram();
    }

    /** 1.16's {@code ShaderLinkHelper.useProgram(program)}. */
    public static void useProgram(int program) {
        OpenGlHelper.glUseProgram(program);
    }

    /** 1.16's {@code ShaderLoader.func_216534_a(type, name, source, sourceName)}: a compiled shader. */
    public static ShaderLoader compile(ShaderLoader.ShaderType type, String name, InputStream source, String sourceName) throws IOException {
        byte[] text = IOUtils.toByteArray(source);
        ByteBuffer buffer = BufferUtils.createByteBuffer(text.length);
        buffer.put(text);
        buffer.position(0);
        int shader = OpenGlHelper.glCreateShader(type == ShaderLoader.ShaderType.VERTEX ? VERTEX : FRAGMENT);
        OpenGlHelper.glShaderSource(shader, buffer);
        OpenGlHelper.glCompileShader(shader);
        if (OpenGlHelper.glGetShaderi(shader, COMPILE_STATUS) == 0) {
            String log = OpenGlHelper.glGetShaderInfoLog(shader, 32768);
            throw new IOException("Couldn't compile " + type.getShaderName() + " shader (" + sourceName + ", " + name + "): " + log);
        }
        try {
            Constructor<ShaderLoader> constructor = ShaderLoader.class.getDeclaredConstructor(ShaderLoader.ShaderType.class, int.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(type, shader, name);
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
    }

    private static Field shaderId;

    private static int shaderOf(ShaderLoader loader) {
        try {
            if (shaderId == null) {
                for (Field field : ShaderLoader.class.getDeclaredFields()) {
                    if (field.getType() == int.class && java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
                        field.setAccessible(true);
                        shaderId = field;
                    }
                }
            }
            return shaderId.getInt(loader);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 1.16's {@code ShaderLinkHelper.linkProgram(manager)}: the program with its two shaders. */
    public static void link(int program, Object vertex, Object fragment) {
        OpenGlHelper.glAttachShader(program, shaderOf((ShaderLoader) vertex));
        OpenGlHelper.glAttachShader(program, shaderOf((ShaderLoader) fragment));
        OpenGlHelper.glLinkProgram(program);
        if (OpenGlHelper.glGetProgrami(program, LINK_STATUS) == 0) {
            LOGGER.warn("1.16.5 mod problem: a shader program didn't link: {}", OpenGlHelper.glGetProgramInfoLog(program, 32768));
        }
    }

    /** Flywheel's {@code GlCompat.safeShaderSource(shader, source)} (LWJGL 3 pointer arrays): LWJGL 2's own call. */
    public static void shaderSource(int shader, CharSequence source) {
        org.lwjgl.opengl.GL20.glShaderSource(shader, source);
    }
}
