package com.michaelsebero.portium.block.nether;

import com.michaelsebero.portium.api.IManyStateBlock;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The growing end of weeping vines (hanging, growing down) and twisting vines (standing, growing up), 1.16. It grows
 * one block now and then until its age reaches 25, and bone meal makes it grow several; the rest of the vine is
 * {@link BlockNetherVinePlant}. Its 26 ages exceed 1.12's metadata, so the full state is kept beside the chunk
 * ({@link IManyStateBlock}). Vines can be climbed, and drop only to shears, silk touch, or by chance (more with fortune).
 */
public class BlockNetherVine extends Block implements IGrowable, IShearable, IManyStateBlock {

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 25);
    private static final AxisAlignedBB WEEPING = new AxisAlignedBB(0.25D, 0.5625D, 0.25D, 0.75D, 1.0D, 0.75D);
    private static final AxisAlignedBB TWISTING = new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 0.9375D, 0.75D);
    private static final double GROWTH_CHANCE = 0.1D;

    final EnumFacing growth;
    private Block body;

    public BlockNetherVine(MapColor mapColor, EnumFacing growth) {
        super(Material.PLANTS, mapColor);
        this.growth = growth;
        this.setDefaultState(this.blockState.getBaseState().withProperty(AGE, 0));
        this.setSoundType(PortiumSoundTypes.NETHER_VINE);
        this.setTickRandomly(true);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
    }

    public void setBody(Block body) {
        this.body = body;
    }

    public Block getBody() {
        return this.body;
    }

    /** A fresh end of a vine, with a random age as 1.16 gives one placed or broken off. */
    public IBlockState withRandomAge(Random random) {
        return this.getDefaultState().withProperty(AGE, random.nextInt(25));
    }

    // ---- Shape and rendering ----

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.growth == EnumFacing.DOWN ? WEEPING : TWISTING;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    // ---- States ----

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, AGE);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return IManyStateBlock.metaOf(state);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return IManyStateBlock.stateOf(this, meta);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        // Placed above (or below) a vine of its own kind, it becomes part of that vine.
        Block next = world.getBlockState(pos.offset(this.growth)).getBlock();
        return next == this || next == this.body ? this.body.getDefaultState() : this.withRandomAge(world.rand);
    }

    // ---- Survival ----

    /** The block the vine hangs from (or stands on): another part of it, or a solid face. */
    static boolean canHangFrom(World world, BlockPos pos, EnumFacing growth, Block top, Block body) {
        BlockPos support = pos.offset(growth.getOpposite());
        IBlockState state = world.getBlockState(support);
        Block block = state.getBlock();
        return block == top || block == body || state.isSideSolid(world, support, growth);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && canHangFrom(world, pos, this.growth, this, this.body);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!canHangFrom(world, pos, this.growth, this, this.body)) world.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!canHangFrom(world, pos, this.growth, this, this.body)) world.destroyBlock(pos, true);
    }

    // ---- Growth ----

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        int age = state.getValue(AGE);
        if (age >= 25 || random.nextDouble() >= GROWTH_CHANCE) return;
        BlockPos next = pos.offset(this.growth);
        if (world.isAirBlock(next)) {
            world.setBlockState(next, state.withProperty(AGE, age + 1));
            world.setBlockState(pos, this.body.getDefaultState());
        }
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return world.isAirBlock(pos.offset(this.growth));
    }

    @Override
    public boolean canUseBonemeal(World world, Random random, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random random, BlockPos pos, IBlockState state) {
        BlockPos next = pos.offset(this.growth);
        int age = Math.min(state.getValue(AGE) + 1, 25);
        int count = growthAmount(random);
        BlockPos end = pos;
        for (int i = 0; i < count && world.isAirBlock(next); i++) {
            world.setBlockState(next, this.getDefaultState().withProperty(AGE, age));
            end = next;
            next = next.offset(this.growth);
            age = Math.min(age + 1, 25);
        }
        // Everything behind the new end becomes vine.
        for (BlockPos at = pos; !at.equals(end); at = at.offset(this.growth)) world.setBlockState(at, this.body.getDefaultState());
    }

    /** How many blocks bone meal adds: each one 82.6% as likely as the one before. */
    static int growthAmount(Random random) {
        double chance = 1.0D;
        int count = 0;
        while (random.nextDouble() < chance) {
            chance *= 0.826D;
            count++;
        }
        return count;
    }

    // ---- Drops ----

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        dropByChance(drops, this, fortune);
    }

    /** 1.16's vine loot: a third of the time, more with fortune (33%, 55%, 77%, always). */
    static void dropByChance(NonNullList<ItemStack> drops, Block top, int fortune) {
        float[] chances = {0.33F, 0.55F, 0.77F, 1.0F};
        if (RANDOM.nextFloat() < chances[Math.min(Math.max(fortune, 0), 3)]) drops.add(new ItemStack(top));
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune) {
        return Collections.singletonList(new ItemStack(this));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }
}
