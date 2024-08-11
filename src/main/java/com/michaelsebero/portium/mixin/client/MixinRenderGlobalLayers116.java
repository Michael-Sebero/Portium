package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.mods116.Mods116;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where 1.16's render libraries hook 1.16's world renderer (Flywheel, under Create's contraptions): the frame begins
 * before the solid layer, each block layer is followed by theirs, and reloading the renderers tells them.
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalLayers116 {

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", at = @At("HEAD"))
    private void portium$beginFrame(BlockRenderLayer layer, double partialTicks, int pass, Entity viewer, CallbackInfoReturnable<Integer> cir) {
        if (layer == BlockRenderLayer.SOLID) Mods116.beginFrame();
    }

    @Inject(method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I", at = @At("RETURN"))
    private void portium$afterLayer(BlockRenderLayer layer, double partialTicks, int pass, Entity viewer, CallbackInfoReturnable<Integer> cir) {
        Mods116.renderLayer(layer.ordinal(), TileEntityRendererDispatcher.staticPlayerX, TileEntityRendererDispatcher.staticPlayerY,
                TileEntityRendererDispatcher.staticPlayerZ);
    }

    @Inject(method = "loadRenderers", at = @At("RETURN"))
    private void portium$reloaded(CallbackInfo ci) {
        Mods116.reloadRenderers();
    }
}
