package com.michaelsebero.portium.modern.host;

import com.michaelsebero.portium.command.ExternalCommand;
import com.michaelsebero.portium.command.PortiumCommands;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.ITextComponent;

import java.util.function.BiConsumer;

/**
 * 1.13 mods' commands: registered with Mojang's Brigadier, as in 1.13, and handed to Portium's command dispatcher one
 * name at a time, so they're typed, suggested and permission-checked like the game's own 1.13 commands. (1.13's
 * command source is Portium's.)
 */
public final class ModernCommands {

    private static CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();

    public static CommandDispatcher<CommandSource> dispatcher() {
        return dispatcher;
    }

    /** A new server's commands start afresh. */
    static void reset() {
        dispatcher = new CommandDispatcher<>();
    }

    /** Hands the commands 1.13 mods registered to Portium's dispatcher. */
    static void forward() {
        CommandDispatcher<CommandSource> commands = dispatcher;
        for (CommandNode<CommandSource> node : commands.getRoot().getChildren()) {
            PortiumCommands.registerExternal(node.getName(), new ExternalCommand() {
                @Override
                public boolean canUse(com.michaelsebero.portium.command.CommandSource source) {
                    return node.canUse(modern(source));
                }

                @Override
                public int run(com.michaelsebero.portium.command.CommandSource source, String input)
                        throws com.michaelsebero.portium.command.brigadier.CommandSyntaxException {
                    try {
                        return commands.execute(input, modern(source));
                    } catch (LinkageError e) {
                        // What 1.12 can't run of a 1.13 command fails the command, not the server.
                        throw new IllegalStateException("1.12 can't run this command: " + e, e);
                    } catch (CommandSyntaxException e) {
                        com.michaelsebero.portium.command.brigadier.Message message = text(e.getRawMessage());
                        throw new com.michaelsebero.portium.command.brigadier.CommandSyntaxException(new CommandExceptionType.Simple(message), message,
                                e.getInput(), e.getCursor());
                    }
                }

                @Override
                public void suggest(com.michaelsebero.portium.command.CommandSource source, String input, BiConsumer<Integer, String> suggestion) {
                    ParseResults<CommandSource> parse = commands.parse(input, modern(source));
                    for (Suggestion each : commands.getCompletionSuggestions(parse).join().getList()) {
                        suggestion.accept(each.getRange().getStart(), each.getText());
                    }
                }
            });
        }
    }

    /** Portium's command source is 1.13's, once 1.13's name is translated. */
    private static CommandSource modern(Object source) {
        return (CommandSource) source;
    }

    private static com.michaelsebero.portium.command.brigadier.Message text(com.mojang.brigadier.Message message) {
        String text = getString(message);
        return () -> text;
    }

    /**
     * Brigadier's {@code Message.getString()}, for any message: 1.13's text components are messages, 1.12's aren't,
     * so calls to it in 1.13 code and in Brigadier come here.
     */
    public static String getString(com.mojang.brigadier.Message message) {
        if (message == null) return "";
        if ((Object) message instanceof ITextComponent) return ((ITextComponent) (Object) message).getString();
        try {
            // Reflectively: a call to Message.getString here would come back here.
            return (String) message.getClass().getMethod("getString").invoke(message);
        } catch (ReflectiveOperationException e) {
            return message.toString();
        }
    }

    private ModernCommands() {}
}
