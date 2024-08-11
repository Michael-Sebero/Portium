package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;

/**
 * What a 1.16 mod's containers (menus) extend in place of 1.16's {@code Container}: 1.12's, made with 1.16's menu type
 * and window id. 1.16's container keeps its slots, window id and clicks where 1.12's does (the same SRG names); the
 * type, Minecraft 1.16.5's own class here, is kept loosely for {@code getType}.
 */
public abstract class Container116 extends Container {

    @Nullable
    private final Object type;

    protected Container116(@Nullable Object type, int windowId) {
        this.type = type;
        this.windowId = windowId;
    }

    /** 1.16's: the menu type it was made with (which the client opens the same menu by). */
    @Srg("func_216957_a")
    public Object getType() {
        if (this.type == null) throw new UnsupportedOperationException("Unable to construct this menu by type");
        return this.type;
    }
}
