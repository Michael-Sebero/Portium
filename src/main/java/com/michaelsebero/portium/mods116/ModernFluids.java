package com.michaelsebero.portium.mods116;

import com.michaelsebero.portium.modern.mc.fluid.Fluid;
import com.michaelsebero.portium.modern.mc.init.Fluids;
import com.michaelsebero.portium.modern.mc.util.registry.IRegistry;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * 1.16's vanilla fluids: Minecraft 1.16.5's own empty, water and lava fluids, which the glue makes (Portium's classes
 * can't name them) and hands here, held by Portium's {@code Fluids} for 1.16 code and registered in Forge 1.16's
 * fluid registry. If 1.13 mods' layer made its own already, 1.16 code shares those.
 */
public final class ModernFluids {

    private static final String[] NAMES = {"empty", "flowing_water", "water", "flowing_lava", "lava"};

    public static void setVanilla(Object empty, Object flowingWater, Object water, Object flowingLava, Object lava) {
        if (Fluids.EMPTY == null) Fluids.portium$set((Fluid) empty, (Fluid) flowingWater, (Fluid) water, (Fluid) flowingLava, (Fluid) lava);
        @SuppressWarnings("unchecked")
        IRegistry<Fluid> fluids = IRegistry.FLUID;
        Fluid[] all = vanilla();
        for (int i = 0; i < NAMES.length; i++) {
            ResourceLocation name = new ResourceLocation(NAMES[i]);
            if (!fluids.containsKey(name)) fluids.put(name, all[i]);
        }
    }

    /** Puts the vanilla fluids in Forge 1.16's fluid registry, as its registry event starts. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerVanilla(IForgeRegistry registry) {
        if (Fluids.EMPTY == null) return;
        Fluid[] all = vanilla();
        for (int i = 0; i < NAMES.length; i++) {
            ResourceLocation name = new ResourceLocation(NAMES[i]);
            if (registry.containsKey(name)) continue;
            if (all[i].getRegistryName() == null) all[i].setRegistryName(name);
            registry.register(all[i]);
        }
    }

    private static Fluid[] vanilla() {
        return new Fluid[] {Fluids.EMPTY, Fluids.FLOWING_WATER, Fluids.WATER, Fluids.FLOWING_LAVA, Fluids.LAVA};
    }

    private ModernFluids() {}
}
