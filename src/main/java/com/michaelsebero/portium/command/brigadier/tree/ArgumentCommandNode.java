package com.michaelsebero.portium.command.brigadier.tree;

import com.michaelsebero.portium.command.brigadier.Command;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.RedirectModifier;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.RequiredArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.context.CommandContextBuilder;
import com.michaelsebero.portium.command.brigadier.context.ParsedArgument;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.function.Predicate;

public class ArgumentCommandNode<S, T> extends CommandNode<S> {

    private final String name;
    private final ArgumentType<T> type;
    private final SuggestionProvider<S> customSuggestions;

    public ArgumentCommandNode(String name, ArgumentType<T> type, Command<S> command, Predicate<S> requirement, CommandNode<S> redirect,
                               RedirectModifier<S> modifier, boolean forks, SuggestionProvider<S> customSuggestions) {
        super(command, requirement, redirect, modifier, forks);
        this.name = name;
        this.type = type;
        this.customSuggestions = customSuggestions;
    }

    public ArgumentType<T> getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getUsageText() {
        return "<" + this.name + ">";
    }

    public SuggestionProvider<S> getCustomSuggestions() {
        return this.customSuggestions;
    }

    @Override
    public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
        int start = reader.getCursor();
        T result = this.type.parse(reader);
        ParsedArgument<S, T> parsed = new ParsedArgument<>(start, reader.getCursor(), result);
        contextBuilder.withArgument(this.name, parsed);
        contextBuilder.withNode(this, parsed.getRange());
    }

    @Override
    public Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        return this.customSuggestions == null ? this.type.listSuggestions(context, builder) : this.customSuggestions.getSuggestions(context, builder);
    }

    @Override
    public RequiredArgumentBuilder<S, T> createBuilder() {
        RequiredArgumentBuilder<S, T> builder = RequiredArgumentBuilder.<S, T>argument(this.name, this.type).requires(this.getRequirement())
                .forward(this.getRedirect(), this.getRedirectModifier(), this.isFork()).suggests(this.customSuggestions);
        if (this.getCommand() != null) builder.executes(this.getCommand());
        return builder;
    }

    @Override
    protected boolean isValidInput(String input) {
        try {
            StringReader reader = new StringReader(input);
            this.type.parse(reader);
            return !reader.canRead() || reader.peek() == ' ';
        } catch (CommandSyntaxException ignored) {
            return false;
        }
    }

    @Override
    protected String getSortedKey() {
        return this.name;
    }

    @Override
    public Collection<String> getExamples() {
        return this.type.getExamples();
    }
}
