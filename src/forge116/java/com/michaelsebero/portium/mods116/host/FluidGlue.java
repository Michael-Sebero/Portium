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

    private static final java.util.Map<Fluid, net.minecraftforge.fluids.FluidAttributes> ATTRIBUTES = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /**
     * Forge 1.16's {@code fluid.getAttributes()}: made once per fluid by its {@code createAttributes()} (a mod's fluid,
     * such as Create's honey), else Forge's own for water, lava and empty.
     */
    public static net.minecraftforge.fluids.FluidAttributes getAttributes(Fluid fluid) {
        net.minecraftforge.fluids.FluidAttributes attributes = ATTRIBUTES.get(fluid);
        if (attributes != null) return attributes;
        attributes = createAttributes(fluid);
        ATTRIBUTES.put(fluid, attributes);
        return attributes;
    }

    private static net.minecraftforge.fluids.FluidAttributes createAttributes(Fluid fluid) {
        for (Class<?> type = fluid.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            try {
                java.lang.reflect.Method create = type.getDeclaredMethod("createAttributes");
                if (!net.minecraftforge.fluids.FluidAttributes.class.isAssignableFrom(create.getReturnType())) continue;
                create.setAccessible(true);
                return (net.minecraftforge.fluids.FluidAttributes) create.invoke(fluid);
            } catch (NoSuchMethodException e) {
                // Look further up.
            } catch (ReflectiveOperationException e) {
                break;
            }
        }
        return vanillaAttributes(fluid);
    }

    /** Forge 1.16's {@code ForgeHooks.createVanillaFluidAttributes}: water's, lava's and the empty fluid's. */
    private static net.minecraftforge.fluids.FluidAttributes vanillaAttributes(Fluid fluid) {
        if (fluid == Fluids.field_204546_a || fluid == Fluids.field_207212_b) {
            return net.minecraftforge.fluids.FluidAttributes.builder(new net.minecraft.util.ResourceLocation("block/water_still"),
                    new net.minecraft.util.ResourceLocation("block/water_flow")).translationKey("block.minecraft.water").color(0xFF3F76E4).build(fluid);
        }
        if (fluid == Fluids.field_204547_b || fluid == Fluids.field_207213_d) {
            return net.minecraftforge.fluids.FluidAttributes.builder(new net.minecraft.util.ResourceLocation("block/lava_still"),
                    new net.minecraft.util.ResourceLocation("block/lava_flow")).translationKey("block.minecraft.lava").luminosity(15).density(3000)
                    .viscosity(6000).temperature(1300).build(fluid);
        }
        return net.minecraftforge.fluids.FluidAttributes.builder(null, null).translationKey("block.minecraft.air").color(0).density(0).temperature(0)
                .luminosity(0).viscosity(0).build(fluid);
    }

    /** Forge 1.16's {@code buffer.writeFluidStack(stack)}. */
    public static void writeFluidStack(net.minecraft.network.PacketBuffer buffer, net.minecraftforge.fluids.FluidStack stack) {
        stack.writeToPacket(buffer);
    }

    /** Forge 1.16's {@code buffer.readFluidStack()}. */
    public static net.minecraftforge.fluids.FluidStack readFluidStack(net.minecraft.network.PacketBuffer buffer) {
        return net.minecraftforge.fluids.FluidStack.readFromPacket(buffer);
    }

    private FluidGlue() {}
}
