package com.michaelsebero.portium.modern.host;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryEntry;

/** Forge 1.13's registry entry calls that depend on Forge 1.13's loading context. The translator points them here. */
public final class RegistryGlue {

    /**
     * {@code entry.setRegistryName(name)}: an unqualified name is the active 1.13 mod's, as in Forge 1.13. Forge
     * 1.12's would give it the active 1.12 mod's, which is Portium's while it passes 1.12's events on.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static IForgeRegistryEntry setRegistryName(ForgeRegistryEntry entry, String name) {
        ModContainer active = ModLoadingContext.get().getActiveContainer();
        ResourceLocation id = name.indexOf(':') >= 0 || active == null ? new ResourceLocation(name) : new ResourceLocation(active.getNamespace(), name);
        return (IForgeRegistryEntry) entry.setRegistryName(id);
    }

    private RegistryGlue() {}
}
