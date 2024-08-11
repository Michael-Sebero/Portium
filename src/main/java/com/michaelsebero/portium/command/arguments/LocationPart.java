package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;

/** One coordinate: absolute, or relative ({@code ~}) to the source's position. */
public final class LocationPart {

    private final boolean relative;
    private final double value;

    public LocationPart(boolean relative, double value) {
        this.relative = relative;
        this.value = value;
    }

    public double get(double base) {
        return this.relative ? this.value + base : this.value;
    }

    public boolean isRelative() {
        return this.relative;
    }

    /** Reads a coordinate; whole absolute numbers are moved to the block centre when {@code centerIntegers}. */
    public static LocationPart parseDouble(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') throw CommandExceptions.POS_MIXED.createWithContext(reader);
        if (!reader.canRead()) throw BuiltInExceptions.READER_EXPECTED_DOUBLE.createWithContext(reader);
        boolean relative = isRelative(reader);
        int start = reader.getCursor();
        double value = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0D;
        String text = reader.getString().substring(start, reader.getCursor());
        if (relative && text.isEmpty()) return new LocationPart(true, 0.0D);
        if (!text.contains(".") && !relative && centerIntegers) value += 0.5D;
        return new LocationPart(relative, value);
    }

    public static LocationPart parseInt(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') throw CommandExceptions.POS_MIXED.createWithContext(reader);
        if (!reader.canRead()) throw BuiltInExceptions.READER_EXPECTED_INT.createWithContext(reader);
        boolean relative = isRelative(reader);
        double value;
        if (reader.canRead() && reader.peek() != ' ') value = relative ? reader.readDouble() : reader.readInt();
        else value = 0.0D;
        return new LocationPart(relative, value);
    }

    private static boolean isRelative(StringReader reader) {
        if (reader.peek() == '~') {
            reader.skip();
            return true;
        }
        return false;
    }
}
