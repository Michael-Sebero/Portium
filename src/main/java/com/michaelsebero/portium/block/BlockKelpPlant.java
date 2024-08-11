package com.michaelsebero.portium.block;

import com.michaelsebero.portium.block.water.BlockWaterPlant;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumItems;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * A kelp segment below the tip (1.13 {@code kelp_plant}). Becomes a new tip when the blocks above are cut,
 * which is what makes kelp farms regrow.
 */
public class BlockKelpPlant extends BlockWaterPlant {

    public BlockKelpPlant() {
        this.setCreativeTab(null);
    }

    @Override
    protected boolean canSustainAt(IBlockAccess world, BlockPos soilPos) {
        Block below = world.getBlockState(soilPos).getBlock();
        return below == this || below == PortiumBlocks.KELP || super.canSustainAt(world, soilPos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (fromPos.equals(pos.up())) {
            Block above = world.getBlockState(fromPos).getBlock();
            if (above != this && above != PortiumBlocks.KELP) {
                world.setBlockState(pos, ((BlockKelp) PortiumBlocks.KELP).getRandomAgeState(world.rand), 2);
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
        return 0;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL);
    }
}
