package com.michaelsebero.portium.world.gen.structure;

import com.michaelsebero.portium.init.PortiumBiomes;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;

/** Which biomes 1.13's ocean structures use. */
final class OceanBiomes {

    static boolean isColdOrNeutral(Biome biome) {
        return biome == Biomes.OCEAN || biome == Biomes.DEEP_OCEAN || biome == PortiumBiomes.COLD_OCEAN || biome == PortiumBiomes.DEEP_COLD_OCEAN
                || biome == PortiumBiomes.FROZEN_OCEAN || biome == PortiumBiomes.DEEP_FROZEN_OCEAN;
    }

    static boolean isWarm(Biome biome) {
        return biome == PortiumBiomes.LUKEWARM_OCEAN || biome == PortiumBiomes.DEEP_LUKEWARM_OCEAN || biome == PortiumBiomes.WARM_OCEAN
                || biome == PortiumBiomes.DEEP_WARM_OCEAN;
    }

    static boolean isOcean(Biome biome) {
        return isColdOrNeutral(biome) || isWarm(biome);
    }

    static boolean isBeach(Biome biome) {
        return biome == Biomes.BEACH || biome == Biomes.COLD_BEACH;
    }

    private OceanBiomes() {}
}
