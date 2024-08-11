package net.minecraftforge.fml.event.server;

import com.michaelsebero.portium.modern.host.ModernCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;

/**
 * Portium's replacement for Forge 1.13's: commands registered here run on Mojang's Brigadier and are typed and
 * suggested through Portium's 1.13 commands (see {@link ModernCommands}).
 */
public class FMLServerStartingEvent extends ServerLifecycleEvent {

    public FMLServerStartingEvent(MinecraftServer server) {
        super(server);
    }

    public CommandDispatcher<CommandSource> getCommandDispatcher() {
        return ModernCommands.dispatcher();
    }
}
