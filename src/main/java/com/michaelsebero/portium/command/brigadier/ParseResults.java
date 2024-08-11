package com.michaelsebero.portium.command.brigadier;

import com.michaelsebero.portium.command.brigadier.context.CommandContextBuilder;
import com.michaelsebero.portium.command.brigadier.tree.CommandNode;

import java.util.Map;

public class ParseResults<S> {

    private final CommandContextBuilder<S> context;
    private final Map<CommandNode<S>, CommandSyntaxException> exceptions;
    private final StringReader reader;

    public ParseResults(CommandContextBuilder<S> context, StringReader reader, Map<CommandNode<S>, CommandSyntaxException> exceptions) {
        this.context = context;
        this.reader = reader;
        this.exceptions = exceptions;
    }

    public CommandContextBuilder<S> getContext() {
        return this.context;
    }

    public StringReader getReader() {
        return this.reader;
    }

    public Map<CommandNode<S>, CommandSyntaxException> getExceptions() {
        return this.exceptions;
    }
}
