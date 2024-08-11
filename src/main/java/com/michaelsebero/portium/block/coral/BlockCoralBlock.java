package com.michaelsebero.portium.block.coral;

import com.michaelsebero.portium.block.water.WaterUtil;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * A coral block. Live blocks die unless at least one side touches water; without silk touch they drop the dead block.
 */
public class BlockCoralBlock extends Block implements ICoral {

    private final CoralType type;
    @Nullable
    private final Block deadBlock;

    public BlockCoralBlock(CoralType type, @Nullable Block deadBlock) {
        super(Material.ROCK, deadBlock == null ? MapColor.GRAY : type.getMapColor());
        this.type = type;
        this.deadBlock = deadBlock;
        this.setHardness(1.5F).setResistance(10.0F);
        this.setHarvestLevel("pickaxe", 0);
        this.setSoundType(deadBlock == null ? SoundType.STONE : PortiumSoundTypes.CORAL);
        this.setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
    }

    @Override
    public CoralType getCoralType() {
        return this.type;
    }

    @Override
    public boolean isDead() {
        return this.deadBlock == null;
    }

    @Nullable
    @Override
    public Block getDeadBlock() {
        return this.deadBlock;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        this.scheduleDryingCheck(world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        this.scheduleDryingCheck(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        this.scheduleDryingCheck(world, pos);
    }

    private void scheduleDryingCheck(World world, BlockPos pos) {
        if (this.deadBlock != null && !WaterUtil.isTouchingWater(world, pos)) {
            world.scheduleUpdate(pos, this, ICoral.dryingDelay(world.rand));
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (this.deadBlock != null && !WaterUtil.isTouchingWater(world, pos)) {
            world.setBlockState(pos, this.deadBlock.getDefaultState(), 2);
        }
    }

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this.deadBlock != null ? this.deadBlock : this);
    }
}
