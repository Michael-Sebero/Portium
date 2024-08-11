package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.BlockLilyPad;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A lily pad stays on a waterlogged block as on a water source, since 1.13 checks the fluid below it. */
@Mixin(BlockLilyPad.class)
public abstract class MixinBlockLilyPad {

    @Redirect(method = "canBlockStay", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$waterBelow(World world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }
}
