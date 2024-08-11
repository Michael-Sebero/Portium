package com.michaelsebero.portium.modern.shim;

import net.minecraft.util.text.ITextComponent;

import java.util.AbstractList;
import java.util.List;

/**
 * A tooltip as 1.13 code fills it: 1.13 adds text components where 1.12 keeps lines of text, so what's added is
 * turned into its formatted text in 1.12's list.
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

    @Override
    public Object get(int index) {
        return this.lines.get(index);
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
