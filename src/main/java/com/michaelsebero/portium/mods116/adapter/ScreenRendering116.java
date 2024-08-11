package com.michaelsebero.portium.mods116.adapter;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Help for Portium's 1.16 screens: 1.16's matrix stack (made by name) and GLFW key codes. Client only. */
@SideOnly(Side.CLIENT)
public final class ScreenRendering116 {

    private static Class<?> matrixStack;

    /** A new 1.16 matrix stack, a class of Minecraft 1.16.5's own, made through the 1.16 mods' class loader. */
    public static Object matrixStack() {
        try {
            if (matrixStack == null) {
                ClassLoader loader = com.michaelsebero.portium.mods116.ModernHooks.loader();
                matrixStack = Class.forName("com.mojang.blaze3d.matrix.MatrixStack", true, loader);
            }
            return matrixStack.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't make a 1.16 matrix stack", e);
        }
    }

    /** 1.12's key code as GLFW's, which 1.16's screens take. */
    public static int glfwKey(int lwjglKey) {
        return com.michaelsebero.portium.modern.client.ModernKeys.toGlfw(lwjglKey);
    }

    private ScreenRendering116() {}
}
