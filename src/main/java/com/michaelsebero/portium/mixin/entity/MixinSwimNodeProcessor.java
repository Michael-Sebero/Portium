package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.pathfinding.SwimNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Swimming mobs path through waterlogged cells, which 1.13 judges by their water. */
@Mixin(SwimNodeProcessor.class)
public abstract class MixinSwimNodeProcessor {

    @Redirect(method = "isFree", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$waterlogged(IBlockAccess world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }
}
