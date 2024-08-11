package com.michaelsebero.portium.advancements;

import net.minecraft.advancements.CriteriaTriggers;

/** The advancement triggers 1.13 added. */
public final class PortiumTriggers {

    public static final FilledBucketTrigger FILLED_BUCKET = CriteriaTriggers.register(new FilledBucketTrigger());
    public static final ChanneledLightningTrigger CHANNELED_LIGHTNING = CriteriaTriggers.register(new ChanneledLightningTrigger());
    public static final FishingRodHookedTrigger FISHING_ROD_HOOKED = CriteriaTriggers.register(new FishingRodHookedTrigger());

    /** Registers the triggers (by loading this class). */
    public static void init() {
        TagItemPredicate.register();
    }

    private PortiumTriggers() {}
}
