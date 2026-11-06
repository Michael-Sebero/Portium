package com.michaelsebero.portium.item;

import net.minecraft.item.ItemRecord;
import net.minecraft.util.SoundEvent;

/** A 1.16 music disc (Pigstep). Its description comes from {@code item.record.<name>.desc}, as 1.12's discs. */
public class ItemMusicDisc116 extends ItemRecord {

    public ItemMusicDisc116(String name, SoundEvent sound) {
        super(name, sound);
    }
}
