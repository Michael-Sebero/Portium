package com.michaelsebero.portium.mixin.modern;

import com.michaelsebero.portium.mods116.adapter.Block116;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * 1.16's diagonal neighbour updates after a block changes (unless the flags hold them back), which 1.12 lacks: told
 * to the old and new 1.16 blocks, as 1.16's world does. Create's kinetic blocks rejoin their network this way.
 */
@Mixin(World.class)
public abstract class MixinWorldBlock116 {

    /** Forge's method, so its name is the same everywhere. */
    @Inject(method = "markAndNotifyBlock", at = @At("TAIL"), remap = false)
    private void portium$diagonalUpdates(BlockPos pos, @Nullable Chunk chunk, IBlockState old, IBlockState now, int flags, CallbackInfo ci) {
        if ((flags & 16) != 0) return;
        World world = (World) (Object) this;
        int passed = flags & -34;
        if (old.getBlock() instanceof Block116) ((Block116) old.getBlock()).updateDiagonalNeighbors(old, world, pos, passed, 511);
        if (now.getBlock() instanceof Block116) ((Block116) now.getBlock()).updateDiagonalNeighbors(now, world, pos, passed, 511);
    }
}
