package com.michaelsebero.portium.block.workstation;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.init.PortiumStats;
import com.michaelsebero.portium.inventory.PortiumGuis;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The grindstone (1.14): on a floor, wall or ceiling, needing nothing to hold it up. It repairs two of the same item
 * and takes enchantments (but curses) off for some experience ({@code ContainerGrindstone}).
 */
public class BlockGrindstone extends Block {

    public static final PropertyEnum<AttachFace> FACE = PropertyEnum.create("face", AttachFace.class);
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    /** 1.14's parts, standing on the floor facing north or south: two legs, two holders, the wheel. */
    private static final double[][] FLOOR_PARTS = {{2, 0, 6, 4, 7, 10}, {12, 0, 6, 14, 7, 10}, {2, 7, 5, 4, 13, 11}, {12, 7, 5, 14, 13, 11},
            {4, 4, 2, 12, 16, 14}};
    private static final Map<AttachFace, Map<EnumFacing, List<AxisAlignedBB>>> SHAPES = new EnumMap<>(AttachFace.class);
    private static final Map<AttachFace, Map<EnumFacing, AxisAlignedBB>> OUTLINES = new EnumMap<>(AttachFace.class);

    static {
        for (AttachFace face : AttachFace.values()) {
            Map<EnumFacing, List<AxisAlignedBB>> byFacing = new EnumMap<>(EnumFacing.class);
            Map<EnumFacing, AxisAlignedBB> outlines = new EnumMap<>(EnumFacing.class);
            for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
                List<AxisAlignedBB> parts = new ArrayList<>();
                AxisAlignedBB outline = null;
                for (double[] part : FLOOR_PARTS) {
                    AxisAlignedBB box = place(part, face, facing);
                    parts.add(box);
                    outline = outline == null ? box : outline.union(box);
                }
                byFacing.put(facing, parts);
                outlines.put(facing, outline);
            }
            SHAPES.put(face, byFacing);
            OUTLINES.put(face, outlines);
        }
    }

    /** A floor part turned to a face and facing, as 1.14's shape table has them. */
    private static AxisAlignedBB place(double[] p, AttachFace face, EnumFacing facing) {
        double x1 = p[0], y1 = p[1], z1 = p[2], x2 = p[3], y2 = p[4], z2 = p[5];
        boolean alongX = facing.getAxis() == EnumFacing.Axis.X;
        if (face == AttachFace.CEILING) {
            double a = 16 - y2, b = 16 - y1;
            y1 = a;
            y2 = b;
        }
        if (face == AttachFace.WALL) {
            // Hung on the wall behind it: the floor's up becomes the facing (south first: attached at the north side).
            double ty1 = z1, ty2 = z2;
            z1 = y1;
            z2 = y2;
            y1 = ty1;
            y2 = ty2;
            if (facing == EnumFacing.NORTH || facing == EnumFacing.WEST) {
                double a = 16 - z2, b = 16 - z1;
                z1 = a;
                z2 = b;
            }
        }
        if (alongX) {
            double tx1 = x1, tx2 = x2;
            x1 = z1;
            x2 = z2;
            z1 = tx1;
            z2 = tx2;
        }
        return new AxisAlignedBB(x1 / 16.0D, y1 / 16.0D, z1 / 16.0D, x2 / 16.0D, y2 / 16.0D, z2 / 16.0D);
    }

    public BlockGrindstone() {
        super(Material.ANVIL, MapColor.IRON);
        this.setHardness(2.0F);
        // 1.16's blast resistance 6.
        this.setResistance(10.0F);
        this.setSoundType(SoundType.STONE);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACE, AttachFace.WALL).withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACE, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)).withProperty(FACE, AttachFace.values()[Math.min(2, meta >> 2 & 3)]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | state.getValue(FACE).ordinal() << 2;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer, EnumHand hand) {
        Object[] placed = AttachFace.forPlacement(placer, facing);
        return this.getDefaultState().withProperty(FACE, (AttachFace) placed[0]).withProperty(FACING, (EnumFacing) placed[1]);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        if (world.isRemote) return true;
        player.openGui(Portium.instance, PortiumGuis.GRINDSTONE, world, pos.getX(), pos.getY(), pos.getZ());
        player.addStat(PortiumStats.INTERACT_WITH_GRINDSTONE);
        return true;
    }

    // ---- Shape ----

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return OUTLINES.get(state.getValue(FACE)).get(state.getValue(FACING));
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                                      @Nullable Entity entity, boolean isActualState) {
        for (AxisAlignedBB part : SHAPES.get(state.getValue(FACE)).get(state.getValue(FACING))) addCollisionBoxToList(pos, entityBox, boxes, part);
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

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }
}
