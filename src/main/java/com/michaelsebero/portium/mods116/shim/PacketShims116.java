package com.michaelsebero.portium.mods116.shim;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryManager;

/**
 * Forge 1.16's registry entries in packets ({@code PacketBuffer.writeRegistryId} and the like). Forge writes numeric
 * ids; Portium writes names (both sides are the same game, so only the pairing matters).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class PacketShims116 {

    private PacketShims116() {}

    public static void writeRegistryId(PacketBuffer buffer, IForgeRegistryEntry entry) {
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(entry.getRegistryType());
        buffer.writeResourceLocation(RegistryManager.ACTIVE.getName(registry));
        buffer.writeResourceLocation(entry.getRegistryName());
    }

    public static IForgeRegistryEntry readRegistryId(PacketBuffer buffer) {
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry(buffer.readResourceLocation());
        return registry.getValue(buffer.readResourceLocation());
    }

    public static IForgeRegistryEntry readRegistryIdSafe(PacketBuffer buffer, Class type) {
        IForgeRegistryEntry entry = readRegistryId(buffer);
        if (!type.isInstance(entry)) throw new IllegalArgumentException("Registry entry " + entry + " isn't a " + type.getName());
        return entry;
    }

    public static void writeRegistryIdUnsafe(PacketBuffer buffer, IForgeRegistry registry, IForgeRegistryEntry entry) {
        buffer.writeResourceLocation(entry.getRegistryName());
    }

    public static void writeRegistryIdUnsafe(PacketBuffer buffer, IForgeRegistry registry, ResourceLocation name) {
        buffer.writeResourceLocation(name);
    }

    public static IForgeRegistryEntry readRegistryIdUnsafe(PacketBuffer buffer, IForgeRegistry registry) {
        return registry.getValue(buffer.readResourceLocation());
    }
}
