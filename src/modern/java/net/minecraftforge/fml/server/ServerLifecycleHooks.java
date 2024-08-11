package net.minecraftforge.fml.server;

import com.michaelsebero.portium.modern.ModernMods;
import net.minecraft.server.MinecraftServer;

/** Portium's replacement for Forge 1.13's: the running server is Forge 1.12's, which drives the server's stages. */
public class ServerLifecycleHooks {

    public static MinecraftServer getCurrentServer() {
        return ModernMods.currentServer();
    }

    /** Forge 1.12 handles exits itself. */
    public static void handleExit(int status) {
        System.exit(status);
    }
}
