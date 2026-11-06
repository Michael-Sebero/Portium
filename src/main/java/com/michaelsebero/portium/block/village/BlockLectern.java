package com.michaelsebero.portium.block.village;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.init.PortiumStats;
import com.michaelsebero.portium.inventory.PortiumGuis;
import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import com.michaelsebero.portium.tile.TileEntityLectern;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The lectern (1.14): holds a book and quill or a written book for anyone to read ({@link TileEntityLectern}). Turning
 * a page gives a 2-tick redstone pulse (strong into the block below); a comparator reads how far through the book the
 * page is. 16 states: facing, powered, has_book.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class BlockLectern extends BlockContainer implements com.michaelsebero.portium.block.IOutlineShape {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyBool HAS_BOOK = PropertyBool.create("has_book");
    /** 1.14's world event for a lectern's page turning. */
    public static final int PAGE_TURN_EVENT = 1043;

    private static final VoxelShape COMMON = VoxelShapes.or(box(0, 0, 0, 16, 2, 16), box(4, 2, 4, 12, 14, 12));
    /** The outline, with the sloped top it leans toward its front. */
    private static final Map<EnumFacing, VoxelShape> SHAPES = new EnumMap<>(EnumFacing.class);

    static {
        SHAPES.put(EnumFacing.WEST, VoxelShapes.or(box(1, 10, 0, 5.333333, 14, 16), box(5.333333, 12, 0, 9.666667, 16, 16),
                box(9.666667, 14, 0, 14, 18, 16), COMMON));
        SHAPES.put(EnumFacing.NORTH, VoxelShapes.or(box(0, 10, 1, 16, 14, 5.333333), box(0, 12, 5.333333, 16, 16, 9.666667),
                box(0, 14, 9.666667, 16, 18, 14), COMMON));
        SHAPES.put(EnumFacing.EAST, VoxelShapes.or(box(15, 10, 0, 10.666667, 14, 16), box(10.666667, 12, 0, 6.333333, 16, 16),
                box(6.333333, 14, 0, 2, 18, 16), COMMON));
        SHAPES.put(EnumFacing.SOUTH, VoxelShapes.or(box(0, 10, 15, 16, 14, 10.666667), box(0, 12, 10.666667, 16, 16, 6.333333),
                box(0, 14, 6.333333, 16, 18, 2), COMMON));
    }

    public BlockLectern() {
        super(Material.WOOD, MapColor.WOOD);
        this.setHardness(2.5F);
        this.setResistance(2.5F * 5.0F / 3.0F);
        this.setSoundType(SoundType.WOOD);
        this.setHarvestLevel("axe", 0);
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(POWERED, false)
                .withProperty(HAS_BOOK, false));
    }

    private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return VoxelShapes.create(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    // ---- States ----

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, POWERED, HAS_BOOK);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | (state.getValue(POWERED) ? 4 : 0) | (state.getValue(HAS_BOOK) ? 8 : 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(POWERED, (meta & 4) != 0)
                .withProperty(HAS_BOOK, (meta & 8) != 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    /** Faces the placer; holds a book already if an operator places one carrying its data (1.14). */
    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        boolean book = false;
        if (!world.isRemote && placer instanceof EntityPlayer && ((EntityPlayer) placer).canUseCommandBlock()) {
            NBTTagCompound tag = placer.getHeldItem(hand).getTagCompound();
            book = tag != null && tag.getCompoundTag("BlockEntityTag").hasKey("Book");
        }
        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(HAS_BOOK, book);
    }

    // ---- Shape ----

    @Override
    public VoxelShape getOutlineShape(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable net.minecraft.entity.player.EntityPlayer viewer) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.bounds(SHAPES.get(state.getValue(FACING)));
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.collisionBounds(COMMON);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        ModernBlocks.addCollisionBoxes(COMMON, pos, entityBox, boxes);
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        return SHAPES.get(state.getValue(FACING)).rayTrace(start, end, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    // ---- The book ----

    /** Puts a book on it if it has none (server); true if it had none, on either side. */
    public static boolean tryPlaceBook(World world, BlockPos pos, IBlockState state, ItemStack stack) {
        if (state.getValue(HAS_BOOK)) return false;
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityLectern) {
                ((TileEntityLectern) tile).setBook(stack.splitStack(1));
                setHasBook(world, pos, state, true);
                world.playSound(null, pos, PortiumSounds.ITEM_BOOK_PUT, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
        }
        return true;
    }

    public static boolean isLecternBook(ItemStack stack) {
        return stack.getItem() == Items.WRITABLE_BOOK || stack.getItem() == Items.WRITTEN_BOOK;
    }

    public static void setHasBook(World world, BlockPos pos, IBlockState state, boolean hasBook) {
        world.setBlockState(pos, state.withProperty(POWERED, false).withProperty(HAS_BOOK, hasBook), 3);
        notifyBelow(world, pos, state);
    }

    /** A page turned: a 2-tick pulse and the page-turn sound. */
    public static void pulse(World world, BlockPos pos, IBlockState state) {
        setPowered(world, pos, state, true);
        world.scheduleUpdate(pos, state.getBlock(), 2);
        world.playEvent(PAGE_TURN_EVENT, pos, 0);
    }

    private static void setPowered(World world, BlockPos pos, IBlockState state, boolean powered) {
        world.setBlockState(pos, state.withProperty(POWERED, powered), 3);
        notifyBelow(world, pos, state);
    }

    private static void notifyBelow(World world, BlockPos pos, IBlockState state) {
        world.notifyNeighborsOfStateChange(pos.down(), state.getBlock(), false);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        setPowered(world, pos, state, false);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (state.getValue(HAS_BOOK)) this.dropBook(state, world, pos);
        if (state.getValue(POWERED)) world.notifyNeighborsOfStateChange(pos.down(), this, false);
        super.breakBlock(world, pos, state);
    }

    /** The book pops out over the lectern's front. */
    private void dropBook(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityLectern)) return;
        TileEntityLectern lectern = (TileEntityLectern) tile;
        EnumFacing facing = state.getValue(FACING);
        ItemStack book = lectern.getBook().copy();
        float x = 0.25F * (float) facing.getXOffset();
        float z = 0.25F * (float) facing.getZOffset();
        EntityItem item = new EntityItem(world, pos.getX() + 0.5D + x, pos.getY() + 1, pos.getZ() + 0.5D + z, book);
        item.setDefaultPickupDelay();
        world.spawnEntity(item);
        lectern.clear();
    }

    // ---- Redstone ----

    @Override
    @SuppressWarnings("deprecation")
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return side == EnumFacing.UP && state.getValue(POWERED) ? 15 : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        if (!state.getValue(HAS_BOOK)) return 0;
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityLectern ? ((TileEntityLectern) tile).getComparatorSignalLevel() : 0;
    }

    // ---- Use ----

    /** With a book on it, opens the book; without, a held item other than a book is held back from use (1.14). */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        if (state.getValue(HAS_BOOK)) {
            if (!world.isRemote) {
                player.openGui(Portium.instance, PortiumGuis.LECTERN, world, pos.getX(), pos.getY(), pos.getZ());
                player.addStat(PortiumStats.INTERACT_WITH_LECTERN);
            }
            return true;
        }
        ItemStack held = player.getHeldItem(hand);
        return !held.isEmpty() && !isLecternBook(held);
    }

    /**
     * A book put on an empty lectern, as 1.14's books do it in their own use code (so also while sneaking, and after
     * the lectern itself had nothing to do).
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!isLecternBook(stack)) return;
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BlockLectern) || state.getValue(HAS_BOOK)) return;
        if (tryPlaceBook(world, event.getPos(), state, stack)) {
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityLectern();
    }
}
