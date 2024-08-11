package com.michaelsebero.portium.mods116.host;

import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;

/** 1.16's container methods asked of any container (1.12's class here), which a 1.16 mod's answers itself. */
public final class ContainerGlue {

    /** {@code container.getType()}: a 1.16 mod's menu type; 1.12's own containers have none. */
    public static ContainerType<?> getType(Container container) {
        return (ContainerType<?>) com.michaelsebero.portium.mods116.shim.ContainerShims116.type(container);
    }

    private ContainerGlue() {}
}
