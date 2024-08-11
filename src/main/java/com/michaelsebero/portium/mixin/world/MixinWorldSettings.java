package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.IWorldGeneration;
import net.minecraft.world.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import javax.annotation.Nullable;

/** Carries the world generation chosen on the Create World screen to the new world's level.dat. */
@Mixin(WorldSettings.class)
public abstract class MixinWorldSettings implements IWorldGeneration {

    @Unique
    @Nullable
    private String portium$generation;

    @Nullable
    @Override
    public String portium$getGeneration() {
        return this.portium$generation;
    }

    @Override
    public void portium$setGeneration(@Nullable String generation) {
        this.portium$generation = generation;
    }
}
