package com.michaelsebero.portium.command.brigadier.arguments;

import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;

public class DoubleArgumentType implements ArgumentType<Double> {

    private final double minimum;
    private final double maximum;

    private DoubleArgumentType(double minimum, double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static DoubleArgumentType doubleArg() {
        return doubleArg(-Double.MAX_VALUE);
    }

    public static DoubleArgumentType doubleArg(double min) {
        return doubleArg(min, Double.MAX_VALUE);
    }

    public static DoubleArgumentType doubleArg(double min, double max) {
        return new DoubleArgumentType(min, max);
    }

    public static double getDouble(CommandContext<?> context, String name) {
        return context.getArgument(name, double.class);
    }

    @Override
    public Double parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        double result = reader.readDouble();
        if (result < this.minimum) {
            reader.setCursor(start);
            throw BuiltInExceptions.DOUBLE_TOO_SMALL.createWithContext(reader, result, this.minimum);
        }
        if (result > this.maximum) {
            reader.setCursor(start);
            throw BuiltInExceptions.DOUBLE_TOO_BIG.createWithContext(reader, result, this.maximum);
        }
        return result;
    }
}
