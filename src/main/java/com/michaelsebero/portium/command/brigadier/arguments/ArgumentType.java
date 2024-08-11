package com.michaelsebero.portium.command.brigadier.arguments;

import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Collections;

public interface ArgumentType<T> {

    T parse(StringReader reader) throws CommandSyntaxException;

    default <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return Suggestions.empty();
    }

    default Collection<String> getExamples() {
        return Collections.emptyList();
    }
}
