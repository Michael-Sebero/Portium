package com.michaelsebero.portium.modern;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.IGenericEvent;
import net.minecraftforge.fml.common.eventhandler.ListenerList;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Forge 1.12's event buses, for the 1.13 mods' event bus ({@code modern.host.ModernEventBus}). 1.13 mods see Forge
 * 1.12's event classes as their own, so their listeners go straight onto 1.12's buses: the main bus for game events,
 * a bus of its own for each mod's loading events. Types are plain objects here because the glue is compiled against
 * Forge 1.13, which has no such classes.
 */
public final class ModernEventSupport {

    private static final Field BUS_ID = busIdField();

    /** Forge 1.12's main event bus. */
    public static Object forgeBus() {
        return MinecraftForge.EVENT_BUS;
    }

    public static Object newBus() {
        return new EventBus();
    }

    /**
     * Adds a listener for an event class, and its subclasses, to a bus. Returns the listener, for
     * {@link #unregister}.
     *
     * @param priority the name of an {@link EventPriority}
     * @param filter   for generic events, the generic type the listener takes; null for any
     */
    public static Object register(Object bus, Class<?> eventType, String priority, boolean receiveCanceled, @Nullable Class<?> filter,
                                  Consumer<Object> handler) {
        IEventListener listener = event -> {
            if (!receiveCanceled && event.isCancelable() && event.isCanceled()) return;
            if (filter != null && (!(event instanceof IGenericEvent) || ((IGenericEvent<?>) event).getGenericType() != filter)) return;
            handler.accept(event);
        };
        listenerList(eventType).register(busId(bus), EventPriority.valueOf(priority), listener);
        return listener;
    }

    public static void unregister(Object bus, Object listener) {
        ListenerList.unregisterAll(busId(bus), (IEventListener) listener);
    }

    public static boolean post(Object bus, Object event) {
        return ((EventBus) bus).post((Event) event);
    }

    /** Whether a class is one of Forge 1.12's events. */
    public static boolean isEvent(Class<?> type) {
        return Event.class.isAssignableFrom(type);
    }

    /** An event class's listener list, found as Forge 1.12 does: through an instance made with its bare constructor. */
    private static ListenerList listenerList(Class<?> eventType) {
        try {
            Constructor<?> constructor = eventType.getConstructor();
            constructor.setAccessible(true);
            return ((Event) constructor.newInstance()).getListenerList();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Can't listen for " + eventType.getName() + ": it has no bare constructor", e);
        }
    }

    private static int busId(Object bus) {
        try {
            return BUS_ID.getInt(bus);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field busIdField() {
        try {
            Field field = EventBus.class.getDeclaredField("busID");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private ModernEventSupport() {}
}
