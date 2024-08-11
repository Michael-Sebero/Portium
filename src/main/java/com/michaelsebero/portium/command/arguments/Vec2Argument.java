package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.math.Vec3d;

/** A horizontal position, {@code x z}. */
public class Vec2Argument implements ArgumentType<Coordinates> {

    public static Vec2Argument vec2() {
        return new Vec2Argument();
    }

    public static Vec2 getVec2(CommandContext<CommandSource> context, String name) {
        Vec3d pos = context.getArgument(name, Coordinates.class).getPosition(context.getSource());
        return new Vec2((float) pos.x, (float) pos.z);
    }

    @Override
    public Coordinates parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        if (!reader.canRead()) throw CommandExceptions.POS2_INCOMPLETE.createWithContext(reader);
        LocationPart x = LocationPart.parseDouble(reader, true);
        if (!reader.canRead() || reader.peek() != ' ') {
            reader.setCursor(start);
            throw CommandExceptions.POS2_INCOMPLETE.createWithContext(reader);
        }
        reader.skip();
        LocationPart z = LocationPart.parseDouble(reader, true);
        return new WorldCoordinates(x, new LocationPart(true, 0.0D), z);
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CoordinateSuggestions.suggest(context, builder, false, this);
    }
}
