package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.block.water.IWaterloggable;
import com.michaelsebero.portium.modern.client.ModernRenderContext;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockFluidRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.MinecraftForgeClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the water in waterlogged cells, in the translucent layer. Portium's aquatic blocks (seagrass, kelp, coral,
 * sea pickles, the conduit) render there only while waterlogged. Waterlogged vanilla blocks draw themselves in their
 * own layer; here only their water is added, unless the block is translucent itself (stained glass panes).
 */
@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher {

    @Shadow
    @Final
    private BlockFluidRenderer fluidRenderer;

    /** Remembers the block being drawn, for Forge 1.13 models' model data (see {@code ModernRenderContext}). */
    @Inject(method = "renderBlock", at = @At("HEAD"))
    private void portium$beginModernContext(IBlockState state, BlockPos pos, IBlockAccess world, BufferBuilder buffer, CallbackInfoReturnable<Boolean> cir) {
        ModernRenderContext.begin(world, pos);
    }

    @Inject(method = "renderBlock", at = @At("RETURN"))
    private void portium$endModernContext(IBlockState state, BlockPos pos, IBlockAccess world, BufferBuilder buffer, CallbackInfoReturnable<Boolean> cir) {
        ModernRenderContext.end();
    }

    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void portium$renderWaterInBlock(IBlockState state, BlockPos pos, IBlockAccess world, BufferBuilder buffer, CallbackInfoReturnable<Boolean> cir) {
        if (MinecraftForgeClient.getRenderLayer() != BlockRenderLayer.TRANSLUCENT) return;
        if (state.getBlock() instanceof IWaterloggable && ((IWaterloggable) state.getBlock()).isWaterlogged(state)) {
            cir.setReturnValue(this.fluidRenderer.renderFluid(world, Blocks.WATER.getDefaultState(), pos, buffer));
        } else if (Waterlogging.isWaterlogged(world, pos)) {
            boolean water = this.fluidRenderer.renderFluid(world, Blocks.WATER.getDefaultState(), pos, buffer);
            if (!state.getBlock().canRenderInLayer(state, BlockRenderLayer.TRANSLUCENT)) cir.setReturnValue(water);
        }
    }
}
