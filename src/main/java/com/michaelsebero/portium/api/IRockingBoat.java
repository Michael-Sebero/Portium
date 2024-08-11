package com.michaelsebero.portium.api;

/**
 * Implemented on {@link net.minecraft.entity.item.EntityBoat} by mixin: boats rock on bubble columns before
 * being launched up or pulled under (1.13).
 */
public interface IRockingBoat {

    void portium$onBubbleColumnSurface(boolean drag);

    /** Current roll angle for rendering, interpolated between ticks. */
    float portium$getRockingAngle(float partialTicks);
}
