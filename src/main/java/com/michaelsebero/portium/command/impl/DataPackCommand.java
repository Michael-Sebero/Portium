package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.IdSuggestions;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import com.michaelsebero.portium.datapack.DataPack;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code datapack list|enable|disable}: manages the world's data packs (1.13). */
public final class DataPackCommand {

    private static final CommandExceptionType.Dynamic UNKNOWN = new CommandExceptionType.Dynamic(id -> TextMessage.translate("commands.datapack.unknown", id));
    private static final CommandExceptionType.Dynamic ALREADY_ENABLED = new CommandExceptionType.Dynamic(id -> TextMessage.translate("commands.datapack.enable.failed", id));
    private static final CommandExceptionType.Dynamic ALREADY_DISABLED = new CommandExceptionType.Dynamic(id -> TextMessage.translate("commands.datapack.disable.failed", id));

    private static final SuggestionProvider<CommandSource> ENABLED = (context, builder) -> IdSuggestions.suggest(quoted(manager().getEnabled()), builder);
    private static final SuggestionProvider<CommandSource> DISABLED = (context, builder) -> {
        List<String> disabled = new ArrayList<>(manager().getAvailable());
        disabled.removeAll(manager().getEnabled());
        return IdSuggestions.suggest(quoted(disabled), builder);
    };

    private interface Placement {
        int index(List<String> enabled) throws CommandSyntaxException;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        // Absent while data packs are switched off in Portium's config.
        dispatcher.register(literal("datapack").requires(source -> source.hasPermissionLevel(2) && manager() != null)
                .then(literal("enable").then(argument("name", StringArgumentType.string()).suggests(DISABLED)
                        .executes(context -> enable(context, enabled -> enabled.size()))
                        .then(literal("after").then(argument("existing", StringArgumentType.string()).suggests(ENABLED)
                                .executes(context -> enable(context, enabled -> enabled.indexOf(enabledPack(context, "existing")) + 1))))
                        .then(literal("before").then(argument("existing", StringArgumentType.string()).suggests(ENABLED)
                                .executes(context -> enable(context, enabled -> enabled.indexOf(enabledPack(context, "existing"))))))
                        .then(literal("last").executes(context -> enable(context, enabled -> enabled.size())))
                        .then(literal("first").executes(context -> enable(context, enabled -> 0)))))
                .then(literal("disable").then(argument("name", StringArgumentType.string()).suggests(ENABLED).executes(DataPackCommand::disable)))
                .then(literal("list").executes(context -> listAvailable(context.getSource()) + listEnabled(context.getSource()))
                        .then(literal("available").executes(context -> listAvailable(context.getSource())))
                        .then(literal("enabled").executes(context -> listEnabled(context.getSource())))));
    }

    private static DataPackManager manager() {
        return DataPackManager.get();
    }

    private static List<String> quoted(Iterable<String> ids) {
        List<String> result = new ArrayList<>();
        for (String id : ids) result.add(StringArgumentType.escapeIfRequired(id));
        return result;
    }

    private static String enabledPack(CommandContext<CommandSource> context, String name) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, name);
        if (!manager().getEnabled().contains(id)) throw UNKNOWN.create(id);
        return id;
    }

    private static int enable(CommandContext<CommandSource> context, Placement placement) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "name");
        if (!manager().getAvailable().contains(id)) throw UNKNOWN.create(id);
        if (manager().getEnabled().contains(id)) throw ALREADY_ENABLED.create(id);
        int index = placement.index(manager().getEnabled());
        context.getSource().sendFeedback(new TextComponentTranslation("commands.datapack.enable.success", describe(id)), true);
        manager().enable(id, index);
        return manager().getEnabled().size();
    }

    private static int disable(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "name");
        if (!manager().getAvailable().contains(id)) throw UNKNOWN.create(id);
        if (!manager().getEnabled().contains(id)) throw ALREADY_DISABLED.create(id);
        context.getSource().sendFeedback(new TextComponentTranslation("commands.datapack.disable.success", describe(id)), true);
        manager().disable(id);
        return manager().getEnabled().size();
    }

    /** {@code [name]} in green, with the pack's description on hover, as 1.13 lists packs. */
    private static ITextComponent describe(String id) {
        DataPack pack = manager().getRepository().getPacks().get(id);
        ITextComponent name = new TextComponentString("[" + id + "]");
        name.getStyle().setColor(TextFormatting.GREEN);
        if (pack != null) name.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, pack.getDescription()));
        return name;
    }

    private static ITextComponent describeAll(List<String> ids) {
        ITextComponent list = new TextComponentString("");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) list.appendText(", ");
            list.appendSibling(describe(ids.get(i)));
        }
        return list;
    }

    private static int listEnabled(CommandSource source) {
        List<String> enabled = manager().getEnabled();
        if (enabled.isEmpty()) source.sendFeedback(new TextComponentTranslation("commands.datapack.list.enabled.none"), false);
        else source.sendFeedback(new TextComponentTranslation("commands.datapack.list.enabled.success", enabled.size(), describeAll(enabled)), false);
        return enabled.size();
    }

    private static int listAvailable(CommandSource source) {
        List<String> available = new ArrayList<>(manager().getAvailable());
        available.removeAll(manager().getEnabled());
        if (available.isEmpty()) source.sendFeedback(new TextComponentTranslation("commands.datapack.list.available.none"), false);
        else source.sendFeedback(new TextComponentTranslation("commands.datapack.list.available.success", available.size(), describeAll(available)), false);
        return available.size();
    }

    private DataPackCommand() {}
}
