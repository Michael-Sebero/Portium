package com.michaelsebero.portium.command.brigadier.arguments;

import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;

public class FloatArgumentType implements ArgumentType<Float> {

    private final float minimum;
    private final float maximum;

    private FloatArgumentType(float minimum, float maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static FloatArgumentType floatArg() {
        return floatArg(-Float.MAX_VALUE);
    }

    public static FloatArgumentType floatArg(float min) {
        return floatArg(min, Float.MAX_VALUE);
    }

    public static FloatArgumentType floatArg(float min, float max) {
        return new FloatArgumentType(min, max);
    }

    public static float getFloat(CommandContext<?> context, String name) {
        return context.getArgument(name, float.class);
    }

    @Override
    public Float parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        float result = reader.readFloat();
        if (result < this.minimum) {
            reader.setCursor(start);
            throw BuiltInExceptions.FLOAT_TOO_SMALL.createWithContext(reader, result, this.minimum);
        }
        if (result > this.maximum) {
            reader.setCursor(start);
            throw BuiltInExceptions.FLOAT_TOO_BIG.createWithContext(reader, result, this.maximum);
        }
        return result;
    }
}
