package com.michaelsebero.portium.command.brigadier;

import com.michaelsebero.portium.command.brigadier.context.CommandContext;

@FunctionalInterface
public interface Command<S> {

    int SINGLE_SUCCESS = 1;

    int run(CommandContext<S> context) throws CommandSyntaxException;
}
