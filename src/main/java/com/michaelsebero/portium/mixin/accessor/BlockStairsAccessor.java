package com.michaelsebero.portium.mixin.accessor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Sets the block stairs copy, for 1.16 mods' stairs, which name it only once it's registered. */
@Mixin(BlockStairs.class)
public interface BlockStairsAccessor {

    @Accessor("modelBlock")
    @Mutable
    void portium$setModelBlock(Block block);

    @Accessor("modelState")
    @Mutable
    void portium$setModelState(IBlockState state);
}
