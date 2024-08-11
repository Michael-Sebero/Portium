package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.arguments.FunctionArgument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.datapack.CommandFunction;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Collection;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code function <name|#tag>}: runs data pack functions. */
public final class FunctionCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        // Absent while data packs, which hold the functions, are switched off in Portium's config.
        dispatcher.register(literal("function").requires(source -> source.hasPermissionLevel(2) && DataPackManager.get() != null)
                .then(argument("name", FunctionArgument.function()).executes(context ->
                        run(context.getSource(), FunctionArgument.getFunctions(context, "name")))));
    }

    private static int run(CommandSource source, Collection<CommandFunction> functions) {
        DataPackManager manager = DataPackManager.get();
        int commands = 0;
        for (CommandFunction function : functions) {
            commands += manager.getFunctions().execute(function, source.withFeedbackDisabled().withMinPermissionLevel(2));
        }
        if (functions.size() == 1) {
            source.sendFeedback(new TextComponentTranslation("commands.function.success.single", commands, functions.iterator().next().getId().toString()), true);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.function.success.multiple", commands, functions.size()), true);
        }
        return commands;
    }

    private FunctionCommand() {}
}
