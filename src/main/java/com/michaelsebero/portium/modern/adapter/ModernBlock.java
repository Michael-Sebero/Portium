package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.block.Block$Properties;
import com.michaelsebero.portium.modern.mc.item.BlockItemUseContext;
import com.michaelsebero.portium.modern.mc.state.StateContainer$Builder;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What a 1.13 mod's {@code Block} subclasses extend instead of 1.12's {@code Block}, and what {@code new Block(...)}
 * in 1.13 code makes: 1.12's block with 1.13's methods, which 1.12's methods call so overrides take effect. Shapes
 * give the bounding and collision boxes, {@code fillStateContainer} the state container, the block's loot table its
 * drops, and states are numbered for 1.12's metadata.
 */
public class ModernBlock extends Block implements IModernBlock {

    /** 1.12 asks whether to draw a face; 1.13 blocks say which faces their neighbours hide. */
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos, net.minecraft.util.EnumFacing side) {
        if (isSideInvisible(state, world.getBlockState(pos.offset(side)), side)) return false;
        return super.shouldSideBeRendered(state, world, pos, side);
    }

    /** Null while 1.12's constructor runs (it asks for the state container and opacity). */
    @Nullable
    private Block$Properties properties;

    public ModernBlock(Block$Properties properties) {
        super(properties.getMaterial(), properties.getMapColor());
        this.properties = properties;
        ((IBlockPropertyHolder) this).portium$applyBlockProperties(properties);
    }

    // Shapes

    @Override
    @Srg("func_196244_b")
    public VoxelShape getShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return VoxelShapes.fullCube();
    }

    @Override
    @Srg("func_196268_f")
    public VoxelShape getCollisionShape(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.properties == null || this.properties.blocksMovement() ? getShape(state, world, pos) : VoxelShapes.empty();
    }

    /** Whether the block's properties let entities collide with it (1.16's {@code canCollide}). */
    protected final boolean portium$canCollide() {
        return this.properties == null || this.properties.blocksMovement();
    }

    /** Rays meet the shape itself, not just the box around it (a cogwheel's gaps let them through). */
    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public net.minecraft.util.math.RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, net.minecraft.util.math.Vec3d start,
                                                                    net.minecraft.util.math.Vec3d end) {
        return getShape(state, world, pos).rayTrace(start, end, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.bounds(getShape(state, world, pos));
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ModernBlocks.collisionBounds(getCollisionShape(state, world, pos));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        ModernBlocks.addCollisionBoxes(getCollisionShape(state, world, pos), pos, entityBox, boxes);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return ModernBlocks.shapeOf(this, state).isFullCube();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return state.getMaterial().isOpaque() && ModernBlocks.shapeOf(this, state).isFullCube();
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return ModernBlocks.faceShape(getShape(state, world, pos));
    }

    // States

    @Srg("func_206840_a")
    protected void fillStateContainer(StateContainer$Builder<Block, IBlockState> builder) {}

    @Override
    protected BlockStateContainer createBlockState() {
        StateContainer$Builder<Block, IBlockState> builder = new StateContainer$Builder<>(this);
        fillStateContainer(builder);
        return builder.build();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return ModernBlocks.metaFromState(this, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return ModernBlocks.stateFromMeta(this, meta);
    }

    // Placement and updates

    @Nullable
    @Override
    @Srg("func_196258_a")
    public IBlockState getStateForPlacement(BlockItemUseContext context) {
        return getDefaultState();
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        EntityPlayer player = placer instanceof EntityPlayer ? (EntityPlayer) placer : null;
        IBlockState state = getStateForPlacement(BlockItemUseContext.at(world, player, hand, placer.getHeldItem(hand), pos, facing, hitX, hitY, hitZ));
        return state == null ? getDefaultState() : state;
    }

    @Override
    @Srg("func_196271_a")
    public IBlockState updatePostPlacement(IBlockState state, EnumFacing facing, IBlockState facingState, World world, BlockPos pos, BlockPos facingPos) {
        return state;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        ModernBlocks.applyShapeUpdate(this, world, pos, fromPos);
    }

    @Override
    @Srg("func_196260_a")
    public boolean isValidPosition(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }

    /** 1.12's placement check has no state to go on; any of the block's states that can stand there will do. */
    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos) && ModernBlocks.anyValidPosition(this, world, pos);
    }

    @Override
    @Srg("func_196250_a")
    public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX,
                                    float hitY, float hitZ) {
        return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX,
                                    float hitY, float hitZ) {
        return onBlockActivated(state, world, pos, player, hand, side, hitX, hitY, hitZ);
    }

    // Items, drops, tile entities and names

    @Override
    @Srg("func_185473_a")
    public ItemStack getItem(IBlockAccess world, BlockPos pos, IBlockState state) {
        return new ItemStack(this);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return getItem((IBlockAccess) world, pos, state);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (world instanceof World) getDrops(state, drops, (World) world, pos, fortune);
    }

    @Nullable
    @Override
    @Srg("func_196283_a_")
    public TileEntity createNewTileEntity(IBlockAccess world) {
        return null;
    }

    /** For 1.13 blocks that implement {@code ITileEntityProvider}, whose 1.12 method this answers. */
    @Nullable
    public TileEntity createNewTileEntity(World world, int meta) {
        return createNewTileEntity((IBlockAccess) world);
    }

    /**
     * The same under its SRG name. Only subclasses implement {@code ITileEntityProvider}, so the release build keeps the
     * name above as it is, while the game outside development calls the provider's method by this one.
     */
    @Nullable
    public TileEntity func_149915_a(World world, int meta) {
        return createNewTileEntity(world, meta);
    }

    /** Forge 1.13's, taking a block reader. */
    @Nullable
    public TileEntity createTileEntity(IBlockState state, IBlockAccess world) {
        return this instanceof ITileEntityProvider ? createNewTileEntity(world) : null;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return createTileEntity(state, (IBlockAccess) world);
    }

    @Override
    public String getTranslationKey() {
        return ModernBlocks.translationKey(this, super.getTranslationKey());
    }

    @Override
    public String getLocalizedName() {
        return ModernBlocks.localize(getTranslationKey());
    }
}
