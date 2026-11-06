package com.michaelsebero.portium.block.jigsaw;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.tile.TileEntityJigsaw;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.14's jigsaw block: a structure's connection point, saved into templates with structure blocks. It faces one of
 * twelve ways ({@link JigsawOrientation}) and remembers its name, the name it joins, the pool it draws from, what it
 * turns into and how it joins ({@link TileEntityJigsaw}); operators in creative edit it on its screen.
 */
public class BlockJigsaw extends Block implements ITileEntityProvider {

    public static final PropertyEnum<JigsawOrientation> ORIENTATION = PropertyEnum.create("orientation", JigsawOrientation.class);

    public BlockJigsaw() {
        super(Material.IRON, MapColor.SILVER);
        this.setBlockUnbreakable();
        this.setResistance(3600000.0F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(ORIENTATION, JigsawOrientation.NORTH_UP));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ORIENTATION);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        JigsawOrientation[] values = JigsawOrientation.values();
        return this.getDefaultState().withProperty(ORIENTATION, values[meta % values.length]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(ORIENTATION).ordinal();
    }

    /** 1.16: the front faces away from the face clicked; placed on a floor or ceiling, its top faces the placer. */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
                                            EntityLivingBase placer) {
        EnumFacing top = facing.getAxis() == EnumFacing.Axis.Y ? placer.getHorizontalFacing().getOpposite() : EnumFacing.UP;
        return this.getDefaultState().withProperty(ORIENTATION, JigsawOrientation.of(facing, top));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        JigsawOrientation orientation = state.getValue(ORIENTATION);
        return state.withProperty(ORIENTATION, JigsawOrientation.of(rotation.rotate(orientation.front), rotation.rotate(orientation.top)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        JigsawOrientation orientation = state.getValue(ORIENTATION);
        return state.withProperty(ORIENTATION, JigsawOrientation.of(mirror.mirror(orientation.front), mirror.mirror(orientation.top)));
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityJigsaw();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                                    float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityJigsaw) || !player.canUseCommandBlock()) return false;
        if (world.isRemote) Portium.proxy.openJigsaw((TileEntityJigsaw) tile);
        return true;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, net.minecraft.util.math.RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this);
    }
}
