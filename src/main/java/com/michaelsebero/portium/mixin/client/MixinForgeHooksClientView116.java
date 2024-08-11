package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.ItemRendering116;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Notes the view an item is drawn for, which 1.16's item renderers are told and 1.12's aren't (see {@link ItemRendering116}). */
@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class MixinForgeHooksClientView116 {

    @Inject(method = "handleCameraTransforms", at = @At("HEAD"))
    private static void portium$noteView(IBakedModel model, ItemCameraTransforms.TransformType type, boolean leftHand, CallbackInfoReturnable<IBakedModel> cir) {
        ItemRendering116.viewing(type);
    }
}
