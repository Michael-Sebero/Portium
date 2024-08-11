package com.michaelsebero.portium.compat;

/**
 * What the running game offers beyond Forge 1.12.2: Cleanroom (a Forge 1.12.2 fork) runs on LWJGL 3 with its own
 * windowing, where Forge runs on LWJGL 2. Portium uses what's there: newer mods' OpenGL calls go straight to LWJGL 3
 * under Cleanroom, and through Portium's LWJGL 2 mapping otherwise. Found by class, without loading anything.
 */
public final class Cleanroom {

    /** Whether the game runs Cleanroom. */
    public static final boolean PRESENT = exists("com.cleanroommc.boot.Main");
    /** Whether LWJGL 3's OpenGL classes are there (Cleanroom, or anything else putting LWJGL 3 on the class path). */
    public static final boolean LWJGL3 = exists("org.lwjgl.opengl.GL") && exists("org.lwjgl.opengl.GLCapabilities");

    private static boolean exists(String name) {
        ClassLoader loader = Cleanroom.class.getClassLoader();
        String path = name.replace('.', '/') + ".class";
        return loader != null ? loader.getResource(path) != null : ClassLoader.getSystemResource(path) != null;
    }

    private Cleanroom() {}
}
