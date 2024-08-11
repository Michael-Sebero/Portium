package com.michaelsebero.portium.modern.mc.init;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.fluid.Fluid;

/**
 * 1.13's {@code net.minecraft.init.Fluids}. The fluids are Minecraft 1.13.2's own classes, which only the 1.13 class
 * loader sees, so Portium's glue makes them and sets them here as 1.13 mods load. (1.13 types the four water and lava
 * fields as {@code FlowingFluid}; the translator casts what's read.)
 */
public final class Fluids {

    @Srg("field_204541_a")
    public static Fluid EMPTY;
    @Srg("field_207212_b")
    public static Fluid FLOWING_WATER;
    @Srg("field_204546_a")
    public static Fluid WATER;
    @Srg("field_207213_d")
    public static Fluid FLOWING_LAVA;
    @Srg("field_204547_b")
    public static Fluid LAVA;

    public static void portium$set(Fluid empty, Fluid flowingWater, Fluid water, Fluid flowingLava, Fluid lava) {
        EMPTY = empty;
        FLOWING_WATER = flowingWater;
        WATER = water;
        FLOWING_LAVA = flowingLava;
        LAVA = lava;
    }

    private Fluids() {}
}
