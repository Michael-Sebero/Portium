package com.michaelsebero.portium.mixin.modern;

import com.michaelsebero.portium.mods116.adapter.Block116;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.16 tells a block it was replaced and added on every change of its state, on the server; 1.12 only when the block
 * itself changes (as {@code breakBlock} and {@code onBlockAdded}, which {@link Block116} answers). For a 1.16 block
 * changing state in place (a shaft turned by a wrench), this tells it too, in 1.16's order.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkBlock116 {

    @Shadow @Final private World world;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void portium$changedInPlace(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        IBlockState old = cir.getReturnValue();
        if (old == null || this.world.isRemote || old.getBlock() != state.getBlock() || !(state.getBlock() instanceof Block116)) return;
        Block116 block = (Block116) state.getBlock();
        block.onReplaced(old, this.world, pos, state, false);
        block.onBlockAdded(state, this.world, pos, old, false);
    }
}
