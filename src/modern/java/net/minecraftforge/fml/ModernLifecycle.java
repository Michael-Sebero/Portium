package net.minecraftforge.fml;

import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Portium's: makes Forge 1.13's loading stage events, whose constructor only Forge's own package may call. Loaded
 * with Forge 1.13 by the 1.13 mods' class loader, so it shares that package.
 */
public final class ModernLifecycle {

    /** The event that moves mods through {@code stage}; {@code advance} false keeps them in it, for repeated events. */
    public static LifecycleEventProvider.LifecycleEvent event(ModLoadingStage stage, @Nullable Supplier<Event> event, boolean advance) {
        LifecycleEventProvider.LifecycleEvent lifecycle = new LifecycleEventProvider.LifecycleEvent(stage,
                advance ? LifecycleEventProvider.LifecycleEvent.Progression.NEXT : LifecycleEventProvider.LifecycleEvent.Progression.STAY);
        if (event != null) lifecycle.setCustomEventSupplier(event);
        return lifecycle;
    }

    private ModernLifecycle() {}
}
