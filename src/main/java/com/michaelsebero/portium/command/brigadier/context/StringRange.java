package com.michaelsebero.portium.command.brigadier.context;

import java.util.Objects;

public class StringRange {

    private final int start;
    private final int end;

    public StringRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public static StringRange at(int pos) {
        return new StringRange(pos, pos);
    }

    public static StringRange between(int start, int end) {
        return new StringRange(start, end);
    }

    public static StringRange encompassing(StringRange a, StringRange b) {
        return new StringRange(Math.min(a.start, b.start), Math.max(a.end, b.end));
    }

    public int getStart() {
        return this.start;
    }

    public int getEnd() {
        return this.end;
    }

    public String get(String string) {
        return string.substring(this.start, this.end);
    }

    public boolean isEmpty() {
        return this.start == this.end;
    }

    public int getLength() {
        return this.end - this.start;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof StringRange && ((StringRange) o).start == this.start && ((StringRange) o).end == this.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.start, this.end);
    }
}
