package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.bee.StickyBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockPistonStructureHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.15's piston stickiness: a sticky block pulls along only what sticks to it, and honey and slime don't stick to each
 * other ({@link StickyBlocks}). 1.12 pulled whatever touched a slime block.
 */
@Mixin(BlockPistonStructureHelper.class)
public abstract class MixinBlockPistonStructureHelper {

    @Shadow @Final private World world;
    @Shadow @Final private EnumFacing moveDirection;

    @Shadow
    protected abstract boolean addBlockLine(BlockPos origin, EnumFacing facing);

    /** The block behind a sticky block in the line being pushed: it stops the line if the two don't stick. */
    @Redirect(method = "addBlockLine", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/block/Block;isAir(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Z",
            remap = false))
    private boolean portium$stopAtNonSticking(Block block, IBlockState state, IBlockAccess access, BlockPos pos) {
        if (block.isAir(state, access, pos)) return true;
        Block previous = this.world.getBlockState(pos.offset(this.moveDirection)).getBlock();
        return !StickyBlocks.canStickTo(previous, block);
    }

    /** A sticky block's neighbours to the sides join the move only if they stick to it. */
    @Redirect(method = "addBranchingBlocks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/state/BlockPistonStructureHelper;addBlockLine(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;)Z"))
    private boolean portium$branchIfSticking(BlockPistonStructureHelper self, BlockPos neighbor, EnumFacing side) {
        Block from = this.world.getBlockState(neighbor.offset(side.getOpposite())).getBlock();
        Block block = this.world.getBlockState(neighbor).getBlock();
        return !StickyBlocks.canStickTo(block, from) || this.addBlockLine(neighbor, side);
    }
}
