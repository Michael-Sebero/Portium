package com.michaelsebero.portium.command.brigadier.suggestion;

import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;

@FunctionalInterface
public interface SuggestionProvider<S> {

    Suggestions getSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException;
}
