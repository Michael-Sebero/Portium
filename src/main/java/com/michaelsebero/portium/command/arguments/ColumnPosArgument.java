package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.math.BlockPos;

/** A block column, {@code x z} in whole blocks (1.13.1). */
public class ColumnPosArgument implements ArgumentType<Coordinates> {

    public static ColumnPosArgument columnPos() {
        return new ColumnPosArgument();
    }

    /** The column as a block position at y 0. */
    public static BlockPos getColumnPos(CommandContext<CommandSource> context, String name) {
        return new BlockPos(context.getArgument(name, Coordinates.class).getPosition(context.getSource()));
    }

    @Override
    public Coordinates parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        if (!reader.canRead()) throw CommandExceptions.POS2_INCOMPLETE.createWithContext(reader);
        LocationPart x = LocationPart.parseInt(reader);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw CommandExceptions.POS2_INCOMPLETE.createWithContext(reader);
        }
        reader.skip();
        LocationPart z = LocationPart.parseInt(reader);
        return new WorldCoordinates(x, new LocationPart(false, 0.0D), z);
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CoordinateSuggestions.suggest(context, builder, false, this);
    }
}
