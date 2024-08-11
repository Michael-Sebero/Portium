package com.michaelsebero.portium.modern;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nullable;

/**
 * Forge 1.12's registries, for 1.13 mods. Both Forges name their registries alike ({@code minecraft:blocks} and so
 * on), and {@code IForgeRegistry} is shared between them, so 1.13 mods register straight into 1.12's registries.
 */
public final class ModernRegistries {

    /** For the glue: 1.12's registry by name, or null if 1.12 has none such (1.13's tile entity types, say). */
    @Nullable
    public static IForgeRegistry<?> get(String name) {
        return RegistryManager.ACTIVE.getRegistry(new ResourceLocation(name));
    }

    private ModernRegistries() {}
}
