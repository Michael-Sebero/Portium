package com.michaelsebero.portium.block.village;

import com.michaelsebero.portium.api.IManyStateBlock;
import com.michaelsebero.portium.block.BlockSupport;
import com.michaelsebero.portium.block.IProjectileHitBlock;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.init.PortiumStats;
import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import com.michaelsebero.portium.tile.TileEntityBell;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
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

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The bell (1.14). It rings when used from the side (below its lip), when a projectile hits it there, or when it gets
 * redstone power; mobs within 32 blocks hear it, and raiders within 48 are revealed if it keeps ringing near them
 * ({@link TileEntityBell}). It stands on a floor, hangs from a ceiling, or hangs from one wall or between two, and
 * settles on one wall when the other goes. Facing, attachment and power make 32 states ({@link IManyStateBlock}).
 */
public class BlockBell extends BlockContainer implements IManyStateBlock, IProjectileHitBlock {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<BellAttachment> ATTACHMENT = PropertyEnum.create("attachment", BellAttachment.class);
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    /** Where 1.14 lets a bell be rung from: below this height on its side. */
    private static final double RING_HEIGHT = 0.8123999834060669D;

    private static final VoxelShape FLOOR_NORTH_SOUTH = box(0, 0, 4, 16, 16, 12);
    private static final VoxelShape FLOOR_EAST_WEST = box(4, 0, 0, 12, 16, 16);
    private static final VoxelShape BELL = VoxelShapes.or(box(4, 4, 4, 12, 6, 12), box(5, 6, 5, 11, 13, 11));
    private static final VoxelShape DOUBLE_WALL_NORTH_SOUTH = VoxelShapes.or(BELL, box(7, 13, 0, 9, 15, 16));
    private static final VoxelShape DOUBLE_WALL_EAST_WEST = VoxelShapes.or(BELL, box(0, 13, 7, 16, 15, 9));
    private static final VoxelShape CEILING = VoxelShapes.or(BELL, box(7, 13, 7, 9, 16, 9));
    /** A bell on one wall, by its facing: the bar reaches the wall it faces away from. */
    private static final Map<EnumFacing, VoxelShape> SINGLE_WALL = new EnumMap<>(EnumFacing.class);

    static {
        SINGLE_WALL.put(EnumFacing.WEST, VoxelShapes.or(BELL, box(0, 13, 7, 13, 15, 9)));
        SINGLE_WALL.put(EnumFacing.EAST, VoxelShapes.or(BELL, box(3, 13, 7, 16, 15, 9)));
        SINGLE_WALL.put(EnumFacing.NORTH, VoxelShapes.or(BELL, box(7, 13, 0, 9, 15, 13)));
        SINGLE_WALL.put(EnumFacing.SOUTH, VoxelShapes.or(BELL, box(7, 13, 3, 9, 15, 16)));
    }

