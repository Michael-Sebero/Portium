package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.fluid.IFluidState;
import com.michaelsebero.portium.modern.mc.init.Fluids;
import com.michaelsebero.portium.modern.mc.util.IItemProvider;
import com.michaelsebero.portium.modern.mc.item.BlockItemUseContext;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A block made by a 1.13 mod, through one of Portium's adapters ({@link ModernBlock}, {@link ModernBlockBed}): 1.13's
 * block methods, which the adapters answer from 1.12's and 1.12's calls reach. For calls 1.13 code makes on any
 * block, Portium's shims use these on 1.13 blocks and 1.12's methods on the rest.
 */
public interface IModernBlock {

    @Srg("func_196244_b")
    VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos);

    @Srg("func_196268_f")
    VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos);

    @Nullable
    @Srg("func_196258_a")
    IBlockState getStateForPlacement(BlockItemUseContext context);

    @Srg("func_196271_a")
    IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState facingState, World world, BlockPos pos, BlockPos facingPos);

    @Srg("func_196260_a")
    boolean isValidPosition(IBlockState state, IBlockAccess world, BlockPos pos);

    @Srg("func_196250_a")
    boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY,
                             float hitZ);

    @Srg("func_185473_a")
    ItemStack getItem(IBlockAccess world, BlockPos pos, IBlockState state);

    /** The tile entity of a 1.13 {@code ITileEntityProvider}, or null. */
    @Nullable
    @Srg("func_196283_a_")
    TileEntity createNewTileEntity(IBlockAccess world);

    /** 1.13's {@code isSideInvisible}: whether a face against this neighbour is hidden (glass against glass). */
    @Srg("func_200122_a")
    default boolean isSideInvisible(IBlockState state, IBlockState adjacent, net.minecraft.util.EnumFacing side) {
        return false;
    }

    /** The fluid in the block, as in a waterlogged block or a fluid's own; none by default. */
    @Srg("func_204507_t")
    default IFluidState getFluidState(IBlockState state) {
        return Fluids.EMPTY.getDefaultState();
    }

    // Drops: 1.13 still drops items by count and kind, as 1.12 does; loot tables for blocks came in 1.14.

    @Srg("func_196264_a")
    default int quantityDropped(IBlockState state, Random random) {
        return 1;
    }

    @Srg("func_196251_a")
    default int getItemsToDropCount(IBlockState state, int fortune, World world, BlockPos pos, Random random) {
        return quantityDropped(state, random);
    }

    @Srg("func_199769_a")
    default IItemProvider getItemDropped(IBlockState state, World world, BlockPos pos, int fortune) {
        return (IItemProvider) this;
    }

    /** Forge 1.13's {@code getDrops}: the dropped items, by the methods above. */
    default void getDrops(IBlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune) {
        int count = getItemsToDropCount(state, fortune, world, pos, world.rand);
        for (int i = 0; i < count; i++) {
            Item item = getItemDropped(state, world, pos, fortune).asItem();
            if (item != Items.AIR) drops.add(new ItemStack(item));
        }
    }
}
