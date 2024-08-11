package com.michaelsebero.portium.command.brigadier;

import com.michaelsebero.portium.command.brigadier.context.CommandContext;

@FunctionalInterface
public interface SingleRedirectModifier<S> {

    S apply(CommandContext<S> context) throws CommandSyntaxException;
}
