package com.michaelsebero.portium.block.light;

import com.michaelsebero.portium.api.IWaterloggableBlock;
import com.michaelsebero.portium.init.PortiumSoundTypes;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The chain (1.16): a thin iron pillar along any axis. Its ends hold lanterns and torches like a fence post. */
public class BlockChain extends BlockRotatedPillar implements IWaterloggableBlock {

    private static final AxisAlignedBB Y_AXIS = BlockLantern.box(6.5, 0, 6.5, 9.5, 16, 9.5);
    private static final AxisAlignedBB Z_AXIS = BlockLantern.box(6.5, 6.5, 0, 9.5, 9.5, 16);
    private static final AxisAlignedBB X_AXIS = BlockLantern.box(0, 6.5, 6.5, 16, 9.5, 9.5);

    public BlockChain() {
        super(Material.IRON, MapColor.AIR);
        this.setHardness(5.0F);
        // 1.16's blast resistance 6 (1.12 keeps three fifths of what is set).
        this.setResistance(10.0F);
        this.setSoundType(PortiumSoundTypes.CHAIN);
        this.setHarvestLevel("pickaxe", 0);
        this.setCreativeTab(CreativeTabs.DECORATIONS);
        this.setDefaultState(this.blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (state.getValue(AXIS)) {
            case Y: return Y_AXIS;
            case Z: return Z_AXIS;
            default: return X_AXIS;
        }
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
        return face.getAxis() == state.getValue(AXIS) ? BlockFaceShape.CENTER_SMALL : BlockFaceShape.UNDEFINED;
    }

    /** Torches stand on a chain's top end (1.16). */
    @Override
    public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(AXIS) == EnumFacing.Axis.Y;
    }

    /** Mobs path around it, as 1.16's {@code allowsMovement} says. */
    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }
}
