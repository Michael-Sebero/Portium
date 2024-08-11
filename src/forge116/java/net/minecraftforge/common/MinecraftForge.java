package net.minecraftforge.common;

import com.michaelsebero.portium.mods116.ModernEventSupport;
import com.michaelsebero.portium.mods116.host.ModernEventBus;
import net.minecraftforge.eventbus.api.IEventBus;

/** Portium's replacement for Forge 1.13's: its event bus is Forge 1.12's main bus (see {@link ModernEventBus}). */
public class MinecraftForge {

    public static final IEventBus EVENT_BUS = new ModernEventBus(ModernEventSupport.forgeBus());

    /** Forge 1.12 has done this already. */
    public static void initialize() {}
}
