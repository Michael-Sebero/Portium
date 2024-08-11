package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;

import javax.annotation.Nullable;

/** A range such as {@code 1..5}, {@code ..3}, {@code 10..} or {@code 7}, as used by selectors and {@code execute if score}. */
public final class MinMaxBounds {

    @Nullable
    private final Double min;
    @Nullable
    private final Double max;

    public static final MinMaxBounds UNBOUNDED = new MinMaxBounds(null, null);

    public MinMaxBounds(@Nullable Double min, @Nullable Double max) {
        this.min = min;
        this.max = max;
    }

    public static MinMaxBounds exactly(double value) {
        return new MinMaxBounds(value, value);
    }

    public static MinMaxBounds atLeast(double value) {
        return new MinMaxBounds(value, null);
    }

    @Nullable
    public Double getMin() {
        return this.min;
    }

    @Nullable
    public Double getMax() {
        return this.max;
    }

    public boolean isUnbounded() {
        return this.min == null && this.max == null;
    }

    public boolean test(double value) {
        return (this.min == null || this.min <= value) && (this.max == null || this.max >= value);
    }

    /** For distances: compares squared values. */
    public boolean testSquared(double valueSquared) {
        return (this.min == null || this.min * this.min <= valueSquared) && (this.max == null || this.max * this.max >= valueSquared);
    }

    public static MinMaxBounds parseInt(StringReader reader) throws CommandSyntaxException {
        return parse(reader, true);
    }

    public static MinMaxBounds parseFloat(StringReader reader) throws CommandSyntaxException {
        return parse(reader, false, true);
    }

    /** For rotations, where {@code 300..60} wraps through zero. */
    public static MinMaxBounds parseWrapped(StringReader reader) throws CommandSyntaxException {
        return parse(reader, false, false);
    }

    private static MinMaxBounds parse(StringReader reader, boolean integers) throws CommandSyntaxException {
        return parse(reader, integers, true);
    }

    private static MinMaxBounds parse(StringReader reader, boolean integers, boolean ordered) throws CommandSyntaxException {
        if (!reader.canRead()) throw CommandExceptions.RANGE_EMPTY.createWithContext(reader);
        int start = reader.getCursor();
        Double min = readNumber(reader, integers);
        Double max;
        if (reader.canRead(2) && reader.peek() == '.' && reader.peek(1) == '.') {
            reader.skip();
            reader.skip();
            max = readNumber(reader, integers);
            if (min == null && max == null) {
                reader.setCursor(start);
                throw CommandExceptions.RANGE_EMPTY.createWithContext(reader);
            }
        } else {
            max = min;
        }
        if (min == null && max == null) {
            reader.setCursor(start);
            throw CommandExceptions.RANGE_EMPTY.createWithContext(reader);
        }
        if (ordered && min != null && max != null && min > max) {
            reader.setCursor(start);
            throw CommandExceptions.RANGE_SWAPPED.createWithContext(reader);
        }
        return new MinMaxBounds(min, max);
    }

    @Nullable
    private static Double readNumber(StringReader reader, boolean integers) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && isNumberChar(reader, integers)) reader.skip();
        String text = reader.getString().substring(start, reader.getCursor());
        if (text.isEmpty()) return null;
        try {
            return integers ? (double) Integer.parseInt(text) : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            reader.setCursor(start);
            throw (integers ? com.michaelsebero.portium.command.brigadier.BuiltInExceptions.READER_INVALID_INT
                    : com.michaelsebero.portium.command.brigadier.BuiltInExceptions.READER_INVALID_DOUBLE).createWithContext(reader, text);
        }
    }

    /** Digits and a minus sign; a dot only when it isn't the start of {@code ..}. */
    private static boolean isNumberChar(StringReader reader, boolean integers) {
        char c = reader.peek();
        if (c >= '0' && c <= '9' || c == '-') return true;
        if (integers || c != '.') return false;
        return !reader.canRead(2) || reader.peek(1) != '.';
    }
}
