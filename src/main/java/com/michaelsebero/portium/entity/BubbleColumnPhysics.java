package com.michaelsebero.portium.entity;

import com.michaelsebero.portium.api.IRockingBoat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;

/**
 * How bubble columns move entities (1.13 {@code Entity.onEnterBubbleColumn*}).
 */
public final class BubbleColumnPhysics {

    /** The entity is in the top block of a column, with air above. Boats rock instead of being launched straight away. */
    public static void onSurface(Entity entity, boolean drag) {
        if (entity instanceof EntityBoat) {
            if (com.michaelsebero.portium.PortiumConfig.mechanics.bubbleColumnBoats) ((IRockingBoat) entity).portium$onBubbleColumnSurface(drag);
            return;
        }
        if (drag) {
            entity.motionY = Math.max(-0.9, entity.motionY - 0.03);
        } else {
            entity.motionY = Math.min(1.8, entity.motionY + 0.1);
        }
    }

    public static void inside(Entity entity, boolean drag) {
        if (drag) {
            entity.motionY = Math.max(-0.3, entity.motionY - 0.03);
        } else {
            entity.motionY = Math.min(0.7, entity.motionY + 0.06);
        }
        entity.fallDistance = 0.0F;
    }

    private BubbleColumnPhysics() {}
}
