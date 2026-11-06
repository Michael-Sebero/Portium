package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.structure.MapGenVillage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkGeneratorOverworld.class)
public interface ChunkGeneratorOverworldAccessor {

    @Accessor("villageGenerator")
    MapGenVillage portium$getVillageGenerator();
}
