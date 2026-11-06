package com.michaelsebero.portium.modern.shim;

import net.minecraft.util.text.ITextComponent;

import java.util.AbstractList;
import java.util.List;

/**
 * A tooltip as newer code (1.13, 1.16) fills it: it adds text components where 1.12 keeps lines of text, so what's
 * added is turned into its formatted text in 1.12's list, and lines are read back as components.
 */
public final class TooltipList extends AbstractList<Object> {

    private final List<Object> lines;

    @SuppressWarnings("unchecked")
    private TooltipList(List<?> lines) {
        this.lines = (List<Object>) lines;
    }

    public static List<Object> wrap(List<?> lines) {
        return lines instanceof TooltipList ? (TooltipList) lines : new TooltipList(lines);
    }

    private static Object line(Object value) {
        return value instanceof ITextComponent ? ((ITextComponent) value).getFormattedText() : value;
    }

    /** Forge 1.13's and 1.16's {@code ItemTooltipEvent.getToolTip()}: the event's lines, as newer code fills them. */
    public static List<Object> lines(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        return wrap(event.getToolTip());
    }

    /** A line, as newer code reads one: a text component (1.12's lines are text). */
    @Override
    public Object get(int index) {
        Object line = this.lines.get(index);
        return line instanceof String ? new net.minecraft.util.text.TextComponentString((String) line) : line;
    }

    @Override
    public Object set(int index, Object value) {
        return this.lines.set(index, line(value));
    }

    @Override
    public void add(int index, Object value) {
        this.lines.add(index, line(value));
    }

    @Override
    public Object remove(int index) {
        return this.lines.remove(index);
    }

    @Override
    public int size() {
        return this.lines.size();
    }
}
