package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.builder.RequiredArgumentBuilder;

/** Registers the 1.13 commands, and holds the builder shorthands they use. */
public final class Commands {

    public static void registerAll(CommandDispatcher<CommandSource> dispatcher) {
        FunctionCommand.register(dispatcher);
        ReloadCommand.register(dispatcher);
        DataPackCommand.register(dispatcher);
        DataCommand.register(dispatcher);
        ExecuteCommand.register(dispatcher);
        BossBarCommand.register(dispatcher);
        ChatCommands.register(dispatcher);
        EntityCommands.register(dispatcher);
        BlockCommands.register(dispatcher);
        PlayerCommands.register(dispatcher);
        ScoreboardCommand.register(dispatcher);
        TeamCommand.register(dispatcher);
        WorldCommands.register(dispatcher);
        ForceLoadCommand.register(dispatcher);
        ServerCommands.register(dispatcher);
        SoundCommands.register(dispatcher);
        ParticleCommand.register(dispatcher);
        ProgressCommands.register(dispatcher);
        SpreadPlayersCommand.register(dispatcher);
        AdminCommands.register(dispatcher);
    }

    public static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<CommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    private Commands() {}
}
