package net.minecraftforge.fml;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.lifecycle.IModBusEvent;

/**
 * Portium's: drives a Forge 1.16 mod container through a loading stage, reaching what only Forge's own package may
 * (the container's construction activity and its mod bus). Loaded with Forge 1.16 by the 1.16 mods' class loader, so
 * it shares that package. Forge 1.16 runs its stages through futures on worker threads; Portium runs them in order,
 * on the thread 1.12's stages run on, mod by mod.
 */
public final class ModernLifecycle {

    /** Makes the mod's instance, as Forge's construction stage does. */
    public static void construct(ModContainer container) {
        run(container, () -> {
            Runnable activity = container.activityMap.get(ModLoadingStage.CONSTRUCT);
            if (activity != null) activity.run();
        });
    }

    /** Posts a loading event to the mod's own bus, the mod made the active one as it would be in Forge. */
    public static void post(ModContainer container, Event event, ModLoadingStage stage) {
        run(container, () -> {
            container.modLoadingStage = stage;
            accept(container, event);
        });
    }

    /** Runs the work mods queued for after a stage, once every mod has had its event. */
    public static void finish(ModLoadingStage stage) {
        stage.portium$runQueuedWork();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event & IModBusEvent> void accept(ModContainer container, Event event) {
        container.acceptEvent((T) event);
    }

    private static void run(ModContainer container, Runnable action) {
        ModLoadingContext context = ModLoadingContext.get();
        context.setActiveContainer(container, container.contextExtension == null ? null : container.contextExtension.get());
        try {
            action.run();
        } finally {
            context.setActiveContainer(null, null);
        }
    }

    private ModernLifecycle() {}
}
