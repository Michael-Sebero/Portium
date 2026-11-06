package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.render.MapIcons116;
import net.minecraft.world.storage.MapDecoration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Map markers drawn from 1.13's icon sheet (16 to a row), with their names; through Forge's hook for custom sheets. */
@Mixin(value = MapDecoration.class, remap = false)
public abstract class MixinMapDecorationRender {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void portium$render116(int index, CallbackInfoReturnable<Boolean> cir) {
        if (MapIcons116.render((MapDecoration) (Object) this, index)) cir.setReturnValue(true);
    }
}
