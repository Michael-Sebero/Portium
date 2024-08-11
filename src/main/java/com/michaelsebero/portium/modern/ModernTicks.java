package com.michaelsebero.portium.modern;

import net.minecraft.tileentity.TileEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Ticking of newer mods' tile entities, kept from crashing the game: a tick that reaches a part of newer Minecraft
 * that Portium doesn't cover yet (a missing method or field) switches ticking off for that kind of tile entity on
 * that side, logged once, as Portium does for event listeners. Other errors are the mod's own and crash as usual.
 */
public final class ModernTicks {

    private static final Logger LOGGER = LogManager.getLogger("Portium/Mods");
    private static final Set<String> OFF = Collections.synchronizedSet(new HashSet<>());

    private static String key(TileEntity tile) {
        boolean client = tile.getWorld() != null && tile.getWorld().isRemote;
        return (client ? "client:" : "server:") + tile.getClass().getName();
    }

    public static boolean isOff(TileEntity tile) {
        return !OFF.isEmpty() && OFF.contains(key(tile));
    }

    public static void switchOff(TileEntity tile, LinkageError error) {
        String key = key(tile);
        if (OFF.add(key)) {
            LOGGER.error("Mod problem: ticking of {} on the {} was switched off; it needs something Portium doesn't provide yet",
                    tile.getClass().getName(), key.startsWith("client") ? "client" : "server", error);
        }
    }

    private ModernTicks() {}
}
