package com.michaelsebero.portium.block.building;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

/**
 * A wooden door of a newer wood. 1.12's doors name their item by block (oak unless it's a vanilla one), so this drops
 * and picks its own: the item registered under its name.
 */
public class BlockPortiumDoor extends BlockDoor {

    private final MapColor mapColor;

    public BlockPortiumDoor(MapColor mapColor) {
        super(Material.WOOD);
        this.mapColor = mapColor;
        this.setHardness(3.0F);
        this.setSoundType(SoundType.WOOD);
        this.disableStats();
    }

    private Item item() {
        Item item = ForgeRegistries.ITEMS.getValue(getRegistryName());
        return item == null ? Items.AIR : item;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return state.getValue(HALF) == EnumDoorHalf.UPPER ? Items.AIR : item();
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(item());
    }

    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.mapColor;
    }
}
