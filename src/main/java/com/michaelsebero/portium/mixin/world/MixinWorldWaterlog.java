package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A waterlogged block destroyed by the world (mobs breaking doors, falling blocks...) leaves its water (1.13). */
@Mixin(World.class)
public abstract class MixinWorldWaterlog {

    @Redirect(method = "destroyBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"))
    private boolean portium$leaveWater(World world, BlockPos pos, IBlockState air, int flags) {
        return world.setBlockState(pos, Waterlogging.leftBehind(world, pos, air), flags);
    }
}
