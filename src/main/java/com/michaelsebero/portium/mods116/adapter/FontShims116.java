package com.michaelsebero.portium.mods116.adapter;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.16's font calls on 1.12's font. 1.16 draws text components, "text properties" and reordered character sequences;
 * here all of those are 1.12's text components (or strings), drawn by their formatted text, so colours and styles
 * carry over as 1.12's formatting codes. Text 1.16 wraps or trims comes back as 1.12 components of one formatted line
 * each, and 1.16's character manager (what wraps text) is the font itself. 1.16's matrix stacks and matrices are
 * applied over 1.12's model view; the buffers 1.16 draws world text into aren't used, as 1.12's font draws at once.
 */
@SideOnly(Side.CLIENT)
public final class FontShims116 {

    /** The formatted text of a 1.16 text (component, properties or reordered sequence; all 1.12 components here). */
    public static String text(@Nullable Object text) {
        if (text == null) return "";
        if (text instanceof ITextComponent) return ((ITextComponent) text).getFormattedText();
        return text.toString();
    }

    /** Each line's formatted text. */
    public static List<String> strings(List<?> lines) {
        List<String> strings = new ArrayList<>(lines.size());
        for (Object line : lines) strings.add(text(line));
        return strings;
    }

    // Drawing (the width drawn to, as 1.16 returns)

    public static int draw(FontRenderer font, Object stack, String text, float x, float y, int color) {
        return draw(font, stack, text, x, y, color, false);
    }

    public static int drawShadow(FontRenderer font, Object stack, String text, float x, float y, int color) {
        return draw(font, stack, text, x, y, color, true);
    }

    /** With 1.16's right-to-left flag, which 1.12's font keeps itself. */
    public static int drawShadow(FontRenderer font, Object stack, String text, float x, float y, int color, boolean bidi) {
        return draw(font, stack, text, x, y, color, true);
    }

    public static int draw(FontRenderer font, Object stack, Object text, float x, float y, int color) {
        return draw(font, stack, text(text), x, y, color, false);
    }

    public static int drawShadow(FontRenderer font, Object stack, Object text, float x, float y, int color) {
        return draw(font, stack, text(text), x, y, color, true);
    }

    private static int draw(FontRenderer font, Object stack, @Nullable String text, float x, float y, int color, boolean shadow) {
        if (text == null) return 0;
        boolean pushed = GuiDrawing116.push(stack);
        // 1.16 draws GUI text with blending on, for colours with alpha.
        GlStateManager.enableBlend();
        int end = font.drawString(text, x, y, color, shadow);
        if (pushed) GlStateManager.popMatrix();
        return end;
    }

    /**
     * World text (signs, nameplates, labels): {@code (text, x, y, color, shadow, matrix, buffers, seeThrough,
     * background, light)}. Drawn at once, under the matrix; the background and light are 1.12's font's own.
     */
    public static int drawInWorld(FontRenderer font, String text, float x, float y, int color, boolean shadow, Object matrix, Object buffers,
                                  boolean seeThrough, int background, int light) {
        boolean pushed = GuiDrawing116.pushMatrix(GuiDrawing116.matrix(matrix));
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        if (seeThrough) GlStateManager.disableDepth();
        // 1.16's world text faces the camera with y down; 1.12's font draws the same way under the same matrix.
        int end = font.drawString(text, x, y, color, shadow);
        if (seeThrough) GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        if (pushed) GlStateManager.popMatrix();
        return end;
    }

    public static int drawInWorld(FontRenderer font, Object text, float x, float y, int color, boolean shadow, Object matrix, Object buffers,
                                  boolean seeThrough, int background, int light) {
        return drawInWorld(font, text(text), x, y, color, shadow, matrix, buffers, seeThrough, background, light);
    }

    // Measuring, trimming and wrapping

    public static int width(FontRenderer font, Object text) {
        return font.getStringWidth(text(text));
    }

    /** Trimmed to a width, as a component. */
    public static Object trim(FontRenderer font, Object text, int width) {
        return new TextComponentString(font.trimStringToWidth(text(text), width));
    }

    public static String trim(FontRenderer font, String text, int width) {
        return font.trimStringToWidth(text, width);
    }

    public static String trim(FontRenderer font, String text, int width, boolean reverse) {
        return font.trimStringToWidth(text, width, reverse);
    }

    /** Wrapped to a width: a component per line. */
    public static List<Object> wrap(FontRenderer font, Object text, int width) {
        List<Object> lines = new ArrayList<>();
        for (String line : font.listFormattedStringToWidth(text(text), width)) lines.add(new TextComponentString(line));
        return lines;
    }

    /** 1.16's character manager, which wraps text: the font itself, whose wrapping the manager's calls use. */
    public static Object characterManager(FontRenderer font) {
        return font;
    }

    /** The manager's {@code func_238362_b_} and {@code func_238365_g_}: wrapped lines, starting in a style. */
    public static List<Object> wrapLines(Object manager, Object text, int width, Object style) {
        return wrap((FontRenderer) manager, text, width);
    }

    public static List<Object> wrapLines(Object manager, String text, int width, Object style) {
        return wrap((FontRenderer) manager, text, width);
    }

    /** The manager's width of a text. */
    public static float managerWidth(Object manager, Object text) {
        return width((FontRenderer) manager, text);
    }

    /** The manager's trims of a string to a width (from the start, or keeping the end), and how much of it fits. */
    public static String managerTrim(Object manager, String text, int width, Object style) {
        return ((FontRenderer) manager).trimStringToWidth(text, width);
    }

    public static String managerTrimEnd(Object manager, String text, int width, Object style) {
        return ((FontRenderer) manager).trimStringToWidth(text, width, true);
    }

    public static Object managerTrim(Object manager, Object text, int width, Object style) {
        return trim((FontRenderer) manager, text, width);
    }

    public static int managerFitting(Object manager, String text, int width, Object style) {
        return ((FontRenderer) manager).trimStringToWidth(text, width).length();
    }

    /** 1.16's {@code func_238418_a_}: text wrapped to a width, drawn line under line. */
    public static void drawWrapped(FontRenderer font, Object text, int x, int y, int width, int color) {
        font.drawSplitString(text(text), x, y, width, color);
    }

    /** 1.16's right-to-left reordering, which 1.12's font does itself while drawing. */
    public static String bidiReorder(FontRenderer font, String text) {
        return text;
    }

    private FontShims116() {}
}
