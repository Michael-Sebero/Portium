package com.michaelsebero.portium.block;

import com.michaelsebero.portium.block.water.BlockWaterPlant;
import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * The growing tip of a kelp column (1.13 {@code kelp}). The segments below it are {@link BlockKelpPlant}.
 * <p>
 * 1.13 tracks age 0-25; block metadata only has room for 0-15, so a column here can grow up to 15 more blocks
 * after it is placed. Natural columns are still limited by water depth first.
 */
public class BlockKelp extends BlockWaterPlant {

    public static final int MAX_AGE = 15;
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, MAX_AGE);
    private static final AxisAlignedBB AABB = new AxisAlignedBB(0, 0, 0, 1, 9 / 16.0, 1);

    public BlockKelp() {
        this.setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0).withProperty(LEVEL, 0));
        this.setTickRandomly(true);
    }

    /** A fresh tip with room left to grow, as placed by players and world generation. */
    public IBlockState getRandomAgeState(Random random) {
        return this.getDefaultState().withProperty(AGE, random.nextInt(MAX_AGE));
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return AABB;
    }

    @Override
    protected boolean canSustainAt(IBlockAccess world, BlockPos soilPos) {
        Block below = world.getBlockState(soilPos).getBlock();
        return below == this || below == PortiumBlocks.KELP_PLANT || super.canSustainAt(world, soilPos);
    }

    @Nonnull
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        return this.getRandomAgeState(world.rand);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        this.checkAndDropBlock(world, pos, state);
        if (world.getBlockState(pos) != state) return;
        int age = state.getValue(AGE);
        BlockPos above = pos.up();
        if (age < MAX_AGE && rand.nextDouble() < 0.14 && WaterUtil.isPlainWaterSource(world.getBlockState(above))) {
            world.setBlockState(above, state.withProperty(AGE, age + 1));
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (fromPos.equals(pos.up())) {
            Block above = world.getBlockState(fromPos).getBlock();
            if (above == this || above == PortiumBlocks.KELP_PLANT) {
                world.setBlockState(pos, PortiumBlocks.KELP_PLANT.getDefaultState(), 2);
                return;
            }
        }
        super.neighborChanged(state, world, pos, block, fromPos);
    }

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return PortiumItems.KELP;
    }

    @Nonnull
    @Override
    public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
        return new ItemStack(PortiumItems.KELP);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(AGE);
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(AGE, meta & MAX_AGE);
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE, LEVEL);
    }
}
