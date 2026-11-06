package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.ModernCapabilities;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;

/**
 * Forge 1.16's {@code LazyOptional} capability calls on Forge 1.12's capabilities, which 1.16 mods use (capabilities
 * are 1.12's for them, {@code LazyOptional} Forge 1.16's). The translator points the calls here.
 */
public final class CapabilityGlue {

    /** {@code provider.getCapability(capability, side)}: 1.12's answer, as a {@code LazyOptional}. */
    public static <T> LazyOptional<T> getCapability(ICapabilityProvider provider, Capability<T> capability, @Nullable Direction side) {
        Object value = ModernCapabilities.get(provider, capability, side);
        if (value == null && capability != null) value = legacyFluids(provider, capability, side);
        return optional(value);
    }

    /**
     * Forge 1.16's fluid capabilities on what has only 1.12's (vanilla buckets, 1.12 mods' tanks): 1.12's handler as a
     * 1.16 one. 1.16's fluid API is its own here (see ModernCapabilities), so nothing else answers these for them.
     */
    @Nullable
    private static Object legacyFluids(Object provider, Capability<?> capability, @Nullable Direction side) {
        boolean item = capability == net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY;
        if (!item && capability != net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return null;
        Object handler = com.michaelsebero.portium.mods116.LegacyFluids116.handler(provider, item, side);
        return handler == null ? null : new LegacyFluidHandler116(handler);
    }

    public static <T> LazyOptional<T> getCapability(ICapabilityProvider provider, Capability<T> capability) {
        return getCapability(provider, capability, null);
    }

    /** For the translator's 1.12 capability methods: a mod's answer, null counting as none. */
    public static boolean isPresent(@Nullable LazyOptional<?> optional) {
        return optional != null && optional.isPresent();
    }

    @Nullable
    public static Object orNull(@Nullable LazyOptional<?> optional) {
        return optional == null ? null : optional.orElse(null);
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

    /** Forge 1.16's {@code SidedInvWrapper.create(inventory, sides...)}: one wrapper per side. */
    @SuppressWarnings("unchecked")
    public static LazyOptional<net.minecraftforge.items.IItemHandlerModifiable>[] sidedWrappers(net.minecraft.inventory.ISidedInventory inventory, Direction... sides) {
        LazyOptional<net.minecraftforge.items.IItemHandlerModifiable>[] wrappers = new LazyOptional[sides.length];
        for (int i = 0; i < sides.length; i++) {
            Direction side = sides[i];
            net.minecraftforge.items.IItemHandlerModifiable wrapper = new net.minecraftforge.items.wrapper.SidedInvWrapper(inventory, side);
            wrappers[i] = LazyOptional.of(() -> wrapper);
        }
        return wrappers;
    }
}
