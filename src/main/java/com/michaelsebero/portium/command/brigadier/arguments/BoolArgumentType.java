package com.michaelsebero.portium.command.brigadier.arguments;

import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;

public class BoolArgumentType implements ArgumentType<Boolean> {

    private BoolArgumentType() {}

    public static BoolArgumentType bool() {
        return new BoolArgumentType();
    }

    public static boolean getBool(CommandContext<?> context, String name) {
        return context.getArgument(name, Boolean.class);
    }

    @Override
    public Boolean parse(StringReader reader) throws CommandSyntaxException {
        return reader.readBoolean();
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if ("true".startsWith(builder.getRemaining().toLowerCase())) builder.suggest("true");
        if ("false".startsWith(builder.getRemaining().toLowerCase())) builder.suggest("false");
        return builder.build();
    }
}
