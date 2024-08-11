package com.michaelsebero.portium.mods116.shim;

import com.michaelsebero.portium.mods116.adapter.Container116;

/** 1.16's container methods for the 1.16 glue, which can't name 1.12's classes (so these take objects). */
public final class ContainerShims116 {

    /** A 1.16 mod's container's menu type; 1.12's own containers have none. */
    public static Object type(Object container) {
        if (container instanceof Container116) return ((Container116) container).getType();
        throw new UnsupportedOperationException("Unable to construct this menu by type: " + container.getClass().getName() + " is 1.12's");
    }

    private ContainerShims116() {}
}
