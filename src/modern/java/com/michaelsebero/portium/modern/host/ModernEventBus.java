package com.michaelsebero.portium.modern.host;

import com.michaelsebero.portium.modern.ModernEventSupport;
import com.michaelsebero.portium.modern.ModernFailures;
import net.jodah.typetools.TypeResolver;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Forge 1.13's event bus API over one of Forge 1.12's event buses. Forge 1.13's event classes that 1.12 also has are
 * 1.12's for 1.13 mods (see {@code ModernMappings}), so a 1.13 mod's listener simply listens on the 1.12 bus: the main
 * bus for game events, or a bus of the mod's own for its loading events. {@code @SubscribeEvent} methods and lambda
 * listeners are found as Forge 1.13 finds them.
 */
public final class ModernEventBus implements IEventBus {

    private final Object bus;
    private final Map<Object, List<Object>> listeners = new ConcurrentHashMap<>();

    public ModernEventBus(Object bus) {
        this.bus = bus;
    }

    @Override
    public void register(Object target) {
        boolean statics = target instanceof Class;
        Class<?> type = statics ? (Class<?>) target : target.getClass();
        List<Object> added = new ArrayList<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) != statics || !method.isAnnotationPresent(SubscribeEvent.class)) continue;
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || !ModernEventSupport.isEvent(parameters[0])) {
                throw new IllegalArgumentException("@SubscribeEvent method " + method + " must take one event");
            }
            SubscribeEvent subscription = method.getAnnotation(SubscribeEvent.class);
            MethodHandle handle;
            try {
                method.setAccessible(true);
                handle = MethodHandles.lookup().unreflect(method);
            } catch (IllegalAccessException | SecurityException e) {
                throw new IllegalArgumentException("Can't call " + method, e);
            }
            MethodHandle invoker = statics ? handle : handle.bindTo(target);
            Listener listener = new Listener(method.getDeclaringClass().getName() + "." + method.getName(), event -> {
                try {
                    invoker.invoke(event);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
            added.add(ModernEventSupport.register(this.bus, parameters[0], subscription.priority().name(), subscription.receiveCanceled(),
                    genericFilter(method.getGenericParameterTypes()[0]), listener::invoke));
        }
        this.listeners.computeIfAbsent(target, key -> new ArrayList<>()).addAll(added);
    }

    /**
     * A 1.13 mod's event listener. One that fails (most often reaching what Portium doesn't provide of 1.13 yet, or
     * left without what its mod's failed setup would have made) is reported and switched off, rather than failing
     * whatever posted the event, every time.
     */
    private static final class Listener {

        private final String name;
        private final Consumer<Object> target;
        private volatile boolean off;

        Listener(String name, Consumer<Object> target) {
            this.name = name;
            this.target = target;
        }

        void invoke(Object event) {
            if (this.off) return;
            try {
                this.target.accept(event);
            } catch (LinkageError | RuntimeException e) {
                // Often a listener whose mod failed to set up, or one reaching what 1.12 lacks: the game goes on.
                this.off = true;
                ModernFailures.add("Event listener " + this.name + " for " + event.getClass().getSimpleName() + " was switched off", e);
            }
        }
    }

    /** For a generic event, the class its generic type argument names. */
    @Nullable
    private static Class<?> genericFilter(Type parameter) {
        if (!(parameter instanceof ParameterizedType)) return null;
        Type argument = ((ParameterizedType) parameter).getActualTypeArguments()[0];
        if (argument instanceof ParameterizedType) argument = ((ParameterizedType) argument).getRawType();
        return argument instanceof Class ? (Class<?>) argument : null;
    }

    @Override
    public <T extends Event> void addListener(Consumer<T> consumer) {
        addListener(EventPriority.NORMAL, consumer);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, Consumer<T> consumer) {
        addListener(priority, false, consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Consumer<T> consumer) {
        addListener(priority, receiveCanceled, (Class<T>) eventType(consumer), consumer);
    }

    @Override
    public <T extends Event> void addListener(EventPriority priority, boolean receiveCanceled, Class<T> type, Consumer<T> consumer) {
        add(consumer, type, priority, receiveCanceled, null);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> filter, Consumer<T> consumer) {
        addGenericListener(filter, EventPriority.NORMAL, consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> filter, EventPriority priority, Consumer<T> consumer) {
        addGenericListener(filter, priority, false, consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> filter, EventPriority priority, boolean receiveCanceled,
                                                                            Consumer<T> consumer) {
        addGenericListener(filter, priority, receiveCanceled, (Class<T>) eventType(consumer), consumer);
    }

    @Override
    public <T extends GenericEvent<? extends F>, F> void addGenericListener(Class<F> filter, EventPriority priority, boolean receiveCanceled,
                                                                            Class<T> type, Consumer<T> consumer) {
        add(consumer, type, priority, receiveCanceled, filter);
    }

    @SuppressWarnings("unchecked")
    private <T> void add(Consumer<T> consumer, Class<?> type, EventPriority priority, boolean receiveCanceled, @Nullable Class<?> filter) {
        Listener wrapped = new Listener(consumer.getClass().getName(), event -> consumer.accept((T) event));
        Object listener = ModernEventSupport.register(this.bus, type, priority.name(), receiveCanceled, filter, wrapped::invoke);
        this.listeners.computeIfAbsent(consumer, key -> new ArrayList<>()).add(listener);
    }

    /** The event type a lambda listener takes, from its generic signature, as Forge 1.13 works it out. */
    private static Class<?> eventType(Consumer<?> consumer) {
        Class<?> type = TypeResolver.resolveRawArgument(Consumer.class, consumer.getClass());
        if (type == TypeResolver.Unknown.class) throw new IllegalStateException("Can't tell which event " + consumer + " listens for");
        return type;
    }

    @Override
    public void unregister(Object target) {
        List<Object> removed = this.listeners.remove(target);
        if (removed == null) return;
        for (Object listener : removed) ModernEventSupport.unregister(this.bus, listener);
    }

    @Override
    public boolean post(Event event) {
        return ModernEventSupport.post(this.bus, event);
    }

    @Override
    public void shutdown() {}
}
