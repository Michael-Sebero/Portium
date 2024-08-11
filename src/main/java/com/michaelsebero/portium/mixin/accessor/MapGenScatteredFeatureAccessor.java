package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapGenScatteredFeature.class)
public interface MapGenScatteredFeatureAccessor {

    @Accessor("maxDistanceBetweenScatteredFeatures")
    int portium$getSpacing();
}
