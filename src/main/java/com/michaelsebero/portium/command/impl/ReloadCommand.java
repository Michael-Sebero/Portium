package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.util.text.TextComponentTranslation;

import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code reload}: reloads 1.12's own data (advancements, loot tables, functions) and every data pack. */
public final class ReloadCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("reload").requires(source -> source.hasPermissionLevel(2)).executes(context -> {
            CommandSource source = context.getSource();
            source.sendFeedback(new TextComponentTranslation("commands.reload.success"), true);
            DataPackManager manager = DataPackManager.get();
            if (manager != null) manager.reload();
            else source.getServer().reload();
            return 0;
        }));
    }

    private ReloadCommand() {}
}
