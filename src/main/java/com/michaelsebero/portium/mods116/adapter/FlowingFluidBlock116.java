package com.michaelsebero.portium.mods116.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.adapter.ModernBlock;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.state.StateContainer$Builder;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.function.Supplier;

/**
 * What a 1.16 mod's {@code FlowingFluidBlock} becomes: a block of a 1.16 fluid (Minecraft 1.16.5's own fluid
 * classes, which 1.12 lacks), with 1.16's level property, no shape, and nothing drawn for the block itself (1.16
 * draws fluids apart from blocks). Forge 1.16's {@code getFluid} returns the fluid.
 */
public class FlowingFluidBlock116 extends ModernBlock {

    @Srg("field_176367_b")
    public static final PropertyInteger LEVEL = BlockLiquid.LEVEL;

    /** The 1.16 fluid's supplier. */
    public final Supplier<?> portium$fluid;

    public FlowingFluidBlock116(Supplier<?> fluid, Block$Properties properties) {
        super(properties);
        this.portium$fluid = fluid;
        setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, 0));
    }

    @Override
    @Srg("func_206840_a")
    protected void fillStateContainer(StateContainer$Builder<Block, IBlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    @Srg("func_196244_b")
    public VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return hitIfLiquid && state.getValue(LEVEL) == 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }
}
