package com.michaelsebero.portium.mixin.client;

import net.minecraft.world.storage.WorldSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldSummary.class)
public interface WorldSummaryAccessor {

    @Accessor("versionId")
    int portium$getVersionId();
}
