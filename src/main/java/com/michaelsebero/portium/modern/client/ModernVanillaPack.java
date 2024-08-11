package com.michaelsebero.portium.modern.client;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.FileResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;

import java.io.File;
import java.util.Set;

/**
 * Minecraft 1.13.2's own block and item textures and models (from the client jar Portium downloads for 1.13 mods),
 * below 1.12's: what 1.13 mods' models name that 1.12 doesn't have ({@code block/oak_planks},
 * {@code block/template_fence_gate}) is found here, and nothing 1.12 or Portium has is replaced.
 */
public final class ModernVanillaPack extends FileResourcePack {

    public ModernVanillaPack(File minecraft) {
        super(minecraft);
    }

    @Override
    public boolean hasResourceName(String name) {
        return (name.startsWith("assets/minecraft/textures/") || name.startsWith("assets/minecraft/models/")) && super.hasResourceName(name);
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of("minecraft");
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public String getPackName() {
        return "Minecraft 1.13.2 textures and models";
    }
}
