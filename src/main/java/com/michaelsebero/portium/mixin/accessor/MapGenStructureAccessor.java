package com.michaelsebero.portium.mixin.accessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapGenStructure.class)
public interface MapGenStructureAccessor {

    @Accessor("structureMap")
    Long2ObjectMap<StructureStart> portium$getStructureMap();
}
