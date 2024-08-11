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
import net.minecraft.world.WorldServer;

import java.util.Arrays;
import java.util.Collection;

public class BlockPosArgument implements ArgumentType<Coordinates> {

    public static BlockPosArgument blockPos() {
        return new BlockPosArgument();
    }

    /** A block position that must be loaded and inside the world (1.13's "loaded block pos"). */
    public static BlockPos getLoadedBlockPos(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        BlockPos pos = getBlockPos(context, name);
        WorldServer world = context.getSource().getWorld();
        if (!world.isBlockLoaded(pos)) throw CommandExceptions.POS_UNLOADED.create();
        if (!world.isValid(pos)) throw CommandExceptions.POS_OUT_OF_WORLD.create();
        return pos;
    }

    public static BlockPos getBlockPos(CommandContext<CommandSource> context, String name) {
        return context.getArgument(name, Coordinates.class).getBlockPos(context.getSource());
    }

    @Override
    public Coordinates parse(StringReader reader) throws CommandSyntaxException {
        return reader.canRead() && reader.peek() == '^' ? LocalCoordinates.parse(reader) : WorldCoordinates.parseInt(reader);
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CoordinateSuggestions.suggest(context, builder, true, this);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");
    }
}
