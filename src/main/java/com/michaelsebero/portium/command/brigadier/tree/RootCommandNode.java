package com.michaelsebero.portium.command.brigadier.tree;

import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.builder.ArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.context.CommandContextBuilder;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Collections;

public class RootCommandNode<S> extends CommandNode<S> {

    public RootCommandNode() {
        super(null, source -> true, null, null, false);
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public String getUsageText() {
        return "";
    }

    @Override
    public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) {}

    @Override
    public Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return Suggestions.empty();
    }

    @Override
    protected boolean isValidInput(String input) {
        return false;
    }

    @Override
    public ArgumentBuilder<S, ?> createBuilder() {
        throw new IllegalStateException("Cannot convert root into a builder");
    }

    @Override
    protected String getSortedKey() {
        return "";
    }

    @Override
    public Collection<String> getExamples() {
        return Collections.emptyList();
    }
}
