package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.init.PortiumSoundTypes;
import com.michaelsebero.portium.world.v116.feature.HugeFungus;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/** Crimson and warped fungi (1.16). Bone meal grows one into a huge fungus when it stands on its own nylium. */
public class BlockNetherFungus extends BlockBush implements IGrowable {

    private static final AxisAlignedBB SHAPE = new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 0.5625D, 0.75D);

    private final boolean warped;

    public BlockNetherFungus(MapColor mapColor, boolean warped) {
        super(Material.PLANTS, mapColor);
        this.warped = warped;
        this.setSoundType(PortiumSoundTypes.FUNGUS);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return SHAPE;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return NetherPlant.isFungusGround(state);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return NetherPlant.isFungusGround(world.getBlockState(pos.down()));
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && NetherPlant.isFungusGround(world.getBlockState(pos.down()));
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.getBlockState(pos.down()).getBlock() == BlockNylium.nyliumFor(this.warped);
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return random.nextFloat() < 0.4F;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        HugeFungus.place(world, random, pos, this.warped, true);
    }
}
