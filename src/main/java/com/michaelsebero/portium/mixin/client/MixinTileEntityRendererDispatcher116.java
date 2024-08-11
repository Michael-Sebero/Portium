package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.TileEntityRendering116;
import com.michaelsebero.portium.modern.adapter.ModernTileEntity;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Newer mods bind tile entity renderers by tile entity type, which 1.12's dispatcher asks for here (see {@link TileEntityRendering116}). */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcher116 {

    @Inject(method = "getRenderer(Lnet/minecraft/tileentity/TileEntity;)Lnet/minecraft/client/renderer/tileentity/TileEntitySpecialRenderer;",
            at = @At("HEAD"), cancellable = true)
    private void portium$byType(TileEntity tile, CallbackInfoReturnable<TileEntitySpecialRenderer<?>> cir) {
        if (!(tile instanceof ModernTileEntity) || tile.isInvalid()) return;
        TileEntitySpecialRenderer<?> renderer = TileEntityRendering116.forType(((ModernTileEntity) tile).getType());
        if (renderer != null) cir.setReturnValue(renderer);
    }
}
