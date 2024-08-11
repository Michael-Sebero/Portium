package com.michaelsebero.portium.loot;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.LootContext;

import javax.annotation.Nullable;

/**
 * Where loot is being generated. 1.13's loot context knows its position; 1.12's doesn't, so the container being
 * filled records it here while its loot table runs.
 */
public final class LootOrigin {

    private static final ThreadLocal<BlockPos> CONTAINER = new ThreadLocal<>();

    public static void set(@Nullable BlockPos pos) {
        if (pos == null) CONTAINER.remove();
        else CONTAINER.set(pos);
    }

    /** The container being filled, else the looted entity, else the player the loot is for. */
    @Nullable
    public static BlockPos of(LootContext context) {
        BlockPos container = CONTAINER.get();
        if (container != null) return container;
        Entity entity = context.getLootedEntity() != null ? context.getLootedEntity() : context.getKillerPlayer();
        return entity == null ? null : new BlockPos(entity);
    }

    private LootOrigin() {}
}
