package com.michaelsebero.portium.block;

import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * A Portium block whose 1.14+ outline (the selection drawn around it) isn't its collision shape, such as a lectern's
 * slanted top or scaffolding's frame. Other blocks' outlines are their collision boxes (client/render/BlockOutlines).
 */
public interface IOutlineShape {

    VoxelShape getOutlineShape(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable EntityPlayer viewer);
}
