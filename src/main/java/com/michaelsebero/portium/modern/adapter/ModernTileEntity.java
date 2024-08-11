package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.Srg;
import com.michaelsebero.portium.modern.mc.tileentity.TileEntityType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What a 1.13 mod's tile entities extend instead of 1.12's {@code TileEntity}: one made with its 1.13 type, which
 * survives changes to its block's state as 1.13's tile entities do (Forge 1.12 replaces a modded tile entity on any
 * state change).
 */
public class ModernTileEntity extends TileEntity {

    private final TileEntityType<?> type;

    public ModernTileEntity(TileEntityType<?> type) {
        this.type = type;
    }

    @Srg("func_200662_C")
    public TileEntityType<?> getType() {
        return this.type;
    }

    @Srg("func_195044_w")
    public IBlockState getBlockState() {
        return this.world == null ? Blocks.AIR.getDefaultState() : this.world.getBlockState(this.pos);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }
}
