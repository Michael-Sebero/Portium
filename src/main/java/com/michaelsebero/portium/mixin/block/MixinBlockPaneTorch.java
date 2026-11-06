package com.michaelsebero.portium.mixin.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;

/** 1.16: torches (and anything needing centre support) stand on glass panes and iron bars. */
@Mixin(BlockPane.class)
public abstract class MixinBlockPaneTorch extends Block {

    private MixinBlockPaneTorch(Material material) {
        super(material);
    }

    @Override
    public boolean canPlaceTorchOnTop(IBlockState state, IBlockAccess world, BlockPos pos) {
        return true;
    }
}
