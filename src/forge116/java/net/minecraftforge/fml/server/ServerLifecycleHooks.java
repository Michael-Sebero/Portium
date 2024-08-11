package net.minecraftforge.fml.server;

import com.michaelsebero.portium.mods116.Mods116;
import net.minecraft.server.MinecraftServer;

/** Portium's replacement for Forge 1.13's: the running server is Forge 1.12's, which drives the server's stages. */
public class ServerLifecycleHooks {

    public static MinecraftServer getCurrentServer() {
        return Mods116.currentServer();
    }

    /** Forge 1.12 handles exits itself. */
    public static void handleExit(int status) {
        System.exit(status);
    }
}
