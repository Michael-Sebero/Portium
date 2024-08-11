package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.adapter.BucketItem116;
import com.michaelsebero.portium.mods116.adapter.FlowingFluidBlock116;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;

/** Forge 1.16's fluid additions to Minecraft's classes. The translator points the calls here. */
public final class FluidGlue {

    /** {@code bucket.getFluid()}: the fluid of a 1.16 mod's bucket, or none for 1.12's own. */
    public static Fluid getFluid(BucketItem bucket) {
        Object item = bucket;
        return item instanceof BucketItem116 ? (Fluid) ((BucketItem116) item).portium$fluid.get() : Fluids.field_204541_a;
    }

    /** {@code block.getFluid()}: a 1.16 mod's fluid block's fluid. */
    public static FlowingFluid getFluid(FlowingFluidBlock block) {
        return (FlowingFluid) ((FlowingFluidBlock116) (Object) block).portium$fluid.get();
    }

    private FluidGlue() {}
}
