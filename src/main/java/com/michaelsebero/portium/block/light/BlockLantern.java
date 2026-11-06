package com.michaelsebero.portium.block.light;

import com.michaelsebero.portium.api.IWaterloggableBlock;
import com.michaelsebero.portium.block.BlockSupport;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The lantern (1.14) and soul lantern (1.16): stands on a block or hangs under one, held by the middle of the face
 * (so also on fences, walls, bars and chains). Falls off when that block goes; pistons break it.
 */
public class BlockLantern extends Block implements IWaterloggableBlock {

    public static final PropertyBool HANGING = PropertyBool.create("hanging");

    private static final AxisAlignedBB STANDING_BODY = box(5, 0, 5, 11, 7, 11);
    private static final AxisAlignedBB STANDING_TOP = box(6, 7, 6, 10, 9, 10);
    private static final AxisAlignedBB STANDING = box(5, 0, 5, 11, 9, 11);
    private static final AxisAlignedBB HANGING_BODY = box(5, 1, 5, 11, 8, 11);
    private static final AxisAlignedBB HANGING_TOP = box(6, 8, 6, 10, 10, 10);
    private static final AxisAlignedBB HANGING_ALL = box(5, 1, 5, 11, 10, 11);

    public BlockLantern(int light) {
        super(Material.IRON);
        this.setHardness(3.5F);
        this.setSoundType(PortiumSoundTypes.LANTERN);
        this.setHarvestLevel("pickaxe", 0);
        this.lightValue = light;
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(HANGING, false));
    }

    static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HANGING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(HANGING, (meta & 1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(HANGING) ? 1 : 0;
    }

    // ---- Placement ----

    /** The side the lantern holds on by: up when hanging, down when standing. */
    private static EnumFacing attachedTo(IBlockState state) {
        return state.getValue(HANGING) ? EnumFacing.UP : EnumFacing.DOWN;
    }

    private static boolean canStay(IBlockState state, IBlockAccess world, BlockPos pos) {
        EnumFacing side = attachedTo(state);
        return BlockSupport.hasCenterSupport(world, pos.offset(side), side.getOpposite());
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return super.canPlaceBlockAt(world, pos)
                && (canStay(this.getDefaultState(), world, pos) || canStay(this.getDefaultState().withProperty(HANGING, true), world, pos));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        // 1.14: the first up or down the player faces (the clicked block's side first) that can hold it.
        for (EnumFacing direction : BlockSupport.nearestLookingDirections(placer, facing)) {
            if (direction.getAxis() != EnumFacing.Axis.Y) continue;
            IBlockState state = this.getDefaultState().withProperty(HANGING, direction == EnumFacing.UP);
            if (canStay(state, world, pos)) return state;
        }
        return this.getDefaultState();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (!canStay(state, world, pos)) {
            this.dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    // ---- Shape ----

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(HANGING) ? HANGING_ALL : STANDING;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        boolean hanging = state.getValue(HANGING);
        addCollisionBoxToList(pos, entityBox, boxes, hanging ? HANGING_BODY : STANDING_BODY);
        addCollisionBoxToList(pos, entityBox, boxes, hanging ? HANGING_TOP : STANDING_TOP);
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
        return BlockFaceShape.UNDEFINED;
    }

    /** Mobs path around it, as 1.14's {@code allowsMovement} says. */
    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
