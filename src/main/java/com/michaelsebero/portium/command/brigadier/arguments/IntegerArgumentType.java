package com.michaelsebero.portium.command.brigadier.arguments;

import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;

public class IntegerArgumentType implements ArgumentType<Integer> {

    private final int minimum;
    private final int maximum;

    private IntegerArgumentType(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static IntegerArgumentType integer() {
        return integer(Integer.MIN_VALUE);
    }

    public static IntegerArgumentType integer(int min) {
        return integer(min, Integer.MAX_VALUE);
    }

    public static IntegerArgumentType integer(int min, int max) {
        return new IntegerArgumentType(min, max);
    }

    public static int getInteger(CommandContext<?> context, String name) {
        return context.getArgument(name, int.class);
    }

    @Override
    public Integer parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        int result = reader.readInt();
        if (result < this.minimum) {
            reader.setCursor(start);
            throw BuiltInExceptions.INTEGER_TOO_SMALL.createWithContext(reader, result, this.minimum);
        }
        if (result > this.maximum) {
            reader.setCursor(start);
            throw BuiltInExceptions.INTEGER_TOO_BIG.createWithContext(reader, result, this.maximum);
        }
        return result;
    }
}
