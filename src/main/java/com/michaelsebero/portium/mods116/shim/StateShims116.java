package com.michaelsebero.portium.mods116.shim;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

import java.util.Collection;
import java.util.Optional;

/** 1.16's block state methods (on {@code StateHolder}) that 1.12's states have under other names, or lack. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class StateShims116 {

    /**
     * {@code with}: as 1.12's, but a property the state lacks leaves it as it is (Portium's stand-in for a block it
     * hasn't backported is air, which 1.16 code then sets its properties on).
     */
    public static Object with(IBlockState state, IProperty property, Comparable value) {
        return state.getPropertyKeys().contains(property) ? state.withProperty(property, value) : state;
    }

    /** {@code cycleValue}. */
    public static Object cycle(IBlockState state, IProperty property) {
        return state.cycleProperty(property);
    }

    /** {@code hasProperty}. */
    public static boolean hasProperty(IBlockState state, IProperty property) {
        return state.getPropertyKeys().contains(property);
    }

    /** {@code getProperties}. */
    public static Collection getProperties(IBlockState state) {
        return state.getPropertyKeys();
    }

    /** {@code func_235903_d_}: the value, if the state has the property. */
    public static Optional getOptional(IBlockState state, IProperty property) {
        return hasProperty(state, property) ? Optional.of(state.getValue(property)) : Optional.empty();
    }

    /** {@code getValues}. */
    public static ImmutableMap getValues(IBlockState state) {
        return state.getProperties();
    }

    /**
     * 1.16's {@code new BlockState(block, values, codec)}: a state of its own (code makes one as a marker, compared by
     * identity), made as 1.12 makes its states.
     */
    public static IBlockState newState(net.minecraft.block.Block block, ImmutableMap values, Object codec) {
        return new MadeState(block, values == null ? ImmutableMap.of() : values);
    }

    private static final class MadeState extends net.minecraft.block.state.BlockStateContainer.StateImplementation {
        MadeState(net.minecraft.block.Block block, ImmutableMap values) {
            super(block, values);
        }
    }

    /** {@code getSelf}: the state itself (1.16's state classes are generic in their own type). */
    public static IBlockState self(IBlockState state) {
        return state;
    }

    /**
     * 1.16's {@code container.getBaseState()}, for block and fluid states alike: typed as objects, so a fluid's state
     * passes where 1.16 code takes its state container's states (1.12's state containers hold block states).
     */
    public static Object baseState(Object container) {
        if (container instanceof com.michaelsebero.portium.modern.mc.fluid.Fluid.States) {
            return ((com.michaelsebero.portium.modern.mc.fluid.Fluid.States) container).base();
        }
        return ((net.minecraft.block.state.BlockStateContainer) container).getBaseState();
    }

    private StateShims116() {}
}
