package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import net.minecraft.util.EnumFacing;

import java.util.EnumSet;

/** A set of axes such as {@code xz}, for {@code execute align}. */
public class SwizzleArgument implements ArgumentType<EnumSet<EnumFacing.Axis>> {

    public static SwizzleArgument swizzle() {
        return new SwizzleArgument();
    }

    @SuppressWarnings("unchecked")
    public static EnumSet<EnumFacing.Axis> getSwizzle(CommandContext<?> context, String name) {
        return (EnumSet<EnumFacing.Axis>) context.getArgument(name, EnumSet.class);
    }

    @Override
    public EnumSet<EnumFacing.Axis> parse(StringReader reader) throws CommandSyntaxException {
        EnumSet<EnumFacing.Axis> axes = EnumSet.noneOf(EnumFacing.Axis.class);
        while (reader.canRead() && reader.peek() != ' ') {
            char c = reader.read();
            EnumFacing.Axis axis;
            switch (c) {
                case 'x': axis = EnumFacing.Axis.X; break;
                case 'y': axis = EnumFacing.Axis.Y; break;
                case 'z': axis = EnumFacing.Axis.Z; break;
                default: throw CommandExceptions.SWIZZLE_INVALID.create();
            }
            if (!axes.add(axis)) throw CommandExceptions.SWIZZLE_INVALID.create();
        }
        return axes;
    }
}
