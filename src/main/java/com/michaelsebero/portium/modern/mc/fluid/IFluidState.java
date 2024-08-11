package com.michaelsebero.portium.modern.mc.fluid;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.particles.IParticleData;
import com.michaelsebero.portium.modern.shim.FluidShims;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.13's {@code net.minecraft.fluid.IFluidState}: one state of a fluid, as a block state is one of a block. What 1.12
 * has of fluids, its water and lava blocks and Portium's waterlogging, reads as these through {@code FluidShims}.
 */
public interface IFluidState {

    @Srg("func_206886_c")
    Fluid getFluid();

    @Srg("func_177229_b")
    <T extends Comparable<T>> T get(IProperty<T> property);

    @Srg("func_206870_a")
    <T extends Comparable<T>, V extends T> Object with(IProperty<T> property, V value);

    @Srg("func_196959_b")
    <T extends Comparable<T>> boolean has(IProperty<T> property);

    @Srg("func_177231_a")
    <T extends Comparable<T>> Object cycle(IProperty<T> property);

    @Srg("func_206871_b")
    ImmutableMap<IProperty<?>, Comparable<?>> getValues();

    @Srg("func_206889_d")
    default boolean isSource() {
        return getFluid().isSource(this);
    }

    @Srg("func_206888_e")
    default boolean isEmpty() {
        return getFluid().isEmpty();
    }

    @Srg("func_206885_f")
    default float getHeight() {
        return getFluid().getHeight(this);
    }

    @Srg("func_206882_g")
    default int getLevel() {
        return getFluid().getLevel(this);
    }

    /** Whether any neighbour along the sides is another fluid, or a block that doesn't fill its cell. */
    @Srg("func_205586_a")
    default boolean shouldRenderSides(IBlockAccess world, BlockPos pos) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos side = pos.add(x, 0, z);
                if (!FluidShims.getFluidState(world, side).getFluid().isEquivalentTo(getFluid()) && !world.getBlockState(side).isOpaqueCube()) return true;
            }
        }
        return false;
    }

    @Srg("func_206880_a")
    default void tick(World world, BlockPos pos) {
        getFluid().tick(world, pos, this);
    }

    @Srg("func_206881_a")
    @SideOnly(Side.CLIENT)
    default void animateTick(World world, BlockPos pos, Random random) {
        getFluid().animateTick(world, pos, this, random);
    }

    @Srg("func_206890_h")
    default boolean ticksRandomly() {
        return getFluid().ticksRandomly();
    }

    @Srg("func_206891_b")
    default void randomTick(World world, BlockPos pos, Random random) {
        getFluid().randomTick(world, pos, this, random);
    }

    @Srg("func_206887_a")
    default Vec3d getFlow(IBlockAccess world, BlockPos pos) {
        return getFluid().getFlow(world, pos, this);
    }

    @Srg("func_206883_i")
    default IBlockState getBlockState() {
        return getFluid().getBlockState(this);
    }

    @Nullable
    @Srg("func_204521_c")
    @SideOnly(Side.CLIENT)
    default IParticleData getDripParticleData() {
        return getFluid().getDripParticleData();
    }

    @Srg("func_180664_k")
    @SideOnly(Side.CLIENT)
    default BlockRenderLayer getRenderLayer() {
        return getFluid().getRenderLayer();
    }

    /** Whether the fluid is in a fluid tag (1.13's {@code Tag<Fluid>}). */
    @Srg("func_206884_a")
    default boolean isTagged(Object tag) {
        return getFluid().isIn(tag);
    }

    @Srg("func_210200_l")
    default float getExplosionResistance() {
        return getFluid().getExplosionResistance();
    }

    @Srg("func_211725_a")
    default boolean canOtherFlowInto(Fluid fluid, EnumFacing direction) {
        return getFluid().canOtherFlowInto(this, fluid, direction);
    }
}
