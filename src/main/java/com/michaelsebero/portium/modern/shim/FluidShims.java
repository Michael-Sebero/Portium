package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.adapter.IModernBlock;
import com.michaelsebero.portium.modern.mc.fluid.Fluid;
import com.michaelsebero.portium.modern.mc.fluid.IFluidState;
import com.michaelsebero.portium.modern.mc.init.Fluids;
import com.michaelsebero.portium.modern.mc.util.registry.IRegistry;
import com.michaelsebero.portium.modern.mc.world.ITickList;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.13's fluid states read from 1.12's world: 1.12's water and lava blocks as 1.13's water and lava, Portium's
 * waterlogged cells as still water, and 1.13 blocks (fluid blocks and waterloggable ones) as they say. 1.13's fluid
 * and block ticks are both 1.12's scheduled block updates.
 */
public final class FluidShims {

    /** 1.13's {@code world.getFluidState(pos)}. */
    public static IFluidState getFluidState(IBlockAccess world, BlockPos pos) {
        IFluidState fluid = getFluidState(world.getBlockState(pos));
        if (fluid.isEmpty() && Waterlogging.isWaterlogged(world, pos)) return Fluids.WATER.getDefaultState();
        return fluid;
    }

    /** 1.13's {@code state.getFluidState()}. */
    public static IFluidState getFluidState(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof IModernBlock) return ((IModernBlock) block).getFluidState(state);
        return legacyFluidState(state);
    }

    /** 1.13's {@code block.getFluidState(state)}. */
    public static IFluidState getFluidState(Block block, IBlockState state) {
        if (block instanceof IModernBlock) return ((IModernBlock) block).getFluidState(state);
        return legacyFluidState(state);
    }

    private static IFluidState legacyFluidState(IBlockState state) {
        if (!(state.getBlock() instanceof BlockLiquid)) return Fluids.EMPTY.getDefaultState();
        boolean lava = state.getMaterial() == Material.LAVA;
        int level = state.getValue(BlockLiquid.LEVEL);
        if (level == 0) return (lava ? Fluids.LAVA : Fluids.WATER).getDefaultState();
        // 1.12 counts down from the source (1 to 7, 8 and up falling); 1.13 counts the height up (1 to 8).
        return flowing(lava ? Fluids.FLOWING_LAVA : Fluids.FLOWING_WATER, level >= 8 ? 8 : 8 - level, level >= 8);
    }

    private static IFluidState flowing(Fluid fluid, int level, boolean falling) {
        IFluidState state = fluid.getDefaultState();
        for (IProperty<?> property : state.getValues().keySet()) {
            if ("level".equals(property.getName())) state = with(state, property, level);
            if ("falling".equals(property.getName())) state = with(state, property, falling);
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static IFluidState with(IFluidState state, IProperty property, Comparable value) {
        return property.getAllowedValues().contains(value) ? (IFluidState) state.with(property, value) : state;
    }

    /** The block a fluid a 1.13 mod registered lives in, for 1.12's fluid tags; null if there's no such fluid. */
    @javax.annotation.Nullable
    public static Block fluidBlock(net.minecraft.util.ResourceLocation id) {
        Object fluid = IRegistry.FLUID.getNullable(id);
        if (!(fluid instanceof Fluid)) return null;
        Block block = ((Fluid) fluid).getDefaultState().getBlockState().getBlock();
        return block == net.minecraft.init.Blocks.AIR ? null : block;
    }

    private static final Map<Class<?>, Method> TAG_CONTAINS = new ConcurrentHashMap<>();

    /**
     * Whether a newer tag contains a fluid: Minecraft 1.13.2's {@code Tag}, or a 1.16 {@code ITag} (Portium's named
     * tags for 1.16 mods), which Portium can't name; their {@code contains} by its SRG name in either version.
     */
    public static boolean isInTag(Object tag, Fluid fluid) {
        if (tag == null) return false;
        Method contains = TAG_CONTAINS.computeIfAbsent(tag.getClass(), type -> {
            for (Method method : type.getMethods()) {
                if ((method.getName().equals("func_199685_a_") || method.getName().equals("func_230235_a_")) && method.getParameterCount() == 1) {
                    // A tag class may be private (Portium's own 1.16 named tags are), its method public.
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new IllegalArgumentException(type + " isn't a tag");
        });
        try {
            return (Boolean) contains.invoke(tag, fluid);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 1.13's {@code world.getPendingFluidTicks()}: updates of the block holding the fluid. */
    public static ITickList<Fluid> getPendingFluidTicks(World world) {
        return new ScheduledUpdates<>(world, true);
    }

    /** 1.13's {@code world.getPendingBlockTicks()}. */
    public static ITickList<Block> getPendingBlockTicks(World world) {
        return new ScheduledUpdates<>(world, false);
    }

    private static final class ScheduledUpdates<T> implements ITickList<T> {

        private final World world;
        private final boolean fluids;

        ScheduledUpdates(World world, boolean fluids) {
            this.world = world;
            this.fluids = fluids;
        }

        private Block block(BlockPos pos, T target) {
            return this.fluids || !(target instanceof Block) ? this.world.getBlockState(pos).getBlock() : (Block) target;
        }

        @Override
        public boolean isTickScheduled(BlockPos pos, T target) {
            return this.world.isUpdateScheduled(pos, block(pos, target));
        }

        @Override
        public void scheduleTick(BlockPos pos, T target, int delay, Object priority) {
            this.world.scheduleUpdate(pos, block(pos, target), delay);
        }

        @Override
        public boolean isTickPending(BlockPos pos, T target) {
            return isTickScheduled(pos, target);
        }
    }

    private FluidShims() {}
}
