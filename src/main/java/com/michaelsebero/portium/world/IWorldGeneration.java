package com.michaelsebero.portium.world;

import javax.annotation.Nullable;

/** Which world generation a world uses, mixed into {@code WorldInfo} and {@code WorldSettings}. See {@link WorldGeneration}. */
public interface IWorldGeneration {

    /** One of {@link WorldGeneration}'s versions, or null for the configured default. */
    @Nullable
    String portium$getGeneration();

    void portium$setGeneration(@Nullable String generation);
}
