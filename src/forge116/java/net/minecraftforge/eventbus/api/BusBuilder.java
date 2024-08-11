package net.minecraftforge.eventbus.api;

import com.michaelsebero.portium.mods116.ModernEventSupport;
import com.michaelsebero.portium.mods116.host.ModernEventBus;

/**
 * Portium's replacement for Forge 1.16's: builds a bus over a new Forge 1.12 event bus (see {@link ModernEventBus}).
 * Exception handlers and phase tracking are 1.12's own.
 */
public final class BusBuilder {

    private IEventExceptionHandler exceptionHandler;
    private boolean trackPhases = true;
    private Class<?> markerType = Object.class;

    public static BusBuilder builder() {
        return new BusBuilder();
    }

    public BusBuilder setTrackPhases(boolean trackPhases) {
        this.trackPhases = trackPhases;
        return this;
    }

    public BusBuilder setExceptionHandler(IEventExceptionHandler handler) {
        this.exceptionHandler = handler;
        return this;
    }

    public IEventExceptionHandler getExceptionHandler() {
        return this.exceptionHandler;
    }

    public boolean getTrackPhases() {
        return this.trackPhases;
    }

    /** Forge 1.16 limits a mod bus to its mod events; 1.12's bus takes any, so this only records the type. */
    public BusBuilder markerType(Class<?> type) {
        this.markerType = type;
        return this;
    }

    public Class<?> getMarkerType() {
        return this.markerType;
    }

    public BusBuilder startShutdown() {
        return this;
    }

    public boolean isStartingShutdown() {
        return false;
    }

    public IEventBus build() {
        return new ModernEventBus(ModernEventSupport.newBus());
    }
}
