package net.minecraftforge.eventbus.api;

import com.michaelsebero.portium.modern.ModernEventSupport;
import com.michaelsebero.portium.modern.host.ModernEventBus;

/**
 * Portium's replacement for Forge 1.13's: builds a bus over a new Forge 1.12 event bus (see {@link ModernEventBus}).
 * Exception handlers and phase tracking are 1.12's own.
 */
public final class BusBuilder {

    private IEventExceptionHandler exceptionHandler;
    private boolean trackPhases = true;

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

    public IEventBus build() {
        return new ModernEventBus(ModernEventSupport.newBus());
    }
}
