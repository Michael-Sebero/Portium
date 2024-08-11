package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A log-like pillar for the 1.13 stripped logs, wood and stripped wood of the six vanilla tree types.
 * Burns like a log and counts as a log for leaf decay.
 */
public class BlockPortiumLog extends BlockRotatedPillar {

    public BlockPortiumLog(MapColor mapColor) {
        super(Material.WOOD, mapColor);
        this.setHardness(2.0F);
        this.setSoundType(SoundType.WOOD);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        // Upright by default, as in 1.16: 1.12's pillar leaves that to subclasses, else the first axis (X) is the
        // default, and blocks placed from it (by world generation) lie on their side.
        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }

    @Override
    public boolean isWood(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean canSustainLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 5;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        // Like vanilla logs: let nearby leaves know they may need to decay.
        if (world.isAreaLoaded(pos.add(-5, -5, -5), pos.add(5, 5, 5))) {
            for (BlockPos leafPos : BlockPos.getAllInBox(pos.add(-4, -4, -4), pos.add(4, 4, 4))) {
                IBlockState leaf = world.getBlockState(leafPos);
                if (leaf.getBlock().isLeaves(leaf, world, leafPos)) leaf.getBlock().beginLeavesDecay(leaf, world, leafPos);
            }
        }
        super.breakBlock(world, pos, state);
    }
}
