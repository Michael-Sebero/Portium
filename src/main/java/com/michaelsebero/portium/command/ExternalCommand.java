package com.michaelsebero.portium.command;

import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;

import java.util.function.BiConsumer;

/**
 * A command another dispatcher runs, under one name in Portium's: how 1.13 mods' commands, registered with Mojang's
 * Brigadier, are typed and suggested like Portium's own (see {@link PortiumCommands#registerExternal}).
 */
public interface ExternalCommand {

    boolean canUse(CommandSource source);

    /** Runs the whole input, without the leading slash. */
    int run(CommandSource source, String input) throws CommandSyntaxException;

    /** Suggestions for the input (no slash), each as where in the input it starts and its text. */
    void suggest(CommandSource source, String input, BiConsumer<Integer, String> suggestion);
}
