package com.michaelsebero.portium.modern.mc.world;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.math.BlockPos;

/**
 * 1.13's {@code net.minecraft.world.ITickList}: a world's scheduled block or fluid ticks. On 1.12 both are the world's
 * scheduled block updates (see {@code FluidShims}).
 */
public interface ITickList<T> {

    @Srg("func_205359_a")
    boolean isTickScheduled(BlockPos pos, T target);

    @Srg("func_205360_a")
    default void scheduleTick(BlockPos pos, T target, int delay) {
        scheduleTick(pos, target, delay, null);
    }

    /** @param priority 1.13's {@code TickPriority}, a class of Minecraft 1.13.2's own; 1.12 has no priorities */
    @Srg("func_205362_a")
    void scheduleTick(BlockPos pos, T target, int delay, Object priority);

    @Srg("func_205361_b")
    boolean isTickPending(BlockPos pos, T target);
}
