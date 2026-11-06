package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.LegacyFluids116;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;

/**
 * A Forge 1.12 fluid handler (a vanilla bucket's, a 1.12 mod's tank) as Forge 1.16's, for 1.16 mods that ask for 1.16's
 * fluid capabilities (Create's drain emptying a water bucket). Fluids cross by id.
 */
public final class LegacyFluidHandler116 implements IFluidHandlerItem {

    private final Object handler;

    LegacyFluidHandler116(Object handler) {
        this.handler = handler;
    }

    static FluidStack stack(String id, int amount, Object tag) {
        if (id == null || amount <= 0) return FluidStack.EMPTY;
        Fluid fluid = (Fluid) (Object) ForgeRegistries.FLUIDS.getValue(new ResourceLocation(id));
        if (fluid == null || fluid == Fluids.field_204541_a) return FluidStack.EMPTY;
        return new FluidStack(fluid, amount, tag instanceof CompoundNBT ? (CompoundNBT) tag : null);
    }

    private static String id(FluidStack stack) {
        // Fluids are registry entries at runtime (Portium patches them so); not in the jar compiled against.
        ResourceLocation name = ((net.minecraftforge.registries.IForgeRegistryEntry<?>) (Object) stack.getFluid()).getRegistryName();
        return name == null ? null : name.toString();
    }

    @Override
    public int getTanks() {
        return LegacyFluids116.tanks(this.handler);
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return stack(LegacyFluids116.fluid(this.handler, tank), LegacyFluids116.amount(this.handler, tank), LegacyFluids116.tag(this.handler, tank));
    }

    @Override
    public int getTankCapacity(int tank) {
        return LegacyFluids116.capacity(this.handler, tank);
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;
        String id = id(resource);
        return id == null ? 0 : LegacyFluids116.fill(this.handler, id, resource.getAmount(), resource.getTag(), action.execute());
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        return result(LegacyFluids116.drain(this.handler, id(resource), resource.getAmount(), resource.getTag(), action.execute()));
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return result(LegacyFluids116.drain(this.handler, null, maxDrain, null, action.execute()));
    }

    private static FluidStack result(Object[] drained) {
        return drained == null ? FluidStack.EMPTY : stack((String) drained[0], (Integer) drained[1], drained[2]);
    }

    @Nonnull
    @Override
    public ItemStack getContainer() {
        return (ItemStack) LegacyFluids116.container(this.handler);
    }
}
