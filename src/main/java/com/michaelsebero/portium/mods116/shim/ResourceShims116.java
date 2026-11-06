package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.modern.mc.resources.IReloadableResourceManager;
import com.michaelsebero.portium.modern.mc.resources.IResourceManagerReloadListener;
import org.apache.logging.log4j.LogManager;

/** 1.16's resource reloading onto Portium's (1.13's) reload listeners, typed loosely (1.16's listener type is its own). */
public final class ResourceShims116 {

    /**
     * {@code manager.addReloadListener(listener)}: 1.16's listeners that reload as 1.13's do (most mods', through
     * {@code IResourceManagerReloadListener}) reload with 1.12's resources; 1.16's staged ones aren't run yet.
     */
    public static void addReloadListener(Object manager, Object listener) {
        // The server's data: run with the other data listeners, now (1.16 adds them before its first load) and at each reload.
        if (manager instanceof com.michaelsebero.portium.mods116.DataResources116) {
            ((com.michaelsebero.portium.mods116.DataResources116) manager).addListener(listener);
            return;
        }
        if (listener instanceof IResourceManagerReloadListener) {
            IResourceManagerReloadListener reloads = (IResourceManagerReloadListener) listener;
            // A mod's reload failing (1.12 can't do what it asks yet) is the mod's, not the game's.
            IResourceManagerReloadListener guarded = resources -> {
                try {
                    reloads.onResourceManagerReload(resources);
                } catch (RuntimeException | LinkageError e) {
                    LogManager.getLogger("Portium/1.16.5").error("1.16 mod problem: resource reload listener {} failed", listener, e);
                }
            };
            // 1.16 runs a listener at the next reload, not when it's added.
            if (manager instanceof com.michaelsebero.portium.modern.mc.resources.ModernResources) {
                ((com.michaelsebero.portium.modern.mc.resources.ModernResources) manager).addReloadListenerLater(guarded);
            } else {
                ((IReloadableResourceManager) manager).addReloadListener(guarded);
            }
        } else {
            // 1.16's staged listeners: prepared and applied in one go, with 1.12's resources, whenever 1.12 reloads them.
            IResourceManagerReloadListener staged = resources -> com.michaelsebero.portium.mods116.Mods116.postEvent("reload_listener", listener, resources,
                    net.minecraft.client.Minecraft.getMinecraft().profiler);
            if (manager instanceof com.michaelsebero.portium.modern.mc.resources.ModernResources) {
                ((com.michaelsebero.portium.modern.mc.resources.ModernResources) manager).addReloadListenerLater(staged);
            } else if (manager instanceof IReloadableResourceManager) {
                ((IReloadableResourceManager) manager).addReloadListener(staged);
            }
        }
    }

    private ResourceShims116() {}
}
