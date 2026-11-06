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

    /** 1.16's {@code screen.init(minecraft, width, height)} on any screen, 1.12's ones too (Create re-opens the screen it came from). */
    public static void init(GuiScreen screen, Minecraft minecraft, int width, int height) {
        screen.setWorldAndResolution(minecraft, width, height);
    }

    /** 1.16's {@code screen.render(stack, mouseX, mouseY, partialTicks)} on any screen: 1.12's draw for 1.12's screens (Create draws the screen it came from behind its own). */
    public static void render(GuiScreen screen, Object matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (screen instanceof com.michaelsebero.portium.mods116.adapter.Screen116) {
            ((com.michaelsebero.portium.mods116.adapter.Screen116) screen).render(matrixStack, mouseX, mouseY, partialTicks);
        } else {
            screen.drawScreen(mouseX, mouseY, partialTicks);
        }
    }
}
