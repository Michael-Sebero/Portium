package com.michaelsebero.portium.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Waterlogged blocks also take part in the translucent layer, where their water is drawn. */
@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk {

    @Shadow private ChunkCache worldView;

    @Redirect(method = "rebuildChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/Block;canRenderInLayer(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockRenderLayer;)Z", remap = false))
    private boolean portium$waterloggedTranslucent(Block block, IBlockState state, BlockRenderLayer layer, @Local BlockPos.MutableBlockPos pos) {
        return block.canRenderInLayer(state, layer) || layer == BlockRenderLayer.TRANSLUCENT && Waterlogging.isWaterlogged(this.worldView, pos);
    }
}
