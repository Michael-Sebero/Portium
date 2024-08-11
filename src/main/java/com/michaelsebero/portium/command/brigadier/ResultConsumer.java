package com.michaelsebero.portium.command.brigadier;

import com.michaelsebero.portium.command.brigadier.context.CommandContext;

/** Told the outcome of every command run, which {@code execute store} uses. */
@FunctionalInterface
public interface ResultConsumer<S> {

    void onCommandComplete(CommandContext<S> context, boolean success, int result);
}
