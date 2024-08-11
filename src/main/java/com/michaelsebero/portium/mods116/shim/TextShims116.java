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

    /** 1.16's {@code Style.EMPTY}: a new style (1.12's styles change in place, so none is shared). */
    public static Style emptyStyle() {
        return new Style();
    }

    private TextShims116() {}
}
