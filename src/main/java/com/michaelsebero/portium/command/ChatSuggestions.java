package com.michaelsebero.portium.command;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.ParseResults;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.context.ParsedArgument;
import com.michaelsebero.portium.command.brigadier.context.StringRange;
import com.michaelsebero.portium.command.brigadier.context.SuggestionContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestion;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.tree.CommandNode;
import com.michaelsebero.portium.command.brigadier.tree.LiteralCommandNode;
import com.michaelsebero.portium.network.CommandSuggestRequest;
import com.michaelsebero.portium.network.CommandSuggestResponse;
import com.michaelsebero.portium.network.PortiumNetwork;
import com.michaelsebero.portium.network.ServerFeaturesMessage;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What 1.13's chat screen shows while a command is typed, worked out for the player typing it: the colour of each
 * argument, the part that doesn't parse, the suggestion list, and the error or usage lines above the input. 1.13
 * clients compute this from a copy of the command tree; Portium's clients ask the server instead, so permissions,
 * server-side suggestions and other mods' 1.12 commands all come out right without one.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class ChatSuggestions {

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) PortiumNetwork.CHANNEL.sendTo(new ServerFeaturesMessage(true), (EntityPlayerMP) event.player);
    }

    public static CommandSuggestResponse respond(EntityPlayerMP player, CommandSuggestRequest request) {
        CommandSuggestResponse response = new CommandSuggestResponse();
        response.id = request.id;
        response.text = request.text;
        if (!request.text.startsWith("/")) return response;
        try {
            ICommandManager legacy = player.getServer().getCommandManager();
            boolean typingName = request.text.indexOf(' ') < 0;
            boolean legacyCommand = !typingName && !PortiumCommands.handles(request.text) && legacy.getCommands().containsKey(nameOf(request.text));
            if (PortiumConfig.mechanics.modernCommands && !legacyCommand) {
                describe(player, request, new TypingSource(CommandSource.of(player), request.targetBlock, request.targetEntity), response);
            } else if (request.wantSuggestions && request.cursor >= 1) {
                completeLegacy(player, request, response);
            }
        } catch (RuntimeException e) {
            Portium.LOGGER.error("Couldn't suggest for '{}'", request.text, e);
        }
        return response;
    }

    private static String nameOf(String text) {
        int space = text.indexOf(' ');
        return space < 0 ? text.substring(1) : text.substring(1, space);
    }

    private static void describe(EntityPlayerMP player, CommandSuggestRequest request, CommandSource source, CommandSuggestResponse response) {
        String text = request.text;
        int cursor = request.cursor;
        CommandDispatcher<CommandSource> dispatcher = PortiumCommands.getDispatcher();
        StringReader reader = new StringReader(text);
        reader.skip();
        ParseResults<CommandSource> parse = dispatcher.parse(reader, source);
        // 1.13 colours only the arguments of the last command, after any "execute ... run".
        List<Integer> arguments = new ArrayList<>();
        for (ParsedArgument<CommandSource, ?> argument : parse.getContext().getLastChild().getArguments().values()) {
            arguments.add(argument.getRange().getStart());
            arguments.add(argument.getRange().getEnd());
        }
        response.arguments = arguments.stream().mapToInt(Integer::intValue).toArray();
        boolean typingName = text.indexOf(' ') < 0;
        // A 1.12 command's name is complete and valid, even though the 1.13 tree doesn't know it.
        boolean legacyName = typingName && player.getServer().getCommandManager().getCommands().containsKey(nameOf(text));
        if (parse.getReader().canRead() && !legacyName) {
            response.errorStart = parse.getReader().getCursor();
            response.errorEnd = response.errorStart + parse.getReader().getRemainingLength();
        }
        if (!request.wantSuggestions || cursor < 1) return;

        Suggestions suggestions = dispatcher.getCompletionSuggestions(parse, cursor);
        if (typingName) suggestions = withLegacyNames(player, text, cursor, suggestions);
        response.hasSuggestions = true;
        response.suggestionStart = suggestions.getRange().getStart();
        for (Suggestion suggestion : suggestions.getList()) {
            ITextComponent tooltip = suggestion.getTooltip() == null ? null : TextMessage.toComponent(suggestion.getTooltip());
            response.suggestions.add(new CommandSuggestResponse.Entry(suggestion.getRange().getStart(), suggestion.getRange().getEnd(), suggestion.getText(), tooltip));
        }

        // With the cursor at the end and nothing to suggest, the reasons it doesn't parse; "Unknown command" stands
        // for every command name that didn't match.
        if (cursor == text.length() && suggestions.isEmpty() && !parse.getExceptions().isEmpty()) {
            int unknownNames = 0;
            for (CommandSyntaxException exception : parse.getExceptions().values()) {
                if (exception.getType() == BuiltInExceptions.LITERAL_INCORRECT) {
                    unknownNames++;
                } else {
                    response.lines.add(describe(exception));
                }
            }
            if (unknownNames > 0) response.lines.add(describe(BuiltInExceptions.DISPATCHER_UNKNOWN_COMMAND.create()));
        }
        // Otherwise the usage of the arguments that can come next, lined up under where they start.
        if (response.lines.isEmpty()) {
            SuggestionContext<CommandSource> context;
            try {
                context = parse.getContext().findSuggestionContext(cursor);
            } catch (IllegalStateException e) {
                return;
            }
            for (Map.Entry<CommandNode<CommandSource>, String> usage : dispatcher.getSmartUsage(context.parent, source).entrySet()) {
                if (usage.getKey() instanceof LiteralCommandNode) continue;
                ITextComponent line = new TextComponentString(usage.getValue());
                line.getStyle().setColor(TextFormatting.GRAY);
                response.lines.add(line);
            }
            if (!response.lines.isEmpty()) response.usageStart = context.startPos;
        }
    }

    /** 1.13's text for a parse error: the message, then where it is ("... at position 5: /give <--[HERE]"). */
    private static ITextComponent describe(CommandSyntaxException exception) {
        ITextComponent message = TextMessage.toComponent(exception.getRawMessage()).createCopy();
        String context = exception.getContext();
        if (context == null) return message;
        return new TextComponentString("").appendSibling(message).appendText(" at position " + exception.getCursor() + ": " + context);
    }

    /** The names of other mods' 1.12 commands, which the 1.13 tree doesn't have, among the command name suggestions. */
    private static Suggestions withLegacyNames(EntityPlayerMP player, String text, int cursor, Suggestions suggestions) {
        String typed = text.substring(1, cursor);
        Set<String> modern = new LinkedHashSet<>();
        for (CommandNode<CommandSource> node : PortiumCommands.getDispatcher().getRoot().getChildren()) modern.add(node.getName());
        List<Suggestion> merged = new ArrayList<>(suggestions.getList());
        for (Map.Entry<String, ICommand> command : player.getServer().getCommandManager().getCommands().entrySet()) {
            String name = command.getKey();
            if (modern.contains(name) || !name.startsWith(typed) || !command.getValue().checkPermission(player.getServer(), player)) continue;
            merged.add(new Suggestion(StringRange.between(1, cursor), name));
        }
        return merged.size() == suggestions.getList().size() ? suggestions : Suggestions.create(text, merged);
    }

    /** A 1.12 command: its own tab completion for the word at the cursor, shown as a 1.13 suggestion list. */
    private static void completeLegacy(EntityPlayerMP player, CommandSuggestRequest request, CommandSuggestResponse response) {
        String before = request.text.substring(1, request.cursor);
        int wordStart = before.lastIndexOf(' ') + 2;
        response.hasSuggestions = true;
        response.suggestionStart = wordStart;
        Set<String> completions = new LinkedHashSet<>(player.getServer().getCommandManager().getTabCompletions(player, before, request.targetBlock));
        for (String completion : completions) {
            response.suggestions.add(new CommandSuggestResponse.Entry(wordStart, request.cursor, completion, null));
        }
    }

    private ChatSuggestions() {}
}
