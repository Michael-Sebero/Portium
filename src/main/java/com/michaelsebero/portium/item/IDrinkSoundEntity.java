package com.michaelsebero.portium.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;

/** A mob with its own drinking sounds (1.14's {@code getDrinkSound} override): the wandering trader. */
public interface IDrinkSoundEntity {

    SoundEvent portium$getDrinkSound(ItemStack stack);
}
