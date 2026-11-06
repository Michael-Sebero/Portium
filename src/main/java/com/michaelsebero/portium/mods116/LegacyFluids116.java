package com.michaelsebero.portium.mods116;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

/**
 * Forge 1.12's fluid handlers (a vanilla bucket's, another 1.12 mod's tank) as the glue's 1.16 handler sees them, in
 * plain values: fluids by 1.16 id ({@code minecraft:water}), amounts, tags. Forge 1.16's fluid API is another one with
 * the same class names, so the glue, compiled against it, can't name these.
 */
public final class LegacyFluids116 {

    /** A provider's 1.12 fluid handler (an item's, if {@code item}), or null. */
    @Nullable
    @SuppressWarnings({"rawtypes"})
    public static Object handler(Object provider, boolean item, @Nullable Object side) {
        Capability capability = item ? CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY : CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
        if (capability == null) return null;
        Object handler = ModernCapabilities.get(provider, capability, side);
        return handler instanceof IFluidHandler ? handler : null;
    }

    private static IFluidTankProperties[] tanksOf(Object handler) {
        IFluidTankProperties[] tanks = ((IFluidHandler) handler).getTankProperties();
        return tanks == null ? new IFluidTankProperties[0] : tanks;
    }

    public static int tanks(Object handler) {
        return tanksOf(handler).length;
    }

    @Nullable
    private static FluidStack contents(Object handler, int tank) {
        IFluidTankProperties[] tanks = tanksOf(handler);
        return tank < 0 || tank >= tanks.length ? null : tanks[tank].getContents();
    }

    @Nullable
    public static String fluid(Object handler, int tank) {
        FluidStack contents = contents(handler, tank);
        return contents == null || contents.getFluid() == null ? null : name(contents.getFluid());
    }

    public static int amount(Object handler, int tank) {
        FluidStack contents = contents(handler, tank);
        return contents == null ? 0 : contents.amount;
    }

    @Nullable
    public static Object tag(Object handler, int tank) {
        FluidStack contents = contents(handler, tank);
        return contents == null ? null : contents.tag;
    }

    public static int capacity(Object handler, int tank) {
        IFluidTankProperties[] tanks = tanksOf(handler);
        return tank < 0 || tank >= tanks.length ? 0 : tanks[tank].getCapacity();
    }

    public static int fill(Object handler, String fluid, int amount, @Nullable Object tag, boolean doFill) {
        FluidStack stack = stack(fluid, amount, tag);
        return stack == null ? 0 : ((IFluidHandler) handler).fill(stack, doFill);
    }

    /** What draining gives, as {fluid id, amount, tag}, or null for nothing. A null fluid drains whatever there is. */
    @Nullable
    public static Object[] drain(Object handler, @Nullable String fluid, int amount, @Nullable Object tag, boolean doDrain) {
        FluidStack drained;
        if (fluid == null) {
            drained = ((IFluidHandler) handler).drain(amount, doDrain);
        } else {
            FluidStack wanted = stack(fluid, amount, tag);
            if (wanted == null) return null;
            drained = ((IFluidHandler) handler).drain(wanted, doDrain);
        }
        if (drained == null || drained.amount <= 0 || drained.getFluid() == null) return null;
        return new Object[] {name(drained.getFluid()), drained.amount, drained.tag};
    }

    /** An item handler's container (an emptied bucket after draining); for others, nothing. */
    public static Object container(Object handler) {
        return handler instanceof IFluidHandlerItem ? ((IFluidHandlerItem) handler).getContainer() : ItemStack.EMPTY;
    }

    /** 1.16's id for a 1.12 fluid: water and lava (and other unnamespaced ones, Forge's milk) are minecraft's. */
    static String name(Fluid fluid) {
        String name = fluid.getName();
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }

    @Nullable
    private static FluidStack stack(String id, int amount, @Nullable Object tag) {
        ResourceLocation name = new ResourceLocation(id);
        Fluid fluid = FluidRegistry.getFluid("minecraft".equals(name.getNamespace()) ? name.getPath() : id);
        if (fluid == null) fluid = FluidRegistry.getFluid(name.getPath());
        return fluid == null ? null : new FluidStack(fluid, amount, tag instanceof NBTTagCompound ? (NBTTagCompound) tag : null);
    }

    private LegacyFluids116() {}
}
