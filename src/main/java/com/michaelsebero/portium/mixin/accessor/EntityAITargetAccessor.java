package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.entity.ai.EntityAITarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityAITarget.class)
public interface EntityAITargetAccessor {

    @Accessor("shouldCheckSight")
    boolean portium$getShouldCheckSight();

    @Accessor("nearbyOnly")
    boolean portium$getNearbyOnly();
}
