package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.compat.LegacyBlockStates;

import java.util.Arrays;
import java.util.Collection;

/** A block state in 1.13's flattened syntax ({@code minecraft:oak_stairs[facing=north]{...}}), translated to 1.12. */
public class BlockStateArgument implements ArgumentType<BlockStateInput> {

    public static BlockStateArgument blockState() {
        return new BlockStateArgument();
    }

    public static BlockStateInput getBlockState(CommandContext<?> context, String name) {
        return context.getArgument(name, BlockStateInput.class);
    }

    @Override
    public BlockStateInput parse(StringReader reader) throws CommandSyntaxException {
        BlockStateParser parser = new BlockStateParser(reader, false).parse(true);
        return new BlockStateInput(LegacyBlockStates.convert(parser.getId().toString(), parser.getProperties()), parser.getNbt(),
                "true".equals(parser.getProperties().get("waterlogged")));
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return IdSuggestions.blocks(builder, false);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "foo{bar=baz}");
    }
}
