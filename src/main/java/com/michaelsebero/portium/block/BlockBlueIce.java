package com.michaelsebero.portium.block;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

/** Blue ice (1.13): slipperier than packed ice, never melts, only drops with silk touch. */
public class BlockBlueIce extends Block {

    public BlockBlueIce() {
        super(Material.PACKED_ICE);
        this.setDefaultSlipperiness(0.989F);
        this.setHardness(2.8F);
        this.setSoundType(SoundType.GLASS);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }
}
