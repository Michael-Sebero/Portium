package com.michaelsebero.portium.block;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.api.IWaterloggableBlock;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * Scaffolding (1.14). Each piece counts how far it is from one standing on something solid (distance 0) through
 * scaffolding: up to 6 sideways; at 7 it falls (or breaks, if it was already settled further in). Mobs stand on its top
 * unless sneaking, which lets them climb down through it; inside, it's climbed like a ladder. Holding scaffolding, the
 * whole cube can be aimed at, so pieces are added sideways and upward through the structure ({@code ItemScaffolding}).
 * 16 states: distance, and bottom (an unsupported piece, drawn with its lower frame); water is kept beside the block.
 */
public class BlockScaffolding extends Block implements IWaterloggableBlock, IOutlineShape {

    public static final PropertyInteger DISTANCE = PropertyInteger.create("distance", 0, 7);
    public static final PropertyBool BOTTOM = PropertyBool.create("bottom");
    public static final int MAX_DISTANCE = 7;

    private static final VoxelShape TOP_SLAB = VoxelShapes.or(box(0, 14, 0, 16, 16, 16), box(0, 0, 0, 2, 16, 2), box(14, 0, 0, 16, 16, 2),
            box(0, 0, 14, 2, 16, 16), box(14, 0, 14, 16, 16, 16));
    private static final VoxelShape BOTTOM_SLAB = box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape FULL = VoxelShapes.or(BOTTOM_SLAB, TOP_SLAB, box(14, 0, 0, 16, 2, 16), box(0, 0, 0, 2, 2, 16),
            box(0, 0, 0, 16, 2, 2), box(0, 0, 14, 16, 2, 16));

    public BlockScaffolding() {
        super(Material.CIRCUITS, MapColor.SAND);
        this.setSoundType(PortiumSoundTypes.SCAFFOLDING);
        this.setLightOpacity(0);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(DISTANCE, MAX_DISTANCE).withProperty(BOTTOM, false));
    }

    private static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return VoxelShapes.create(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    // ---- States ----

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, DISTANCE, BOTTOM);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DISTANCE) | (state.getValue(BOTTOM) ? 8 : 0);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(DISTANCE, meta & 7).withProperty(BOTTOM, (meta & 8) != 0);
    }

    /** 1.14's {@code getDistance}: 0 on something solid, else one more than the nearest piece beside it, or the one below's. */
    public static int getDistance(IBlockAccess world, BlockPos pos) {
        BlockPos below = pos.down();
        IBlockState under = world.getBlockState(below);
        int distance = MAX_DISTANCE;
        if (under.getBlock() instanceof BlockScaffolding) {
            distance = under.getValue(DISTANCE);
        } else if (under.getBlockFaceShape(world, below, EnumFacing.UP) == BlockFaceShape.SOLID) {
            return 0;
        }
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            IBlockState beside = world.getBlockState(pos.offset(side));
            if (beside.getBlock() instanceof BlockScaffolding) {
                distance = Math.min(distance, beside.getValue(DISTANCE) + 1);
                if (distance == 1) break;
            }
        }
        return distance;
    }

    private static boolean hasScaffoldingBelow(IBlockAccess world, BlockPos pos, int distance) {
        return distance > 0 && !(world.getBlockState(pos.down()).getBlock() instanceof BlockScaffolding);
    }

    public IBlockState stateFor(IBlockAccess world, BlockPos pos) {
        int distance = getDistance(world, pos);
        return this.getDefaultState().withProperty(DISTANCE, distance).withProperty(BOTTOM, hasScaffoldingBelow(world, pos, distance));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        return this.stateFor(world, pos);
    }

    // ---- Support ----

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) world.scheduleUpdate(pos, this, 1);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!world.isRemote) world.scheduleUpdate(pos, this, 1);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        IBlockState updated = this.stateFor(world, pos);
        if (updated.getValue(DISTANCE) == MAX_DISTANCE) {
            if (state.getValue(DISTANCE) == MAX_DISTANCE) {
                world.spawnEntity(new EntityFallingBlock(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, updated));
            } else {
                world.destroyBlock(pos, true);
            }
        } else if (updated != state) {
            world.setBlockState(pos, updated, 3);
        }
    }

    // ---- Shape ----

    @Override
    public VoxelShape getOutlineShape(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EntityPlayer viewer) {
        return this.outline(state, viewer);
    }

    /** The outline: the whole cube to someone holding scaffolding (so it can be built on from any side). */
    private VoxelShape outline(IBlockState state, @Nullable EntityLivingBase viewer) {
        if (viewer != null && viewer.getHeldItemMainhand().getItem() == Item.getItemFromBlock(this)) return VoxelShapes.fullCube();
        return state.getValue(BOTTOM) ? FULL : TOP_SLAB;
    }

    /**
     * 1.14's collision: the top, for a mob standing on it and not sneaking; the lower frame of an unsupported piece for
     * one inside it; else nothing, so it can be walked into and climbed.
     */
    private static VoxelShape collision(IBlockState state, BlockPos pos, @Nullable Entity entity) {
        if (entity == null) return VoxelShapes.empty();
        if (entity.posY > pos.getY() + 1.0D - 1.0E-5F && !entity.isSneaking()) return TOP_SLAB;
        return state.getValue(DISTANCE) != 0 && state.getValue(BOTTOM) && entity.posY > pos.getY() - 1.0E-5F ? BOTTOM_SLAB : VoxelShapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        ModernBlocks.addCollisionBoxes(collision(state, pos, entity), pos, entityBox, boxes);
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Nullable
    @Override
    @SuppressWarnings("deprecation")
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        EntityPlayer viewer = world.isRemote ? Portium.proxy.clientPlayer() : null;
        RayTraceResult hit = this.outline(state, viewer).rayTrace(start, end, pos);
        if (hit == null) return null;
        // As 1.14 does, the face is the whole cube's, so a piece can be built on from the side even between its posts.
        RayTraceResult cube = VoxelShapes.fullCube().rayTrace(start, end, pos);
        if (cube != null && cube.hitVec.squareDistanceTo(start) < hit.hitVec.squareDistanceTo(start)) {
            return new RayTraceResult(hit.hitVec, cube.sideHit, pos);
        }
        return hit;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
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
    public boolean isLadder(IBlockState state, IBlockAccess world, BlockPos pos, EntityLivingBase entity) {
        return true;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    // ---- Fire ----

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 60;
    }
}
