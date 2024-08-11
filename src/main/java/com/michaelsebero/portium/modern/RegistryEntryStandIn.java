package com.michaelsebero.portium.modern;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.IRegistryDelegate;

import javax.annotation.Nullable;

/**
 * What Portium's stand-ins for classes newer Forges register extend (fluids, particle types, stat types): a Forge
 * registry entry, with the methods Forge's {@code ForgeRegistryEntry} gives them and the same erased descriptors.
 * Both Forges have {@code IForgeRegistryEntry}, so the 1.13 glue compiles against this as well.
 */
public abstract class RegistryEntryStandIn<T extends IForgeRegistryEntry<T>> implements IForgeRegistryEntry<T> {

    public final IRegistryDelegate<T> delegate = new Delegate();
    @Nullable
    private ResourceLocation registryName;

    @SuppressWarnings("unchecked")
    public final T setRegistryName(ResourceLocation name) {
        if (this.registryName != null) {
            throw new IllegalStateException("Attempted to set registry name with existing registry name! New: " + name + " Old: " + this.registryName);
        }
        this.registryName = name;
        return (T) this;
    }

    public final T setRegistryName(String name) {
        return setRegistryName(new ResourceLocation(name));
    }

    public final T setRegistryName(String modId, String name) {
        return setRegistryName(new ResourceLocation(modId, name));
    }

    @Nullable
    public final ResourceLocation getRegistryName() {
        return this.registryName;
    }

    private final class Delegate implements IRegistryDelegate<T> {

        @Override
        @SuppressWarnings("unchecked")
        public T get() {
            return (T) RegistryEntryStandIn.this;
        }

        @Override
        public ResourceLocation name() {
            return RegistryEntryStandIn.this.registryName;
        }

        @Override
        public Class<T> type() {
            return getRegistryType();
        }
    }
}
