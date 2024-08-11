package com.michaelsebero.portium.command.brigadier.tree;

import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.Command;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.RedirectModifier;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.context.CommandContextBuilder;
import com.michaelsebero.portium.command.brigadier.context.StringRange;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Predicate;

public class LiteralCommandNode<S> extends CommandNode<S> {

    private final String literal;

    public LiteralCommandNode(String literal, Command<S> command, Predicate<S> requirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks) {
        super(command, requirement, redirect, modifier, forks);
        this.literal = literal;
    }

    public String getLiteral() {
        return this.literal;
    }

    @Override
    public String getName() {
        return this.literal;
    }

    @Override
    public void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
        int start = reader.getCursor();
        int end = this.parse(reader);
        if (end < 0) throw BuiltInExceptions.LITERAL_INCORRECT.createWithContext(reader, this.literal);
        contextBuilder.withNode(this, StringRange.between(start, end));
    }

    private int parse(StringReader reader) {
        int start = reader.getCursor();
        if (reader.canRead(this.literal.length())) {
            int end = start + this.literal.length();
            if (reader.getString().substring(start, end).equals(this.literal)) {
                reader.setCursor(end);
                if (!reader.canRead() || reader.peek() == ' ') return end;
                reader.setCursor(start);
            }
        }
        return -1;
    }

    @Override
    public Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (this.literal.toLowerCase(Locale.ROOT).startsWith(builder.getRemaining().toLowerCase(Locale.ROOT))) return builder.suggest(this.literal).build();
        return Suggestions.empty();
    }

    @Override
    protected boolean isValidInput(String input) {
        return this.parse(new StringReader(input)) > -1;
    }

    @Override
    public String getUsageText() {
        return this.literal;
    }

    @Override
    public LiteralArgumentBuilder<S> createBuilder() {
        LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder.<S>literal(this.literal).requires(this.getRequirement()).forward(this.getRedirect(), this.getRedirectModifier(), this.isFork());
        if (this.getCommand() != null) builder.executes(this.getCommand());
        return builder;
    }

    @Override
    protected String getSortedKey() {
        return this.literal;
    }

    @Override
    public Collection<String> getExamples() {
        return Collections.singleton(this.literal);
    }
}
