package com.michaelsebero.portium.block.water;

import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A plant that only exists inside a water source: seagrass and kelp. The cell always counts as water.
 */
@SuppressWarnings("deprecation")
public abstract class BlockWaterPlant extends BlockBush implements IWaterloggable, IShearable {

    protected BlockWaterPlant() {
        super(Material.WATER, MapColor.WATER);
        this.setSoundType(PortiumSoundTypes.WET_GRASS);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean isWaterlogged(IBlockState state) {
        return true;
    }

    @Override
    public int getLightOpacity(IBlockState state) {
        return WaterUtil.waterLightOpacity();
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return WaterUtil.isPlainWaterSource(world.getBlockState(pos)) && this.canSustainAt(world, pos.down());
    }

    @Override
    public boolean canBlockStay(World world, BlockPos pos, IBlockState state) {
        return this.canSustainAt(world, pos.down());
    }

    /** Seagrass and kelp root on any block with a solid top face, except magma (1.13). */
    protected boolean canSustainAt(IBlockAccess world, BlockPos soilPos) {
        IBlockState soil = world.getBlockState(soilPos);
        return soil.getBlock() != net.minecraft.init.Blocks.MAGMA && soil.getBlockFaceShape(world, soilPos, EnumFacing.UP) == BlockFaceShape.SOLID;
    }

    @Override
    protected boolean canSustainBush(IBlockState state) {
        return true;
    }

    @Override
    protected void checkAndDropBlock(World world, BlockPos pos, IBlockState state) {
        if (!this.canBlockStay(world, pos, state)) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockState(pos, WaterUtil.waterSource(), 3);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        WaterUtil.restoreWaterIfCleared(world, pos);
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return type == EntityLiving.SpawnPlacementType.IN_WATER;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.TRANSLUCENT;
    }

    // Drops: nothing by default, the item only with shears (seagrass).

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean isShearable(@Nonnull ItemStack item, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Nonnull
    @Override
    public List<ItemStack> onSheared(@Nonnull ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.emptyList();
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, net.minecraft.entity.Entity entity) {
        return true;
    }
}
