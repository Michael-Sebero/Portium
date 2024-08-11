package com.michaelsebero.portium.mixin.block;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(BlockDynamicLiquid.class)
public interface BlockDynamicLiquidInvoker {

    @Invoker("canFlowInto")
    boolean portium$canFlowInto(World world, BlockPos pos, IBlockState state);

    @Invoker("tryFlowInto")
    void portium$tryFlowInto(World world, BlockPos pos, IBlockState state, int level);

    @Invoker("getPossibleFlowDirections")
    Set<EnumFacing> portium$getPossibleFlowDirections(World world, BlockPos pos);
}
