package com.michaelsebero.portium.entity;

import net.minecraft.util.SoundEvent;

/** What a crossbow sets on the arrows it fires (1.14): piercing, the sound of its hit, that a crossbow shot it. */
public interface ICrossbowArrow {

    void portium$setPierceLevel(byte level);

    byte portium$getPierceLevel();

    void portium$setShotFromCrossbow(boolean shot);

    boolean portium$isShotFromCrossbow();

    void portium$setHitSound(SoundEvent sound);
}
