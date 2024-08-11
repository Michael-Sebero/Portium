package com.michaelsebero.portium.command.brigadier.builder;

import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import com.michaelsebero.portium.command.brigadier.tree.ArgumentCommandNode;
import com.michaelsebero.portium.command.brigadier.tree.CommandNode;

public class RequiredArgumentBuilder<S, T> extends ArgumentBuilder<S, RequiredArgumentBuilder<S, T>> {

    private final String name;
    private final ArgumentType<T> type;
    private SuggestionProvider<S> suggestionsProvider;

    private RequiredArgumentBuilder(String name, ArgumentType<T> type) {
        this.name = name;
        this.type = type;
    }

    public static <S, T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
        return new RequiredArgumentBuilder<>(name, type);
    }

    public RequiredArgumentBuilder<S, T> suggests(SuggestionProvider<S> provider) {
        this.suggestionsProvider = provider;
        return this.getThis();
    }

    public SuggestionProvider<S> getSuggestionsProvider() {
        return this.suggestionsProvider;
    }

    @Override
    protected RequiredArgumentBuilder<S, T> getThis() {
        return this;
    }

    public ArgumentType<T> getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public ArgumentCommandNode<S, T> build() {
        ArgumentCommandNode<S, T> result = new ArgumentCommandNode<>(this.name, this.type, this.getCommand(), this.getRequirement(), this.getRedirect(),
                this.getRedirectModifier(), this.isFork(), this.suggestionsProvider);
        for (CommandNode<S> argument : this.getArguments()) result.addChild(argument);
        return result;
    }
}
