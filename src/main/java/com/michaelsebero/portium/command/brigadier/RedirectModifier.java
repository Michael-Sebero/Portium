package com.michaelsebero.portium.command.brigadier;

import com.michaelsebero.portium.command.brigadier.context.CommandContext;

import java.util.Collection;

/** Turns one source into any number of sources for a redirected (possibly forking) branch, like {@code execute as}. */
@FunctionalInterface
public interface RedirectModifier<S> {

    Collection<S> apply(CommandContext<S> context) throws CommandSyntaxException;
}
