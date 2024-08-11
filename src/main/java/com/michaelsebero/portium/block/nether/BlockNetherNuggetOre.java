package com.michaelsebero.portium.block.nether;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Blocks that drop gold nuggets (1.16): nether gold ore always drops 2-6 (more with fortune) and a little experience;
 * gilded blackstone drops 2-5 now and then (10% of the time, more with fortune) and itself otherwise.
 */
public class BlockNetherNuggetOre extends Block {

    private static final float[] GILDED_CHANCES = {0.1F, 0.14285715F, 0.25F, 1.0F};

    private final boolean gilded;

    private BlockNetherNuggetOre(MapColor mapColor, SoundType sound, boolean gilded) {
        super(Material.ROCK, mapColor);
        this.gilded = gilded;
        this.setSoundType(sound);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    public static BlockNetherNuggetOre goldOre(SoundType sound) {
        BlockNetherNuggetOre ore = new BlockNetherNuggetOre(MapColor.NETHERRACK, sound, false);
        ore.setHardness(3.0F);
        return ore;
    }

    public static BlockNetherNuggetOre gildedBlackstone(SoundType sound) {
        BlockNetherNuggetOre block = new BlockNetherNuggetOre(MapColor.BLACK, sound, true);
        block.setHardness(1.5F).setResistance(10.0F);
        return block;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : RANDOM;
        if (this.gilded) {
            if (random.nextFloat() < GILDED_CHANCES[Math.min(Math.max(fortune, 0), 3)]) {
                drops.add(new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4)));
            } else {
                drops.add(new ItemStack(this));
            }
            return;
        }
        int count = 2 + random.nextInt(5);
        // 1.16's ore_drops fortune bonus: multiplies by 1 to fortune + 1.
        if (fortune > 0) count *= Math.max(1, random.nextInt(fortune + 2));
        drops.add(new ItemStack(Items.GOLD_NUGGET, count));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return this.gilded ? Item.getItemFromBlock(this) : Items.GOLD_NUGGET;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    public int getExpDrop(IBlockState state, IBlockAccess world, BlockPos pos, int fortune) {
        Random random = world instanceof World ? ((World) world).rand : RANDOM;
        return this.gilded ? 0 : random.nextInt(2);
    }
}
