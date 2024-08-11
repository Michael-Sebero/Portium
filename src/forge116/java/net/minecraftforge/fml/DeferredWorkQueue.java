package net.minecraftforge.fml;

import net.minecraftforge.fml.event.lifecycle.ParallelDispatchEvent;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Portium's replacement for Forge 1.16's: the work mods queue during a loading stage's event ({@code enqueueWork}),
 * run in order on the loading thread once every mod has had the event. Forge's runs each piece inside the future it
 * hands back, where a failure is kept from everyone unless the mod looks; mods rarely look, so Portium reports any
 * failing work as a mod problem as well as failing the future.
 */
public class DeferredWorkQueue {

    private static final Map<Class<? extends ParallelDispatchEvent>, DeferredWorkQueue> QUEUES = new ConcurrentHashMap<>();

    private final ModLoadingStage stage;
    private final List<Runnable> tasks = new ArrayList<>();

    public DeferredWorkQueue(ModLoadingStage stage, Class<? extends ParallelDispatchEvent> event) {
        this.stage = stage;
        QUEUES.put(event, this);
    }

    /** The queue of the stage whose event is of this class. */
    public static Optional<DeferredWorkQueue> lookup(Optional<Class<? extends ParallelDispatchEvent>> event) {
        return event.map(QUEUES::get);
    }

    /** Runs what was queued, in order, and empties the queue. */
    void runTasks() {
        List<Runnable> queued;
        synchronized (this.tasks) {
            queued = new ArrayList<>(this.tasks);
            this.tasks.clear();
        }
        for (Runnable task : queued) task.run();
    }

    public CompletableFuture<Void> enqueueWork(IModInfo mod, Runnable work) {
        return enqueueWork(mod, () -> {
            work.run();
            return null;
        });
    }

    public <T> CompletableFuture<T> enqueueWork(IModInfo mod, Supplier<T> work) {
        CompletableFuture<T> done = new CompletableFuture<>();
        synchronized (this.tasks) {
            this.tasks.add(() -> {
                try {
                    done.complete(work.get());
                } catch (Throwable e) {
                    com.michaelsebero.portium.mods116.ModernFailures.add(mod.getDisplayName() + " (" + mod.getModId() + "): work queued during " + this.stage
                            + " failed", e);
                    done.completeExceptionally(e);
                }
            });
        }
        return done;
    }

    /** Forge's older way: work for after the active mod's current stage. */
    @Deprecated
    public static CompletableFuture<Void> runLater(Runnable work) {
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        DeferredWorkQueue queue = container.modLoadingStage.getDeferredWorkQueue();
        return queue == null ? CompletableFuture.completedFuture(null) : queue.enqueueWork(container.getModInfo(), work);
    }
}
