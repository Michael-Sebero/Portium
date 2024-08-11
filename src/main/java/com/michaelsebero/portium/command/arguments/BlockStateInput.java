package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** A block state to place, with optional block entity data, from {@code setblock}/{@code fill}/{@code clone}. */
public class BlockStateInput {

    private final IBlockState state;
    @Nullable
    private final NBTTagCompound tag;
    /** 1.13's {@code waterlogged} property, kept beside the 1.12 state. */
    private final boolean waterlogged;

    public BlockStateInput(IBlockState state, @Nullable NBTTagCompound tag) {
        this(state, tag, false);
    }

    public BlockStateInput(IBlockState state, @Nullable NBTTagCompound tag, boolean waterlogged) {
        this.state = state;
        this.tag = tag;
        this.waterlogged = waterlogged;
    }

    public IBlockState getState() {
        return this.state;
    }

    /** Places the block (and its data), as 1.13's {@code BlockStateInput.place}. Changing only the water is a change too. */
    public boolean place(World world, BlockPos pos, int flags) {
        boolean placed = world.setBlockState(pos, this.state, flags);
        boolean watered = Waterlogging.canWaterlog(this.state) && Waterlogging.setWaterlogged(world, pos, this.waterlogged);
        if (!placed && !watered) return false;
        if (this.tag != null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile != null) {
                NBTTagCompound data = this.tag.copy();
                data.setInteger("x", pos.getX());
                data.setInteger("y", pos.getY());
                data.setInteger("z", pos.getZ());
                tile.readFromNBT(data);
            }
        }
        return true;
    }
}
