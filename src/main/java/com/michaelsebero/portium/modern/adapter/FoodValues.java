package com.michaelsebero.portium.modern.adapter;

import java.util.List;

/**
 * 1.14's food, as item properties carry it: how much it feeds, whether it's meat (for wolves), whether it's eaten
 * when full and quickly, and the effects it may give, each with its chance.
 */
public final class FoodValues {

    public final int hunger;
    public final float saturation;
    public final boolean meat;
    public final boolean alwaysEdible;
    public final boolean fast;
    /** 1.12's potion effects ({@code PotionEffect}), typed loosely for the glue, which compiles against 1.16. */
    public final List<?> effects;
    public final List<Float> chances;

    public FoodValues(int hunger, float saturation, boolean meat, boolean alwaysEdible, boolean fast, List<?> effects, List<Float> chances) {
        this.hunger = hunger;
        this.saturation = saturation;
        this.meat = meat;
        this.alwaysEdible = alwaysEdible;
        this.fast = fast;
        this.effects = effects;
        this.chances = chances;
    }
}
