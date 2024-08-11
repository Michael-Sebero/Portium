package com.michaelsebero.portium.modern.host;

import com.michaelsebero.portium.modern.ModernCapabilities;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * Forge 1.13's {@code LazyOptional} capability calls on Forge 1.12's capabilities, which 1.13 mods use (capabilities
 * are 1.12's for them, {@code LazyOptional} Forge 1.13's). The translator points the calls here.
 */
public final class CapabilityGlue {

    /** {@code provider.getCapability(capability, side)}: 1.12's answer, as a {@code LazyOptional}. */
    public static <T> LazyOptional<T> getCapability(ICapabilityProvider provider, Capability<T> capability, @Nullable EnumFacing side) {
        return optional(ModernCapabilities.get(provider, capability, side));
    }

    public static <T> LazyOptional<T> getCapability(ICapabilityProvider provider, Capability<T> capability) {
        return getCapability(provider, capability, null);
    }

    /** {@code capability.orEmpty(asked, optional)}. */
    public static <T> LazyOptional<T> orEmpty(Capability<T> capability, Capability<?> asked, LazyOptional<?> optional) {
        return capability == asked ? optional.cast() : LazyOptional.empty();
    }

    /** A capability, or null, as a {@code LazyOptional}. */
    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> optional(@Nullable Object value) {
        return value == null ? LazyOptional.empty() : LazyOptional.of(() -> (T) value);
    }

    private CapabilityGlue() {}
}
