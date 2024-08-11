package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.NetherAmbience;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In the Nether, the music of the biome the player is in (1.16). */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftMusic {

    @Inject(method = "getAmbientMusicType", at = @At("RETURN"), cancellable = true)
    private void portium$netherMusic(CallbackInfoReturnable<MusicTicker.MusicType> cir) {
        if (cir.getReturnValue() != MusicTicker.MusicType.NETHER) return;
        MusicTicker.MusicType music = NetherAmbience.music((Minecraft) (Object) this);
        if (music != null) cir.setReturnValue(music);
    }
}
