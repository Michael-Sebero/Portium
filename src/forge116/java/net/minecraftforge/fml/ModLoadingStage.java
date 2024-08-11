package net.minecraftforge.fml;

import com.michaelsebero.portium.mods116.Mods116;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Portium's replacement for Forge 1.16's: its loading stages, without Forge's own way of running them (Portium's glue
 * moves mods through them, see {@code ModernHost}). Each stage whose event lets mods queue work for after it has that
 * work queue, which the glue runs once every mod has had the event, as Forge does.
 */
public enum ModLoadingStage {
    ERROR(null),
    VALIDATE(null),
    CONSTRUCT(FMLConstructModEvent.class),
    CREATE_REGISTRIES(null),
    LOAD_REGISTRIES(null),
    COMMON_SETUP(FMLCommonSetupEvent.class),
    SIDED_SETUP(Mods116.isClient() ? FMLClientSetupEvent.class : FMLDedicatedServerSetupEvent.class),
    ENQUEUE_IMC(InterModEnqueueEvent.class),
    PROCESS_IMC(InterModProcessEvent.class),
    COMPLETE(FMLLoadCompleteEvent.class),
    DONE(null);

    @Nullable
    private final Class<? extends ParallelDispatchEvent> event;
    @Nullable
    private final DeferredWorkQueue queue;

    ModLoadingStage(@Nullable Class<? extends ParallelDispatchEvent> event) {
        this.event = event;
        this.queue = event == null ? null : new DeferredWorkQueue(this, event);
    }

    public Optional<Class<? extends ParallelDispatchEvent>> getParallelEventClass() {
        return Optional.ofNullable(this.event);
    }

    public DeferredWorkQueue getDeferredWorkQueue() {
        return this.queue;
    }

    /** For Portium's glue: runs the work mods queued during this stage. */
    void portium$runQueuedWork() {
        if (this.queue != null) this.queue.runTasks();
    }
}
