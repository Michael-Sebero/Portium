package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockFarmland;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Waterlogged blocks hydrate farmland (1.13). */
@Mixin(BlockFarmland.class)
public abstract class MixinBlockFarmland {

    @Redirect(method = "hasWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"))
    private IBlockState portium$seeWaterloggedAsWater(World world, BlockPos pos) {
        return Waterlogging.withWater(world, pos);
    }
}
