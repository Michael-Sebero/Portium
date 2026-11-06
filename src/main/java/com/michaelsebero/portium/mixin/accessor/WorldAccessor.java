package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(World.class)
public interface WorldAccessor {

    /** Whether the world spawns monsters (off in peaceful single player, or by the server's spawn-monsters setting). */
    @Accessor("spawnHostileMobs")
    boolean portium$spawnsHostileMobs();
}
