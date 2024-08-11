package com.michaelsebero.portium.block;

import com.michaelsebero.portium.entity.passive.EntityTurtle;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

/**
 * Turtle eggs (1.13): 1-4 per block, crack over time on sand and hatch into baby turtles that call that beach home.
 * They break when trampled. Breaking the block removes one egg at a time; eggs only drop with silk touch.
 */
@SuppressWarnings("deprecation")
public class BlockTurtleEgg extends Block implements IStackableBlock {

    public static final PropertyInteger EGGS = PropertyInteger.create("eggs", 1, 4);
    public static final PropertyInteger HATCH = PropertyInteger.create("hatch", 0, 2);

    private static final AxisAlignedBB ONE_AABB = new AxisAlignedBB(3 / 16.0, 0, 3 / 16.0, 12 / 16.0, 7 / 16.0, 12 / 16.0);
    private static final AxisAlignedBB MULTI_AABB = new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 7 / 16.0, 15 / 16.0);

    public BlockTurtleEgg() {
        super(Material.DRAGON_EGG, MapColor.SAND);
        this.setDefaultState(this.blockState.getBaseState().withProperty(EGGS, 1).withProperty(HATCH, 0));
        this.setHardness(0.5F);
        this.setSoundType(SoundType.METAL);
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.MISC);
    }

    @Override
    public PropertyInteger getCountProperty() {
        return EGGS;
    }

    // ---- Hatching ----

    private static boolean onSand(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() == Blocks.SAND;
    }

    /** Eggs crack around dawn, or rarely at any time. */
    private static boolean isHatchingTime(World world) {
        float angle = world.getCelestialAngle(1.0F);
        return (angle < 0.69F && angle > 0.65F) || world.rand.nextInt(500) == 0;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!isHatchingTime(world) || !onSand(world, pos)) return;
        int hatch = state.getValue(HATCH);
        if (hatch < 2) {
            world.playSound(null, pos, PortiumSounds.ENTITY_TURTLE_EGG_CRACK, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            world.setBlockState(pos, state.withProperty(HATCH, hatch + 1), 2);
            return;
        }
        world.playSound(null, pos, PortiumSounds.ENTITY_TURTLE_EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
        world.setBlockToAir(pos);
        for (int i = 0; i < state.getValue(EGGS); i++) {
            world.playEvent(2001, pos, Block.getStateId(state));
            EntityTurtle turtle = new EntityTurtle(world);
            turtle.setGrowingAge(-24000);
            turtle.setHome(pos);
            turtle.setLocationAndAngles(pos.getX() + 0.3D + i * 0.2D, pos.getY(), pos.getZ() + 0.3D, 0.0F, 0.0F);
            world.spawnEntity(turtle);
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (onSand(world, pos) && !world.isRemote) {
            world.playEvent(2005, pos, 0);
        }
    }

    // ---- Trampling ----

    private static boolean canTrample(World world, Entity entity) {
        if (entity instanceof EntityTurtle) return false;
        if (entity instanceof EntityLivingBase && !(entity instanceof EntityPlayer)) {
            return world.getGameRules().getBoolean("mobGriefing");
        }
        return true;
    }

    private void tryTrample(World world, BlockPos pos, Entity entity, int chance) {
        if (canTrample(world, entity) && !world.isRemote && world.rand.nextInt(chance) == 0) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == this) this.removeOneEgg(world, pos, state);
        }
    }

    private void removeOneEgg(World world, BlockPos pos, IBlockState state) {
        world.playSound(null, pos, PortiumSounds.ENTITY_TURTLE_EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.rand.nextFloat() * 0.2F);
        int eggs = state.getValue(EGGS);
        if (eggs <= 1) {
            world.destroyBlock(pos, false);
        } else {
            world.setBlockState(pos, state.withProperty(EGGS, eggs - 1), 2);
            world.playEvent(2001, pos, Block.getStateId(state));
        }
    }

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        this.tryTrample(world, pos, entity, 100);
        super.onEntityWalk(world, pos, entity);
    }

    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        if (!(entity instanceof EntityZombie)) this.tryTrample(world, pos, entity, 3);
        super.onFallenUpon(world, pos, entity, fallDistance);
    }

    // ---- Breaking: one egg at a time, silk touch only ----

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.harvestBlock(world, player, pos, state, te, stack);
        this.removeOneEgg(world, pos, state);
    }

    @Nonnull
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Nonnull
    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    // ---- Shape ----

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && world.isSideSolid(pos.down(), EnumFacing.UP);
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(EGGS) > 1 ? MULTI_AABB : ONE_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Nonnull
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Nonnull
    @Override
    public net.minecraft.util.BlockRenderLayer getRenderLayer() {
        return net.minecraft.util.BlockRenderLayer.CUTOUT;
    }

    // eggs-1 in bits 0-1, hatch in bits 2-3.
    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(EGGS) - 1) | (state.getValue(HATCH) << 2);
    }

    @Nonnull
    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(EGGS, (meta & 3) + 1).withProperty(HATCH, Math.min(2, (meta >> 2) & 3));
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, EGGS, HATCH);
    }
}
