package com.michaelsebero.portium.block;

import com.michaelsebero.portium.block.water.BlockWaterPlant;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * Two blocks tall seagrass, grown by bone-mealing seagrass. Shearing either half gives two seagrass (1.13).
 */
public class BlockTallSeagrass extends BlockWaterPlant {

    public static final PropertyEnum<BlockDoublePlant.EnumBlockHalf> HALF = BlockDoublePlant.HALF;

    public BlockTallSeagrass() {
        this.setDefaultState(this.blockState.getBaseState().withProperty(HALF, BlockDoublePlant.EnumBlockHalf.LOWER).withProperty(LEVEL, 0));
        this.setCreativeTab(null);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        if (state.getBlock() != this) return super.canBlockStay(world, pos, state);
        if (state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER) {
            IBlockState below = world.getBlockState(pos.down());
            return below.getBlock() == this && below.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.LOWER;
        }
        IBlockState above = world.getBlockState(pos.up());
        return above.getBlock() == this && above.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER && super.canBlockStay(world, pos, state);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && WaterUtil.isPlainWaterSource(world.getBlockState(pos.up()));
    }

    @Override
    public boolean isShearable(@Nonnull ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nonnull ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(PortiumBlocks.SEAGRASS, 2));
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
        return new ItemStack(PortiumBlocks.SEAGRASS);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER ? 1 : 0;
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(HALF, (meta & 1) == 1 ? BlockDoublePlant.EnumBlockHalf.UPPER : BlockDoublePlant.EnumBlockHalf.LOWER);
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF, LEVEL);
    }
}
