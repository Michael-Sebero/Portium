package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.DataResources116;
import com.michaelsebero.portium.mods116.ModernFailures;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.Unit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 1.16's data reload for 1.16 mods: Forge's event collecting their reload listeners, and the listeners run, each to
 * its end before the next, over the data packs Portium has loaded. 1.16 prepares them side by side off the server
 * thread; here the server waits for them, as 1.12 loads everything.
 */
public final class ReloadGlue {

    private static final Executor DIRECT = Runnable::run;
    private static DataPackRegistries registries;

    /** The server's data registries, as far as 1.16 mods ask them: its recipes and resources. */
    static synchronized DataPackRegistries registries() {
        if (registries == null) {
            try {
                registries = WorldGenGlue.allocate(DataPackRegistries.class);
                set("field_240954_d_", RecipeManager116.game());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return registries;
    }

    public static DataPackRegistries registries(Object server) {
        return registries();
    }

    private static void set(String name, Object value) throws ReflectiveOperationException {
        Field field = DataPackRegistries.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(registries, value);
    }

    /** Portium loaded the data packs (again). */
    static void reload(Object resources, Object profiler) {
        DataResources116 data = (DataResources116) resources;
        List<Object> listeners = new ArrayList<>();
        try {
            DataPackRegistries all = registries();
            set("field_240952_b_", resources);
            AddReloadListenerEvent event = new AddReloadListenerEvent(all);
            MinecraftForge.EVENT_BUS.post(event);
            for (IFutureReloadListener listener : event.getListeners()) listeners.add(unwrap(listener));
        } catch (Throwable e) {
            ModernFailures.add("Forge 1.16's reload listener event couldn't be posted, so mods' own data isn't loaded", e);
        }
        listeners.addAll(data.listeners());
        for (Object listener : listeners) run(listener, resources, profiler);
    }

    /** Forge wraps each listener to skip it when mod loading failed; Portium runs what loaded. */
    private static Object unwrap(IFutureReloadListener listener) {
        try {
            Field wrapped = listener.getClass().getDeclaredField("wrapped");
            wrapped.setAccessible(true);
            Object inner = wrapped.get(listener);
            return inner == null ? listener : inner;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return listener;
        }
    }

    /** One listener, prepared and applied on the calling thread. */
    static void run(Object listener, Object resources, Object profiler) {
        try {
            // 1.13's kind of listener (Portium's stand-in for 1.16's simple one) reloads in one step.
            if (listener instanceof IResourceManagerReloadListener && !(listener instanceof net.minecraft.client.resources.ReloadListener)) {
                ((IResourceManagerReloadListener) listener).func_195410_a((IResourceManager) resources);
                return;
            }
            IFutureReloadListener.IStage stage = new IFutureReloadListener.IStage() {
                @Override
                public <T> CompletableFuture<T> func_216872_a(T prepared) {
                    return CompletableFuture.completedFuture(prepared);
                }
            };
            CompletableFuture<Void> done = ((IFutureReloadListener) listener).func_215226_a(stage, (IResourceManager) resources, (IProfiler) profiler,
                    (IProfiler) profiler, DIRECT, DIRECT);
            if (done != null) done.join();
        } catch (Throwable e) {
            Throwable cause = e instanceof java.util.concurrent.CompletionException && e.getCause() != null ? e.getCause() : e;
            ModernFailures.add("The reload listener " + listener.getClass().getName() + " failed, so what it loads from data or resource packs is missing", cause);
        }
    }

    private ReloadGlue() {}
}
