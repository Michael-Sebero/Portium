package com.michaelsebero.portium.block.workstation;

import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.tile.TileEntityComposter;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The composter (1.14): plant matter and food raise its level by chance; at seven it settles into bone meal a second
 * later, taken out by hand or by a hopper below. Hoppers above fill it.
 */
public class BlockComposter extends Block {

    public static final PropertyInteger LEVEL = PropertyInteger.create("level", 0, 8);
    /** The world event a fill sends, as 1.14 does: its sound and specks on clients (see {@code MixinRenderGlobalEvents}). */
    public static final int FILL_EVENT = 1500;

    private static final AxisAlignedBB FLOOR = box(0, 0, 0, 16, 2, 16);
    private static final AxisAlignedBB[] WALLS = {box(0, 0, 0, 2, 16, 16), box(14, 0, 0, 16, 16, 16), box(0, 0, 0, 16, 16, 2), box(0, 0, 14, 16, 16, 16)};

    public BlockComposter() {
        super(Material.WOOD);
        this.setHardness(0.6F);
        this.setSoundType(SoundType.WOOD);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreativeTabs.MISC);
        this.setDefaultState(this.blockState.getBaseState().withProperty(LEVEL, 0));
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    public static ItemStack boneMeal() {
        return new ItemStack(Items.DYE, 1, 15);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LEVEL);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(LEVEL, Math.min(meta, 8));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LEVEL);
    }

    // ---- Use ----

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        int level = state.getValue(LEVEL);
        ItemStack held = player.getHeldItem(hand);
        if (level < 8 && Compostables.chance(held) >= 0.0F) {
            if (level < 7 && !world.isRemote) {
                IBlockState after = attemptCompost(state, world, pos, held);
                world.playEvent(FILL_EVENT, pos, state != after ? 1 : 0);
                if (!player.capabilities.isCreativeMode) held.shrink(1);
            }
            return true;
        }
        if (level == 8) {
            empty(state, world, pos);
            return true;
        }
        return false;
    }

    /** Takes the bone meal out of a full composter. */
    public static IBlockState empty(IBlockState state, World world, BlockPos pos) {
        if (!world.isRemote) {
            double x = (double) (world.rand.nextFloat() * 0.7F) + 0.15000000596046448D;
            double y = (double) (world.rand.nextFloat() * 0.7F) + 0.06000000238418579D + 0.6D;
            double z = (double) (world.rand.nextFloat() * 0.7F) + 0.15000000596046448D;
            EntityItem item = new EntityItem(world, (double) pos.getX() + x, (double) pos.getY() + y, (double) pos.getZ() + z, boneMeal());
            item.setDefaultPickupDelay();
            world.spawnEntity(item);
        }
        IBlockState empty = state.withProperty(LEVEL, 0);
        world.setBlockState(pos, empty, 3);
        world.playSound(null, pos, PortiumSounds.BLOCK_COMPOSTER_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return empty;
    }

    /**
     * A farmer villager puts one seed in (1.16's FarmerWorkTask): false if the composter is full or isn't there. Seeds
     * count by their compost chance, as a player's do.
     */
    public static boolean villagerCompost(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockComposter) || state.getValue(LEVEL) >= 7) return false;
        IBlockState after = attemptCompost(state, world, pos, new ItemStack(net.minecraft.init.Items.WHEAT_SEEDS));
        world.playEvent(FILL_EVENT, pos, after != state ? 1 : 0);
        return true;
    }

    /** One item in: by its chance the level goes up (always on an empty composter if it counts at all). */
    public static IBlockState attemptCompost(IBlockState state, World world, BlockPos pos, ItemStack stack) {
        int level = state.getValue(LEVEL);
        float chance = Compostables.chance(stack);
        if ((level != 0 || chance <= 0.0F) && world.rand.nextDouble() >= (double) chance) return state;
        int raised = level + 1;
        IBlockState after = state.withProperty(LEVEL, raised);
        world.setBlockState(pos, after, 3);
        if (raised == 7) world.scheduleUpdate(pos, state.getBlock(), 20);
        return after;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (state.getValue(LEVEL) == 7) world.scheduleUpdate(pos, this, 20);
    }

    /** At seven, a second later: ready. */
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (state.getValue(LEVEL) == 7) {
            world.setBlockState(pos, state.withProperty(LEVEL, 8), 3);
            world.playSound(null, pos, PortiumSounds.BLOCK_COMPOSTER_READY, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    // ---- Drops: itself, and its bone meal when full ----

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(new ItemStack(this));
        if (state.getValue(LEVEL) == 8) drops.add(boneMeal());
    }

    // ---- Tile entity, for hoppers ----

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityComposter();
    }

    // ---- Shape: a box with a floor two pixels thick ----

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        addCollisionBoxToList(pos, entityBox, boxes, FLOOR);
        for (AxisAlignedBB wall : WALLS) addCollisionBoxToList(pos, entityBox, boxes, wall);
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
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.UP ? BlockFaceShape.BOWL : BlockFaceShape.SOLID;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    /** The top of the compost in the middle, for the fill specks. */
    public static double fillHeight(IBlockState state) {
        int level = state.getBlock() instanceof BlockComposter ? state.getValue(LEVEL) : 0;
        return (double) Math.max(2, 1 + Math.min(level, 7) * 2) / 16.0D;
    }
}
