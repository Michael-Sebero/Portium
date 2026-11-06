package com.michaelsebero.portium.item;

import net.minecraft.util.SoundEvent;

/** An item drunk with its own sound (1.15's {@code Item.getDrinkSound}), instead of 1.12's one drinking sound. */
public interface ICustomDrinkSound {

    SoundEvent getDrinkSound();
}
