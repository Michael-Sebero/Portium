package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import com.michaelsebero.portium.world.v116.feature.NetherVegetation;
import com.michaelsebero.portium.world.v116.feature.NetherVines;
import com.michaelsebero.portium.world.v116.GenFlags;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Crimson and warped nylium (1.16): netherrack overgrown with fungus. It turns back into netherrack under a block that
 * covers it whole, drops netherrack unless silk-touched, and bone meal grows the forest's plants around it.
 */
public class BlockNylium extends Block implements IGrowable {

    private final boolean warped;

    public BlockNylium(MapColor mapColor, boolean warped) {
        super(Material.ROCK, mapColor);
        this.warped = warped;
        this.setHardness(0.4F);
        this.setSoundType(PortiumSoundTypes.NYLIUM);
        this.setHarvestLevel("pickaxe", 0);
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    /** Whether light still reaches the top: no opaque, whole block on it. */
    private static boolean isUncovered(World world, BlockPos pos) {
        BlockPos up = pos.up();
        IBlockState above = world.getBlockState(up);
        return !(above.isOpaqueCube() || above.getLightOpacity(world, up) >= 15);
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isRemote && !isUncovered(world, pos)) world.setBlockState(pos, Blocks.NETHERRACK.getDefaultState());
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        // Random ticks only; scheduled ticks have nothing to do.
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Item.getItemFromBlock(Blocks.NETHERRACK);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.isAirBlock(pos.up());
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        BlockPos above = pos.up();
        if (this.warped) {
            NetherVegetation.place(world, random, above, NetherVegetation.WARPED_FOREST, 3, 1, GenFlags.GROWTH);
            NetherVegetation.place(world, random, above, NetherVegetation.SPROUTS, 3, 1, GenFlags.GROWTH);
            if (random.nextInt(8) == 0) NetherVines.spreadTwisting(world, random, above, 3, 1, 2, GenFlags.GROWTH);
        } else {
            NetherVegetation.place(world, random, above, NetherVegetation.CRIMSON_FOREST, 3, 1, GenFlags.GROWTH);
        }
    }

    /** Nylium, which fungi, roots and sprouts grow on. */
    public static boolean isNylium(Block block) {
        return block instanceof BlockNylium;
    }

    public boolean isWarped() {
        return this.warped;
    }

    /** The nylium the given fungus wants beneath it to grow huge. */
    static Block nyliumFor(boolean warped) {
        return warped ? PortiumBlocks.WARPED_NYLIUM : PortiumBlocks.CRIMSON_NYLIUM;
    }
}
