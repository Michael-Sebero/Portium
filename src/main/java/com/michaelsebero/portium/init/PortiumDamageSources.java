package com.michaelsebero.portium.init;

import net.minecraft.util.DamageSource;

public final class PortiumDamageSources {

    /** Dolphins drying out on land (1.13). */
    public static final DamageSource DRYOUT = new DamageSource("dryout");

    /** Moving inside a grown sweet berry bush (1.14). */
    public static final DamageSource SWEET_BERRY_BUSH = new DamageSource("sweetBerryBush");

    private PortiumDamageSources() {}
}
