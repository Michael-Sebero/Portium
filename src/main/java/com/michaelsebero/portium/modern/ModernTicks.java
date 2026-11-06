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

    /**
     * Whether an exception thrown in a newer mod's code looks like Portium's gap rather than the mod's bug: a value of
     * the wrong class where 1.12's and the newer game's types meet, or anything thrown from Portium's own stand-ins
     * and bridges (their frames are near the top).
     */
    public static boolean isLayerFault(Throwable error) {
        if (error instanceof ClassCastException || error instanceof UnsupportedOperationException) return true;
        StackTraceElement[] frames = error.getStackTrace();
        for (int i = 0; i < Math.min(6, frames.length); i++) {
            String name = frames[i].getClassName();
            if (name.contains("PortiumBridge") || name.startsWith("com.michaelsebero.portium.") || name.startsWith("mc116.") || name.startsWith("forge116.")) return true;
        }
        return false;
    }

    public static void switchOff(TileEntity tile, Throwable error) {
        String key = key(tile);
        if (OFF.add(key)) {
            LOGGER.error("Mod problem: ticking of {} on the {} was switched off; it needs something Portium doesn't provide yet",
                    tile.getClass().getName(), key.startsWith("client") ? "client" : "server", error);
            com.michaelsebero.portium.diagnostics.ProblemReport.record("Ticking of " + tile.getClass().getName() + " on the "
                    + (key.startsWith("client") ? "client" : "server") + " was switched off", error);
        }
    }

    /** Whether a newer mod's block, entity class or the like was switched off (on either side). */
    public static boolean isOff(Object thing) {
        return !OFF.isEmpty() && OFF.contains("any:" + thing.getClass().getName());
    }

    /** Switches a newer mod's block's ticks, or an entity class's updates, off: logged and reported once. */
    public static void switchOff(Object thing, Throwable error) {
        String name = thing.getClass().getName();
        if (OFF.add("any:" + name)) {
            LOGGER.error("Mod problem: ticking of {} was switched off; it needs something Portium doesn't provide yet", name, error);
            com.michaelsebero.portium.diagnostics.ProblemReport.record("Ticking of " + name + " was switched off", error);
        }
    }

    private ModernTicks() {}
}
