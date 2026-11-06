package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.WorldEvents116;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** World events 1.12 doesn't know (1.14+'s composter fill and so on) play as 1.14 plays them ({@link WorldEvents116}). */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobalEvents {

    @Shadow
    private WorldClient world;

    @Inject(method = "playEvent", at = @At("HEAD"), cancellable = true)
    private void portium$events116(EntityPlayer player, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (this.world != null && WorldEvents116.play(this.world, type, pos, data)) ci.cancel();
    }
}
