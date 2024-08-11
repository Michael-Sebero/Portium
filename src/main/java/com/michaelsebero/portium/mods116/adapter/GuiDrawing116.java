package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.mods116.ModernHooks;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;

/**
 * 1.16's GUI drawing ({@code AbstractGui}'s blits, fills and strings), which screens and widgets call on themselves or
 * statically, done with 1.12's tessellator and font: 1.16's matrix stack's top pose is applied over 1.12's model view
 * first (a mod's screen moves and scales what it draws with it), and 1.16's blit offset is 1.12's {@code zLevel}.
 * The stack, a class of Minecraft 1.16.5's own, is taken as an object.
 */
@SideOnly(Side.CLIENT)
public final class GuiDrawing116 {

    private static final Field Z_LEVEL = ObfuscationReflectionHelper.findField(Gui.class, "field_73735_i");
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static MethodHandle pose;
    private static MethodHandle matrix;

    // Blit offset

    public static int getBlitOffset(Gui gui) {
        try {
            return (int) Z_LEVEL.getFloat(gui);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    public static void setBlitOffset(Gui gui, int offset) {
        try {
            Z_LEVEL.setFloat(gui, offset);
        } catch (IllegalAccessException e) {
            // Left as it is.
        }
    }

    // Fills

    public static void fill(Object stack, int x1, int y1, int x2, int y2, int color) {
        push(stack);
        Gui.drawRect(x1, y1, x2, y2, color);
        pop(stack);
    }

    public static void hLine(Gui gui, Object stack, int x1, int x2, int y, int color) {
        if (x2 < x1) {
            int swap = x1;
            x1 = x2;
            x2 = swap;
        }
        fill(stack, x1, y, x2 + 1, y + 1, color);
    }

    public static void vLine(Gui gui, Object stack, int x, int y1, int y2, int color) {
        if (y2 < y1) {
            int swap = y1;
            y1 = y2;
            y2 = swap;
        }
        fill(stack, x, y1 + 1, x + 1, y2, color);
    }

    public static void fillGradient(Gui gui, Object stack, int x1, int y1, int x2, int y2, int top, int bottom) {
        push(stack);
        float z = getBlitOffset(gui);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x2, y1, z).color(top >> 16 & 255, top >> 8 & 255, top & 255, top >>> 24).endVertex();
        buffer.pos(x1, y1, z).color(top >> 16 & 255, top >> 8 & 255, top & 255, top >>> 24).endVertex();
        buffer.pos(x1, y2, z).color(bottom >> 16 & 255, bottom >> 8 & 255, bottom & 255, bottom >>> 24).endVertex();
        buffer.pos(x2, y2, z).color(bottom >> 16 & 255, bottom >> 8 & 255, bottom & 255, bottom >>> 24).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        pop(stack);
    }

    // Text (1.16's text components are 1.12's here)

    public static void drawCenteredString(Object stack, FontRenderer font, String text, int x, int y, int color) {
        push(stack);
        font.drawStringWithShadow(text, x - font.getStringWidth(text) / 2f, y, color);
        pop(stack);
    }

    public static void drawCenteredString(Object stack, FontRenderer font, ITextComponent text, int x, int y, int color) {
        drawCenteredString(stack, font, text.getFormattedText(), x, y, color);
    }

    public static void drawString(Object stack, FontRenderer font, String text, int x, int y, int color) {
        push(stack);
        font.drawStringWithShadow(text, x, y, color);
        pop(stack);
    }

    public static void drawString(Object stack, FontRenderer font, ITextComponent text, int x, int y, int color) {
        drawString(stack, font, text.getFormattedText(), x, y, color);
    }

    // Blits

    /** {@code blit(stack, x, y, u, v, width, height)} of a 256 by 256 texture, at the GUI's blit offset. */
    public static void blit(Gui gui, Object stack, int x, int y, int u, int v, int width, int height) {
        blit(stack, x, y, getBlitOffset(gui), (float) u, (float) v, width, height, 256, 256);
    }

    /** {@code blit(stack, x, y, z, u, v, width, height, textureHeight, textureWidth)}, as 1.16 orders them. */
    public static void blit(Object stack, int x, int y, int z, float u, float v, int width, int height, int textureHeight, int textureWidth) {
        inner(stack, x, x + width, y, y + height, z, u / textureWidth, (u + width) / textureWidth, v / textureHeight, (v + height) / textureHeight);
    }

    /** {@code blit(stack, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight)}: a region stretched. */
    public static void blit(Object stack, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        inner(stack, x, x + width, y, y + height, 0, u / textureWidth, (u + uWidth) / textureWidth, v / textureHeight, (v + vHeight) / textureHeight);
    }

    /** {@code blit(stack, x, y, u, v, width, height, textureWidth, textureHeight)}. */
    public static void blit(Object stack, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        blit(stack, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    /** {@code blit(stack, x, y, z, width, height, sprite)}: an atlas sprite. */
    public static void blit(Object stack, int x, int y, int z, int width, int height, TextureAtlasSprite sprite) {
        inner(stack, x, x + width, y, y + height, z, sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV());
    }

    private static void inner(Object stack, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2) {
        push(stack);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x1, y2, z).tex(u1, v2).endVertex();
        buffer.pos(x2, y2, z).tex(u2, v2).endVertex();
        buffer.pos(x2, y1, z).tex(u2, v1).endVertex();
        buffer.pos(x1, y1, z).tex(u1, v1).endVertex();
        tessellator.draw();
        GlStateManager.enableAlpha();
        pop(stack);
    }

    // Forge 1.16's GuiUtils, on Forge 1.12's (whose tooltips take no colours of their own).

    public static void gradientRect(Object matrix, int z, int left, int top, int right, int bottom, int startColor, int endColor) {
        boolean pushed = pushMatrix(matrix(matrix));
        net.minecraftforge.fml.client.config.GuiUtils.drawGradientRect(z, left, top, right, bottom, startColor, endColor);
        if (pushed) GlStateManager.popMatrix();
    }

    public static void hoveringText(Object stack, java.util.List<?> lines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxWidth,
                                    FontRenderer font) {
        push(stack);
        net.minecraftforge.fml.client.config.GuiUtils.drawHoveringText(FontShims116.strings(lines), mouseX, mouseY, screenWidth, screenHeight, maxWidth, font);
        pop(stack);
    }

    public static void hoveringText(Object stack, java.util.List<?> lines, int mouseX, int mouseY, int screenWidth, int screenHeight, int maxWidth,
                                    int background, int borderStart, int borderEnd, FontRenderer font) {
        hoveringText(stack, lines, mouseX, mouseY, screenWidth, screenHeight, maxWidth, font);
    }

    // The stack's pose (or a matrix) over 1.12's model view, when it isn't the identity.

    static boolean push(@Nullable Object stack) {
        return pushMatrix(pose(stack));
    }

    static void pop(@Nullable Object stack) {
        if (pose(stack) != null) GlStateManager.popMatrix();
    }

    /** Multiplies a matrix (column by column) onto 1.12's model view, pushed; nothing for none or the identity. */
    static boolean pushMatrix(@Nullable float[] matrix) {
        if (matrix == null || isIdentity(matrix)) return false;
        GlStateManager.pushMatrix();
        MATRIX.clear();
        MATRIX.put(matrix);
        MATRIX.flip();
        GlStateManager.multMatrix(MATRIX);
        return true;
    }

    @Nullable
    private static float[] pose(@Nullable Object stack) {
        if (stack == null) return null;
        float[] matrix = (float[]) glue("pose", stack);
        return matrix == null || isIdentity(matrix) ? null : matrix;
    }

    /** A 1.16 {@code Matrix4f}'s values, column by column. */
    @Nullable
    static float[] matrix(@Nullable Object matrix) {
        return matrix == null ? null : (float[]) glue("matrix", matrix);
    }

    @Nullable
    private static Object glue(String name, Object argument) {
        try {
            MethodHandle handle = "pose".equals(name) ? pose : matrix;
            if (handle == null) {
                Class<?> glue = Class.forName("com.michaelsebero.portium.mods116.host.RenderGlue", true, ModernHooks.loader());
                handle = MethodHandles.publicLookup().findStatic(glue, name, MethodType.methodType(float[].class, Object.class));
                if ("pose".equals(name)) {
                    pose = handle;
                } else {
                    matrix = handle;
                }
            }
            return handle.invoke(argument);
        } catch (Throwable e) {
            return null;
        }
    }

    private static boolean isIdentity(float[] matrix) {
        for (int i = 0; i < 16; i++) {
            if (matrix[i] != (i % 5 == 0 ? 1f : 0f)) return false;
        }
        return true;
    }

    private GuiDrawing116() {}
}