    public BlockBell() {
        super(Material.IRON, MapColor.GOLD);
        this.setHardness(5.0F);
        this.setResistance(5.0F * 5.0F / 3.0F);
        this.setSoundType(SoundType.ANVIL);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
                .withProperty(ATTACHMENT, BellAttachment.FLOOR).withProperty(POWERED, false));
    }

    private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return VoxelShapes.create(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    // ---- States ----

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ATTACHMENT, POWERED);
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
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    // ---- Shape ----

    public static VoxelShape shape(IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        switch (state.getValue(ATTACHMENT)) {
            case FLOOR:
                return facing.getAxis() == EnumFacing.Axis.Z ? FLOOR_NORTH_SOUTH : FLOOR_EAST_WEST;
            case CEILING:
                return CEILING;
            case DOUBLE_WALL:
                return facing.getAxis() == EnumFacing.Axis.Z ? DOUBLE_WALL_NORTH_SOUTH : DOUBLE_WALL_EAST_WEST;
            default:
                return SINGLE_WALL.get(facing);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.bounds(shape(state));
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.collisionBounds(shape(state));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        ModernBlocks.addCollisionBoxes(shape(state), pos, entityBox, boxes);
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        return shape(state).rayTrace(start, end, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        // A standing bell's frame covers the middle of its top and bottom; nothing else is a face to build on.
        return state.getValue(ATTACHMENT) == BellAttachment.FLOOR && face.getAxis() == EnumFacing.Axis.Y ? BlockFaceShape.CENTER
                : BlockFaceShape.UNDEFINED;
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
        // The frame is a block model; the bell itself swings, drawn by its renderer.
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    // ---- Placement and support ----

    /** 1.14's {@code getStateForPlacement}: on the clicked wall (or between two), else on the floor or ceiling. */
    @Nullable
    private IBlockState placement(World world, BlockPos pos, EnumFacing clicked, EnumFacing placerFacing) {
        IBlockState state;
        if (clicked.getAxis() == EnumFacing.Axis.Y) {
            state = this.getDefaultState().withProperty(ATTACHMENT, clicked == EnumFacing.DOWN ? BellAttachment.CEILING : BellAttachment.FLOOR)
                    .withProperty(FACING, placerFacing);
            return canStay(world, pos, state) ? state : null;
        }
        EnumFacing.Axis axis = clicked.getAxis();
        EnumFacing negative = EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.NEGATIVE, axis);
        boolean between = solidFace(world, pos.offset(negative), negative.getOpposite()) && solidFace(world, pos.offset(negative.getOpposite()), negative);
        state = this.getDefaultState().withProperty(FACING, clicked.getOpposite())
                .withProperty(ATTACHMENT, between ? BellAttachment.DOUBLE_WALL : BellAttachment.SINGLE_WALL);
        if (canStay(world, pos, state)) return state;
        boolean floor = solidFace(world, pos.down(), EnumFacing.UP);
        state = state.withProperty(ATTACHMENT, floor ? BellAttachment.FLOOR : BellAttachment.CEILING);
        return canStay(world, pos, state) ? state : null;
    }

    /** 1.16's {@code isSolidSide}: the neighbour's whole face. */
    private static boolean solidFace(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return world.getBlockState(pos).getBlockFaceShape(world, pos, face) == BlockFaceShape.SOLID;
    }

    /** Toward the block holding the bell up. */
    private static EnumFacing supportDirection(IBlockState state) {
        switch (state.getValue(ATTACHMENT)) {
            case FLOOR:
                return EnumFacing.DOWN;
            case CEILING:
                return EnumFacing.UP;
            default:
                return state.getValue(FACING);
        }
    }

    /** 1.14's {@code isValidPosition}: what the bell hangs from holds it (a ceiling only needs a middle to hang from). */
    private static boolean canStay(IBlockAccess world, BlockPos pos, IBlockState state) {
        EnumFacing support = supportDirection(state);
        if (support == EnumFacing.UP) return BlockSupport.hasCenterSupport(world, pos.up(), EnumFacing.DOWN);
        return solidFace(world, pos.offset(support), support.getOpposite());
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos) && this.placement(world, pos, side, EnumFacing.NORTH) != null;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        IBlockState state = this.placement(world, pos, facing, placer.getHorizontalFacing());
        return state != null ? state : this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block block, BlockPos fromPos) {
        boolean powered = world.isBlockPowered(pos);
        if (powered != state.getValue(POWERED)) {
            if (powered) this.ring(world, pos, null);
            state = state.withProperty(POWERED, powered);
            world.setBlockState(pos, state, 3);
        }
        BlockPos offset = fromPos.subtract(pos);
        EnumFacing side = EnumFacing.getFacingFromVector(offset.getX(), offset.getY(), offset.getZ());
        if (!pos.offset(side).equals(fromPos)) return;
        IBlockState updated = updateShape(state, side, world, pos, fromPos);
        if (updated == null) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        } else if (updated != state) {
            world.setBlockState(pos, updated, 3);
        }
    }

    /**
     * 1.14's {@code updatePostPlacement} for a change on {@code side}: the bell falls when what holds it goes (unless
     * it's hung between walls), settles on one wall when either of two goes, and hangs between two when a wall comes
     * opposite the one it's on. Null when it falls.
     */
    @Nullable
    private static IBlockState updateShape(IBlockState state, EnumFacing side, World world, BlockPos pos, BlockPos neighborPos) {
        BellAttachment attachment = state.getValue(ATTACHMENT);
        EnumFacing support = supportDirection(state);
        if (support == side && !canStay(world, pos, state) && attachment != BellAttachment.DOUBLE_WALL) return null;
        EnumFacing facing = state.getValue(FACING);
        if (side.getAxis() == facing.getAxis()) {
            // As 1.14 does it, the wall a double-hung bell keeps is checked on its far face.
            if (attachment == BellAttachment.DOUBLE_WALL && !solidFace(world, neighborPos, side)) {
                return state.withProperty(ATTACHMENT, BellAttachment.SINGLE_WALL).withProperty(FACING, side.getOpposite());
            }
            if (attachment == BellAttachment.SINGLE_WALL && support.getOpposite() == side && solidFace(world, neighborPos, facing)) {
                return state.withProperty(ATTACHMENT, BellAttachment.DOUBLE_WALL);
            }
        }
        return state;
    }

    // ---- Ringing ----

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing,
                                    float hitX, float hitY, float hitZ) {
        return this.attemptRing(world, state, pos, facing, hitY, player, true);
    }

    @Override
    public void onProjectileHit(World world, IBlockState state, RayTraceResult hit, Entity projectile, @Nullable Entity shooter) {
        BlockPos pos = hit.getBlockPos();
        this.attemptRing(world, state, pos, hit.sideHit, hit.hitVec.y - pos.getY(), shooter instanceof EntityPlayer ? (EntityPlayer) shooter : null, true);
    }

    /** Rings it if hit where 1.14 lets it ring (when {@code checkSide}); true if it was hit there. */
    public boolean attemptRing(World world, IBlockState state, BlockPos pos, EnumFacing side, double hitY, @Nullable EntityPlayer player,
                               boolean checkSide) {
        if (checkSide && !canRingFrom(state, side, hitY)) return false;
        if (this.ring(world, pos, side) && player != null) player.addStat(PortiumStats.BELL_RING);
        return true;
    }

    private static boolean canRingFrom(IBlockState state, EnumFacing side, double hitY) {
        if (side.getAxis() == EnumFacing.Axis.Y || hitY > RING_HEIGHT) return false;
        EnumFacing facing = state.getValue(FACING);
        switch (state.getValue(ATTACHMENT)) {
            case FLOOR:
                return facing.getAxis() == side.getAxis();
            case SINGLE_WALL:
            case DOUBLE_WALL:
                return facing.getAxis() != side.getAxis();
            case CEILING:
                return true;
            default:
                return false;
        }
    }

    /** Swings the bell away from {@code side} (or along its facing), server side. */
    public boolean ring(World world, BlockPos pos, @Nullable EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        if (world.isRemote || !(tile instanceof TileEntityBell)) return false;
        if (side == null) side = world.getBlockState(pos).getValue(FACING);
        ((TileEntityBell) tile).ring(side);
        world.playSound(null, pos, PortiumSounds.BLOCK_BELL_USE, SoundCategory.BLOCKS, 2.0F, 1.0F);
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBell();
    }
}
