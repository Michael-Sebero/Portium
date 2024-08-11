package com.michaelsebero.portium.mods116.shim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderItem;

/** 1.16's screen fields, read and written as 1.12's screen's own. Client only. */
public final class ScreenShims116 {

    public static Minecraft minecraft(GuiScreen screen) {
        return screen.mc;
    }

    public static void minecraft(GuiScreen screen, Minecraft minecraft) {
        screen.mc = minecraft;
    }

    public static int width(GuiScreen screen) {
        return screen.width;
    }

    public static void width(GuiScreen screen, int width) {
        screen.width = width;
    }

    public static int height(GuiScreen screen) {
        return screen.height;
    }

    public static void height(GuiScreen screen, int height) {
        screen.height = height;
    }

    public static FontRenderer font(GuiScreen screen) {
        return screen.mc != null ? screen.mc.fontRenderer : Minecraft.getMinecraft().fontRenderer;
    }

    public static void font(GuiScreen screen, FontRenderer font) {}

    public static RenderItem itemRenderer(GuiScreen screen) {
        return Minecraft.getMinecraft().getRenderItem();
    }

    public static void itemRenderer(GuiScreen screen, RenderItem renderer) {}

    private ScreenShims116() {}
}
