package com.michaelsebero.portium.modern.mc.fluid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.particles.IParticleData;
import com.michaelsebero.portium.modern.mc.state.StateContainer$Builder;
import com.michaelsebero.portium.modern.shim.FluidShims;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 1.13's {@code net.minecraft.fluid.Fluid}: a kind of fluid, with states like a block's. 1.13's water, lava and
 * flowing fluids are Minecraft 1.13.2's own classes extending this; their states live in blocks (see
 * {@code FluidShims}), so a fluid's ticks are its block's scheduled updates.
 */
public abstract class Fluid extends com.michaelsebero.portium.modern.RegistryEntryStandIn<Fluid> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Class<Fluid> getRegistryType() {
        return Fluid.class;
    }


    /** Every fluid state, numbered, as 1.13 keeps them. */
    @Srg("field_207201_d")
    public static final ObjectIntIdentityMap<IFluidState> STATE_REGISTRY = new ObjectIntIdentityMap<>();

    private final ImmutableList<IFluidState> states;
    private IFluidState defaultState;

    protected Fluid() {
        StateContainer$Builder<Fluid, IFluidState> builder = new StateContainer$Builder<>(this);
        fillStateContainer(builder);
        this.states = makeStates(builder.properties());
        for (IFluidState state : this.states) STATE_REGISTRY.put(state, STATE_REGISTRY.size());
        setDefaultState(this.states.get(0));
    }

    /** One state per combination of values, each knowing its neighbours along every property. */
    private ImmutableList<IFluidState> makeStates(List<IProperty<?>> properties) {
        List<Map<IProperty<?>, Comparable<?>>> combinations = new ArrayList<>();
        combinations.add(Collections.emptyMap());
        for (IProperty<?> property : properties) {
            List<Map<IProperty<?>, Comparable<?>>> next = new ArrayList<>();
            for (Map<IProperty<?>, Comparable<?>> combination : combinations) {
                for (Comparable<?> value : property.getAllowedValues()) {
                    Map<IProperty<?>, Comparable<?>> extended = new java.util.LinkedHashMap<>(combination);
                    extended.put(property, value);
                    next.add(extended);
                }
            }
            combinations = next;
        }
        Map<Map<IProperty<?>, Comparable<?>>, FluidState> byValues = new HashMap<>();
        ImmutableList.Builder<IFluidState> all = ImmutableList.builder();
        for (Map<IProperty<?>, Comparable<?>> combination : combinations) {
            FluidState state = new FluidState(this, ImmutableMap.copyOf(combination));
            byValues.put(combination, state);
            all.add(state);
        }
        for (FluidState state : byValues.values()) {
            for (IProperty<?> property : properties) {
                Map<Comparable<?>, FluidState> along = new HashMap<>();
                for (Comparable<?> value : property.getAllowedValues()) {
                    Map<IProperty<?>, Comparable<?>> changed = new java.util.LinkedHashMap<>(state.getValues());
                    changed.put(property, value);
                    along.put(value, byValues.get(changed));
                }
                state.neighbours.put(property, along);
            }
        }
        return all.build();
    }

    @Srg("func_207184_a")
    protected void fillStateContainer(StateContainer$Builder<Fluid, IFluidState> builder) {}

    /** Every state of this fluid (1.13 gets them from its state container). */
    public List<IFluidState> getValidStates() {
        return this.states;
    }

    /**
     * 1.16's state container, which 1.16 reads the fluid's states from: 1.16's state containers are 1.12's block state
     * containers to 1.16 code, so this is one listing the fluid's states.
     */
    @Srg("func_207182_e")
    public net.minecraft.block.state.BlockStateContainer getStateContainer() {
        return new States(this.states);
    }

    /** A fluid's states as 1.16 code sees its state container (see {@code StateShims116.baseState}). */
    public static final class States extends net.minecraft.block.state.BlockStateContainer {

        private final ImmutableList<IFluidState> states;

        States(ImmutableList<IFluidState> states) {
            super(net.minecraft.init.Blocks.AIR);
            this.states = states;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public ImmutableList<IBlockState> getValidStates() {
            return (ImmutableList) this.states;
        }

        /** The base state: the first, with each property at its first value. */
        public IFluidState base() {
            return this.states.get(0);
        }
    }

    @Srg("func_207183_f")
    protected final void setDefaultState(IFluidState state) {
        this.defaultState = state;
    }

    @Srg("func_207188_f")
    public final IFluidState getDefaultState() {
        return this.defaultState;
    }

    @Srg("func_180664_k")
    @SideOnly(Side.CLIENT)
    protected abstract BlockRenderLayer getRenderLayer();

    @Srg("func_204524_b")
    public abstract Item getFilledBucket();

    @Srg("func_204522_a")
    @SideOnly(Side.CLIENT)
    protected void animateTick(World world, BlockPos pos, IFluidState state, Random random) {}

    @Srg("func_207191_a")
    protected void tick(World world, BlockPos pos, IFluidState state) {}

    @Srg("func_207186_b")
    protected void randomTick(World world, BlockPos pos, IFluidState state, Random random) {}

    @Nullable
    @Srg("func_204521_c")
    @SideOnly(Side.CLIENT)
    protected IParticleData getDripParticleData() {
        return null;
    }

    @Srg("func_211757_a")
    protected abstract boolean canOtherFlowInto(IFluidState state, Fluid fluid, EnumFacing direction);

    @Srg("func_205564_a")
    protected abstract Vec3d getFlow(IBlockAccess world, BlockPos pos, IFluidState state);

    @Srg("func_205569_a")
    public abstract int getTickRate(IBlockAccess world);

    @Srg("func_207196_h")
    protected boolean ticksRandomly() {
        return false;
    }

    @Srg("func_204538_c")
    protected boolean isEmpty() {
        return false;
    }

    @Srg("func_210195_d")
    protected abstract float getExplosionResistance();

    @Srg("func_207181_a")
    public abstract float getHeight(IFluidState state);

    @Srg("func_204527_a")
    protected abstract IBlockState getBlockState(IFluidState state);

    @Srg("func_207193_c")
    public abstract boolean isSource(IFluidState state);

    @Srg("func_207192_d")
    public abstract int getLevel(IFluidState state);

    @Srg("func_207187_a")
    public boolean isEquivalentTo(Fluid fluid) {
        return fluid == this;
    }

    /** Whether this fluid is in a fluid tag (1.13's {@code Tag<Fluid>}, a class of Minecraft 1.13.2's own). */
    @Srg("func_207185_a")
    public boolean isIn(Object tag) {
        return FluidShims.isInTag(tag, this);
    }
}
