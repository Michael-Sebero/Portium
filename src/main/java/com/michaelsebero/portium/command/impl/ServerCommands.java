package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.LegacyCommands;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.ParseResults;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.context.ParsedCommandNode;
import com.michaelsebero.portium.command.brigadier.tree.CommandNode;
import net.minecraft.command.ICommand;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code help} and {@code list} (1.13). Help also lists other mods' commands, which still run through 1.12. */
public final class ServerCommands {

    private static final CommandExceptionType.Simple HELP_FAILED = new CommandExceptionType.Simple(TextMessage.translate("commands.help.failed"));

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("help")
                .executes(context -> {
                    CommandSource source = context.getSource();
                    Map<String, ITextComponent> lines = new TreeMap<>();
                    for (Map.Entry<CommandNode<CommandSource>, String> usage : dispatcher.getSmartUsage(dispatcher.getRoot(), source).entrySet()) {
                        lines.put(usage.getKey().getName(), new TextComponentString("/" + usage.getValue()));
                    }
                    for (ICommand command : source.getServer().getCommandManager().getPossibleCommands(LegacyCommands.sender(source))) {
                        if (!lines.containsKey(command.getName())) lines.put(command.getName(), legacyUsage(source, command));
                    }
                    for (ITextComponent line : lines.values()) source.sendFeedback(line, false);
                    return lines.size();
                })
                .then(argument("command", StringArgumentType.greedyString()).executes(context -> {
                    CommandSource source = context.getSource();
                    String input = StringArgumentType.getString(context, "command");
                    ParseResults<CommandSource> parse = dispatcher.parse(input, source);
                    List<ParsedCommandNode<CommandSource>> nodes = parse.getContext().getNodes();
                    if (nodes.isEmpty()) {
                        ICommand legacy = source.getServer().getCommandManager().getCommands().get(input.split(" ", 2)[0]);
                        if (legacy == null || !legacy.checkPermission(source.getServer(), LegacyCommands.sender(source))) throw HELP_FAILED.create();
                        source.sendFeedback(legacyUsage(source, legacy), false);
                        return 1;
                    }
                    Map<CommandNode<CommandSource>, String> usages = dispatcher.getSmartUsage(nodes.get(nodes.size() - 1).getNode(), source);
                    for (String usage : usages.values()) source.sendFeedback(new TextComponentString("/" + parse.getReader().getString() + " " + usage), false);
                    return usages.size();
                })));

        dispatcher.register(literal("list")
                .executes(context -> list(context.getSource(), EntityPlayerMP::getDisplayName))
                .then(literal("uuids").executes(context -> list(context.getSource(), player ->
                        new TextComponentTranslation("commands.list.nameAndId", player.getName(), player.getGameProfile().getId())))));
    }

    private static ITextComponent legacyUsage(CommandSource source, ICommand command) {
        return new TextComponentTranslation(command.getUsage(LegacyCommands.sender(source)));
    }

    private static int list(CommandSource source, Function<EntityPlayerMP, ITextComponent> name) {
        PlayerList players = source.getServer().getPlayerList();
        List<EntityPlayerMP> online = players.getPlayers();
        ITextComponent names = new TextComponentString("");
        for (int i = 0; i < online.size(); i++) {
            if (i > 0) names.appendText(", ");
            names.appendSibling(name.apply(online.get(i)));
        }
        source.sendFeedback(new TextComponentTranslation("commands.list.players", online.size(), players.getMaxPlayers(), names), false);
        return online.size();
    }

    private ServerCommands() {}
}
