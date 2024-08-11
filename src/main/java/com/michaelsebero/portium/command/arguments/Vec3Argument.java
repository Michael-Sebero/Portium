package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.Collection;

/** A precise position; whole numbers are block centres unless {@code centerIntegers} is off. */
public class Vec3Argument implements ArgumentType<Coordinates> {

    private final boolean centerIntegers;

    private Vec3Argument(boolean centerIntegers) {
        this.centerIntegers = centerIntegers;
    }

    public static Vec3Argument vec3() {
        return new Vec3Argument(true);
    }

    public static Vec3Argument vec3(boolean centerIntegers) {
        return new Vec3Argument(centerIntegers);
    }

    public static Vec3d getVec3(CommandContext<CommandSource> context, String name) {
        return context.getArgument(name, Coordinates.class).getPosition(context.getSource());
    }

    public static Coordinates getLocation(CommandContext<CommandSource> context, String name) {
        return context.getArgument(name, Coordinates.class);
    }

    @Override
    public Coordinates parse(StringReader reader) throws CommandSyntaxException {
        return reader.canRead() && reader.peek() == '^' ? LocalCoordinates.parse(reader) : WorldCoordinates.parseDouble(reader, this.centerIntegers);
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CoordinateSuggestions.suggest(context, builder, true, this);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");
    }
}
