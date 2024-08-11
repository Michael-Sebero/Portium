package net.minecraftforge.fml;

import net.minecraftforge.eventbus.api.Event;

/**
 * Portium's replacement for Forge 1.16's mod loader: Forge 1.12 loads the game, so this only does what mods ask of
 * it, handing an event to every 1.16 mod's event bus.
 */
public class ModLoader {

    private static final ModLoader INSTANCE = new ModLoader();

    public static ModLoader get() {
        return INSTANCE;
    }

    /** Posts an event to each 1.16 mod's own event bus. */
    public void postEvent(Event event) {
        ModList.get().forEachModContainer((id, container) -> container.acceptEvent((Event & net.minecraftforge.fml.event.lifecycle.IModBusEvent) event));
    }

    public static boolean isLoadingStateValid() {
        return true;
    }

    private ModLoader() {}
}
