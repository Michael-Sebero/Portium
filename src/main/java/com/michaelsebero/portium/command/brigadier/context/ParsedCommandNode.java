package com.michaelsebero.portium.command.brigadier.context;

import com.michaelsebero.portium.command.brigadier.tree.CommandNode;

public class ParsedCommandNode<S> {

    private final CommandNode<S> node;
    private final StringRange range;

    public ParsedCommandNode(CommandNode<S> node, StringRange range) {
        this.node = node;
        this.range = range;
    }

    public CommandNode<S> getNode() {
        return this.node;
    }

    public StringRange getRange() {
        return this.range;
    }
}
