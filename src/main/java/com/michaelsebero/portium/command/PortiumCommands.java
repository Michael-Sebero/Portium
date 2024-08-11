package com.michaelsebero.portium.command;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.command.brigadier.Command;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.ParseResults;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.builder.RequiredArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestion;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.command.impl.Commands;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;

/** The 1.13 command dispatcher: runs vanilla commands typed in chat, command blocks and data pack functions. */
public final class PortiumCommands {

    private static final CommandDispatcher<CommandSource> DISPATCHER = new CommandDispatcher<>();
    private static boolean registered;

    public static synchronized CommandDispatcher<CommandSource> getDispatcher() {
        if (!registered) {
            registered = true;
            Commands.registerAll(DISPATCHER);
            // Tells each command's source how it went, which is how execute store hears results.
            DISPATCHER.setConsumer((context, success, result) -> context.getSource().onCommandComplete(context, success, result));
        }
        return DISPATCHER;
    }

    private static String rootOf(String command) {
        String trimmed = command.startsWith("/") ? command.substring(1) : command;
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    /** Whether a typed command is one this dispatcher handles. */
    public static boolean handles(String command) {
        return getDispatcher().getRoot().getChild(rootOf(command)) != null;
    }

    public static int execute(ICommandSender sender, String command) {
        return execute(CommandSource.of(sender), command);
    }

    /** Runs a command, reporting errors to the source as 1.13 does (the message, then the input up to the error). */
    public static int execute(CommandSource source, String command) {
        StringReader reader = new StringReader(command);
        if (reader.canRead() && reader.peek() == '/') reader.skip();
        try {
            return getDispatcher().execute(getDispatcher().parse(reader, source));
        } catch (CommandSyntaxException e) {
            source.sendErrorMessage(TextMessage.toComponent(e.getRawMessage()));
            if (e.getInput() != null && e.getCursor() >= 0) source.sendErrorMessage(errorContext(e, command));
        } catch (RuntimeException e) {
            ITextComponent detail = new TextComponentString(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            ITextComponent failed = new TextComponentTranslation("command.failed");
            failed.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, detail));
            source.sendErrorMessage(failed);
            Portium.LOGGER.error("Command '{}' failed", command, e);
        }
        return 0;
    }

    private static ITextComponent errorContext(CommandSyntaxException e, String command) {
        int cursor = Math.min(e.getInput().length(), e.getCursor());
        ITextComponent context = new TextComponentString("");
        context.getStyle().setColor(TextFormatting.GRAY).setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        if (cursor > CommandSyntaxException.CONTEXT_AMOUNT) context.appendText("...");
        context.appendText(e.getInput().substring(Math.max(0, cursor - CommandSyntaxException.CONTEXT_AMOUNT), cursor));
        if (cursor < e.getInput().length()) {
            ITextComponent rest = new TextComponentString(e.getInput().substring(cursor));
            rest.getStyle().setColor(TextFormatting.RED).setUnderlined(true);
            context.appendSibling(rest);
        }
        ITextComponent here = new TextComponentTranslation("command.context.here");
        here.getStyle().setColor(TextFormatting.RED).setItalic(true);
        context.appendSibling(here);
        return context;
    }

    /**
     * Completions for 1.12's chat tab-completion, which replaces the last word typed: each suggestion is turned
     * into the text that should replace that word.
     */
    public static List<String> getTabCompletions(ICommandSender sender, String input) {
        CommandSource source = CommandSource.of(sender);
        ParseResults<CommandSource> parse = getDispatcher().parse(new StringReader(input), source);
        Suggestions suggestions = getDispatcher().getCompletionSuggestions(parse);
        int wordStart = input.lastIndexOf(' ') + 1;
        List<String> completions = new ArrayList<>();
        for (Suggestion suggestion : suggestions.getList()) {
            if (suggestion.getRange().getStart() > wordStart) {
                completions.add(input.substring(wordStart, suggestion.getRange().getStart()) + suggestion.getText());
            } else {
                String applied = suggestion.apply(input);
                if (applied.length() >= wordStart) completions.add(applied.substring(wordStart));
            }
        }
        return completions;
    }

    /** The command names a sender may use, for completing the first word. */
    public static List<String> getRootNames(ICommandSender sender, String prefix) {
        CommandSource source = CommandSource.of(sender);
        List<String> names = new ArrayList<>();
        for (com.michaelsebero.portium.command.brigadier.tree.CommandNode<CommandSource> node : getDispatcher().getRoot().getChildren()) {
            if (node.canUse(source) && node.getName().startsWith(prefix)) names.add(node.getName());
        }
        return names;
    }

    /**
     * Adds a command run by another dispatcher: its name with anything after it is handed over whole, and so are
     * suggestions. Replaces a command of the same name.
     */
    public static synchronized void registerExternal(String name, ExternalCommand command) {
        getDispatcher().getRoot().removeChild(name);
        Command<CommandSource> run = context -> command.run(context.getSource(), strip(context.getInput()));
        getDispatcher().register(LiteralArgumentBuilder.<CommandSource>literal(name).requires(command::canUse).executes(run)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("arguments", StringArgumentType.greedyString()).executes(run)
                        .suggests((context, builder) -> {
                            String input = builder.getInput();
                            int offset = input.startsWith("/") ? 1 : 0;
                            SuggestionsBuilder all = builder.createOffset(builder.getStart());
                            command.suggest(context.getSource(), input.substring(offset),
                                    (start, text) -> all.add(builder.createOffset(start + offset).suggest(text)));
                            return all.build();
                        })));
    }

    private static String strip(String input) {
        return input.startsWith("/") ? input.substring(1) : input;
    }

    private PortiumCommands() {}
}
