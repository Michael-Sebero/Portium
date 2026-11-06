package com.michaelsebero.portium.mods116.shim;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.util.function.UnaryOperator;

/**
 * 1.16's formattable text ({@code IFormattableTextComponent}, 1.12's text component to Portium): 1.16 builds styles
 * by returning new ones, 1.12 changes its component's style in place.
 */
public final class TextShims116 {

    public static ITextComponent setStyle(ITextComponent text, Style style) {
        return text.setStyle(style);
    }

    public static ITextComponent appendString(ITextComponent text, String string) {
        return text.appendText(string);
    }

    public static ITextComponent appendSibling(ITextComponent text, ITextComponent sibling) {
        return text.appendSibling(sibling);
    }

    @SuppressWarnings("unchecked")
    public static ITextComponent modifyStyle(ITextComponent text, UnaryOperator<?> modifier) {
        Style changed = ((UnaryOperator<Style>) modifier).apply(text.getStyle().createDeepCopy());
        return text.setStyle(changed);
    }

    public static ITextComponent mergeStyle(ITextComponent text, Style style) {
        Style own = text.getStyle();
        if (style.getColor() != null) own.setColor(style.getColor());
        if (style.getBold()) own.setBold(true);
        if (style.getItalic()) own.setItalic(true);
        if (style.getUnderlined()) own.setUnderlined(true);
        if (style.getStrikethrough()) own.setStrikethrough(true);
        if (style.getObfuscated()) own.setObfuscated(true);
        if (style.getClickEvent() != null) own.setClickEvent(style.getClickEvent());
        if (style.getHoverEvent() != null) own.setHoverEvent(style.getHoverEvent());
        if (style.getInsertion() != null) own.setInsertion(style.getInsertion());
        return text;
    }

    public static ITextComponent mergeStyle(ITextComponent text, TextFormatting formatting) {
        apply(text.getStyle(), formatting);
        return text;
    }

    public static ITextComponent mergeStyle(ITextComponent text, TextFormatting[] formattings) {
        for (TextFormatting formatting : formattings) apply(text.getStyle(), formatting);
        return text;
    }

    public static ITextComponent copy(ITextComponent text) {
        return text.createCopy();
    }

    public static String truncated(ITextComponent text, int length) {
        String string = text.getUnformattedText();
        return string.length() <= length ? string : string.substring(0, length);
    }

    /** A formatting code on a style, as 1.16's {@code Style.applyFormatting} does. */
    public static Style apply(Style style, TextFormatting formatting) {
        switch (formatting) {
            case OBFUSCATED:
                return style.setObfuscated(true);
            case BOLD:
                return style.setBold(true);
            case STRIKETHROUGH:
                return style.setStrikethrough(true);
            case UNDERLINE:
                return style.setUnderlined(true);
            case ITALIC:
                return style.setItalic(true);
            case RESET:
                return style.setColor(null).setBold(false).setItalic(false).setUnderlined(false).setStrikethrough(false).setObfuscated(false);
            default:
                return style.setColor(formatting);
        }
    }

    // 1.16's text properties and reordered sequences are 1.12's text components here (strings, for plain ones).

    /** 1.16's {@code getString()} of text properties: the text, without its style. */
    public static String string(Object text) {
        return text instanceof ITextComponent ? ((ITextComponent) text).getUnformattedText() : String.valueOf(text);
    }

    /** 1.16's {@code ITextProperties.func_240652_a_}: plain text properties. */
    public static Object plain(String text) {
        return new net.minecraft.util.text.TextComponentString(text);
    }

    /** 1.16's {@code func_241878_f()}, a component reordered for drawing: the component, which 1.12's font reorders itself. */
    public static Object reordering(ITextComponent text) {
        return text;
    }

    private static final int[] COLORS = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55,
            0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    /** 1.16's {@code formatting.getColor()}: the colour's RGB, or null for the styles (bold, reset...). */
    @javax.annotation.Nullable
    public static Integer formattingColor(TextFormatting formatting) {
        int index = formatting.getColorIndex();
        return index >= 0 && index < COLORS.length ? COLORS[index] : null;
    }

    /** 1.16's {@code style.withFont(font)}: the style as it is (1.12 draws all text in its one font). */
    public static Style withFont(Style style, net.minecraft.util.ResourceLocation font) {
        return style;
    }

    // 1.16's styles are immutable: each "with" makes a new one. Here each changes a copy of 1.12's (which change in place).

    /** 1.16's {@code style.setColor(color)}: the nearest of 1.12's sixteen colours (by the colour's RGB). */
    public static Style withColor(Style style, @javax.annotation.Nullable Object color) {
        return style.createShallowCopy().setColor(color == null ? null : nearest(rgb(color)));
    }

    /** 1.16's {@code style.setFormatting(formatting)}: the formatting's colour. */
    public static Style withFormattingColor(Style style, @javax.annotation.Nullable TextFormatting formatting) {
        return style.createShallowCopy().setColor(formatting == null || !formatting.isColor() ? null : formatting);
    }

    public static Style withBold(Style style, @javax.annotation.Nullable Boolean bold) {
        return style.createShallowCopy().setBold(bold);
    }

    public static Style withItalic(Style style, @javax.annotation.Nullable Boolean italic) {
        return style.createShallowCopy().setItalic(italic);
    }

    public static Style withUnderlined(Style style, @javax.annotation.Nullable Boolean underlined) {
        return style.createShallowCopy().setUnderlined(underlined);
    }

    public static Style withClickEvent(Style style, @javax.annotation.Nullable net.minecraft.util.text.event.ClickEvent event) {
        return style.createShallowCopy().setClickEvent(event);
    }

    public static Style withInsertion(Style style, @javax.annotation.Nullable String insertion) {
        return style.createShallowCopy().setInsertion(insertion);
    }

    /** 1.16's {@code applyFormatting} and {@code forceFormatting}: a formatting code on a copy. */
    public static Style withFormatting(Style style, TextFormatting formatting) {
        return apply(style.createShallowCopy(), formatting);
    }

    /** 1.16's {@code createStyleFromFormattings(formattings...)}. */
    public static Style withFormattings(Style style, TextFormatting[] formattings) {
        Style copy = style.createShallowCopy();
        for (TextFormatting formatting : formattings) apply(copy, formatting);
        return copy;
    }

    /** 1.16's {@code style.mergeStyle(parent)}: this style's settings, the parent's where this has none. */
    public static Style withParent(Style style, Style parent) {
        return style.createShallowCopy().setParentStyle(parent);
    }

    private static int rgb(Object color) {
        try {
            java.lang.reflect.Method value = color.getClass().getMethod("func_240742_a_");
            return (Integer) value.invoke(color);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return 0xFFFFFF;
        }
    }

    private static TextFormatting nearest(int rgb) {
        int best = 15;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < COLORS.length; i++) {
            int dr = (COLORS[i] >> 16 & 255) - (rgb >> 16 & 255), dg = (COLORS[i] >> 8 & 255) - (rgb >> 8 & 255), db = (COLORS[i] & 255) - (rgb & 255);
            long distance = (long) dr * dr + (long) dg * dg + (long) db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return TextFormatting.fromColorIndex(best);
    }

    /** 1.16's {@code Style.EMPTY}: a new style (1.12's styles change in place, so none is shared). */
    public static Style emptyStyle() {
        return new Style();
    }

    private TextShims116() {}
}
