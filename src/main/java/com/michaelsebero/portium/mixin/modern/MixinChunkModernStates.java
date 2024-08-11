package com.michaelsebero.portium.mixin.modern;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.modern.state.IModernStateChunk;
import com.michaelsebero.portium.modern.state.ModernStateData;
import com.michaelsebero.portium.modern.state.ModernStates;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunks keep the full states of 1.13 blocks with more than 16 states, which 1.12's metadata can't hold (see
 * {@link ModernStates}): kept as they're set, and given back wherever the chunk is read.
 */
@Mixin(Chunk.class)
public abstract class MixinChunkModernStates implements IModernStateChunk {

    @Shadow @Final private World world;

    @Unique
    private final ModernStateData portium$modernStates = new ModernStateData();

    @Override
    public ModernStateData portium$getModernStates() {
        return this.portium$modernStates;
    }

    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/block/state/IBlockState;", at = @At("RETURN"))
    private IBlockState portium$fullState(IBlockState state, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y,
                                          @Local(argsOnly = true, ordinal = 2) int z) {
        return ModernStates.full((Chunk) (Object) this, state, x, y, z);
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void portium$keepFullState(BlockPos pos, IBlockState state, CallbackInfoReturnable<IBlockState> cir) {
        ModernStates.set((Chunk) (Object) this, this.world, pos, state);
    }
}
