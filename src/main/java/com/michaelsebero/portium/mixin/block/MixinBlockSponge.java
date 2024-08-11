package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.BlockSponge;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sponges drain waterlogged blocks, leaving the block, and soak on through them (1.13). */
@Mixin(BlockSponge.class)
public abstract class MixinBlockSponge {

    @Redirect(method = "absorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$seeWaterloggedAsWater(World world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }

    @Redirect(method = "absorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"))
    private boolean portium$drainWaterlogged(World world, BlockPos pos, IBlockState air, int flags) {
        return Waterlogging.isWaterlogged(world, pos) ? Waterlogging.setWaterlogged(world, pos, false) : world.setBlockState(pos, air, flags);
    }
}
