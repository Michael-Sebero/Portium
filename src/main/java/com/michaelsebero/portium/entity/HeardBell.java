package com.michaelsebero.portium.entity;

import net.minecraft.entity.EntityLivingBase;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.14's {@code HEARD_BELL_TIME} memory: when a mob within 32 blocks of a ringing bell last heard it (server side).
 * Like 1.14's, it isn't saved; villagers' AI reads it to run and hide.
 */
public final class HeardBell {

    private static final Map<EntityLivingBase, Long> HEARD = new WeakHashMap<>();

    public static void heard(EntityLivingBase entity, long time) {
        HEARD.put(entity, time);
    }

    /** The world time the mob last heard a bell, or -1. */
    public static long lastHeard(EntityLivingBase entity) {
        Long time = HEARD.get(entity);
        return time == null ? -1L : time;
    }

    private HeardBell() {}
}
