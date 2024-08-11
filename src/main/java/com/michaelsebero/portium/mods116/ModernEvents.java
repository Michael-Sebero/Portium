package com.michaelsebero.portium.mods116;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Passes Forge 1.12's loading events on to 1.13 mods' own buses. Game events need no passing on: 1.13 mods listen on
 * 1.12's main bus.
 */
public final class ModernEvents {

    private final IModernHost host;

    ModernEvents(IModernHost host) {
        this.host = host;
    }

    /** Each world tells Portium of entities leaving it, for Forge 1.16's capability invalidation listeners. */
    @SubscribeEvent
    public void worldLoad(net.minecraftforge.event.world.WorldEvent.Load event) {
        com.michaelsebero.portium.mods116.shim.CapabilityShims116.watch(event.getWorld());
    }

    @SubscribeEvent
    public void newRegistry(RegistryEvent.NewRegistry event) {
        ModernTileEntityTypes.create();
        ModernEntityTypes.create();
        Mods116.createRegistries();
    }

    /** Every registry's event: the raw parameter type makes Forge 1.12 pass all of them, not just one type's. */
    @SubscribeEvent
    @SuppressWarnings("rawtypes")
    public void register(RegistryEvent.Register event) {
        this.host.register(event.getName(), event.getRegistry());
    }
}
