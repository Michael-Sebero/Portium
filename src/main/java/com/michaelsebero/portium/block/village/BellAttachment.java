package com.michaelsebero.portium.block.village;

import net.minecraft.util.IStringSerializable;

/** How a bell hangs (1.14's {@code BellAttachment}). */
public enum BellAttachment implements IStringSerializable {
    FLOOR("floor"),
    CEILING("ceiling"),
    SINGLE_WALL("single_wall"),
    DOUBLE_WALL("double_wall");

    private final String name;

    BellAttachment(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
