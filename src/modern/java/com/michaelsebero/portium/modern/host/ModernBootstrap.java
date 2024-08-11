package com.michaelsebero.portium.modern.host;

import com.michaelsebero.portium.modern.mc.fluid.Fluid;
import com.michaelsebero.portium.modern.mc.init.Fluids;
import com.michaelsebero.portium.modern.mc.util.registry.IRegistry;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.util.ResourceLocation;

/**
 * Makes the game objects 1.13 has and 1.12 doesn't, which are Minecraft 1.13.2's own classes: as 1.13's bootstrap
 * does before mods load. Portium's stand-ins hold them, since Portium's own classes can't name Minecraft 1.13.2's.
 */
final class ModernBootstrap {

    static void run() {
        // Compiled against 1.13's fluid classes, which extend Portium's Fluid once translated.
        Fluids.portium$set(fluid(new EmptyFluid()), fluid(new WaterFluid.Flowing()), fluid(new WaterFluid.Source()),
                fluid(new LavaFluid.Flowing()), fluid(new LavaFluid.Source()));
        IRegistry<Fluid> fluids = IRegistry.FLUID;
        fluids.put(new ResourceLocation("empty"), Fluids.EMPTY);
        fluids.put(new ResourceLocation("flowing_water"), Fluids.FLOWING_WATER);
        fluids.put(new ResourceLocation("water"), Fluids.WATER);
        fluids.put(new ResourceLocation("flowing_lava"), Fluids.FLOWING_LAVA);
        fluids.put(new ResourceLocation("lava"), Fluids.LAVA);
    }

    private static Fluid fluid(Object fluid) {
        return (Fluid) fluid;
    }

    private ModernBootstrap() {}
}
