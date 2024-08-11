package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Crimson and warped roots, and nether sprouts (1.16): small replaceable plants on nylium and soul soil, set off-centre
 * in their cell. Roots drop themselves; sprouts only come away with shears.
 */
public class BlockNetherRoots extends BlockBush implements IShearable {

    private static final AxisAlignedBB ROOTS = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 0.8125D, 0.875D);
    private static final AxisAlignedBB SPROUTS = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 0.1875D, 0.875D);

    private final boolean sprouts;

    public BlockNetherRoots(MapColor mapColor, boolean sprouts) {
        super(NetherMaterials.NETHER_PLANTS, mapColor);
        this.sprouts = sprouts;
        this.setSoundType(sprouts ? PortiumSoundTypes.NETHER_SPROUT : PortiumSoundTypes.ROOT);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return (this.sprouts ? SPROUTS : ROOTS).offset(state.getOffset(world, pos));
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return NetherPlant.isRootGround(state);
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return NetherPlant.isRootGround(world.getBlockState(pos.down()));
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && NetherPlant.isRootGround(world.getBlockState(pos.down()));
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return this.sprouts ? Items.AIR : Item.getItemFromBlock(this);
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return this.sprouts;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }
}
