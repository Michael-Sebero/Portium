package com.michaelsebero.portium.mixin.block;

import com.michaelsebero.portium.block.nether.BlockSoulFire;
import com.michaelsebero.portium.init.PortiumBlocks;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fire lit on soul sand or soul soil burns as soul fire (1.16). */
@Mixin(BlockFire.class)
public abstract class MixinBlockFire {

    @Inject(method = "onBlockAdded", at = @At("HEAD"), cancellable = true)
    private void portium$soulFire(World world, BlockPos pos, IBlockState state, CallbackInfo ci) {
        if (PortiumBlocks.SOUL_FIRE == null || !BlockSoulFire.isSoulFireBase(world.getBlockState(pos.down()).getBlock())) return;
        world.setBlockState(pos, PortiumBlocks.SOUL_FIRE.getDefaultState());
        ci.cancel();
    }
}
