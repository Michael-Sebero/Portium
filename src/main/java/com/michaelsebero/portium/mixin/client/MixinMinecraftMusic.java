package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.NetherAmbience;
import com.michaelsebero.portium.client.UnderwaterMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In the Nether, the music of the biome the player is in (1.16); underwater in oceans and rivers, 1.13's underwater music. */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftMusic {

    @Inject(method = "getAmbientMusicType", at = @At("RETURN"), cancellable = true)
    private void portium$netherMusic(CallbackInfoReturnable<MusicTicker.MusicType> cir) {
        MusicTicker.MusicType water = UnderwaterMusic.music((Minecraft) (Object) this, cir.getReturnValue());
        if (water != null) {
            cir.setReturnValue(water);
            return;
        }
        if (cir.getReturnValue() != MusicTicker.MusicType.NETHER) return;
        MusicTicker.MusicType music = NetherAmbience.music((Minecraft) (Object) this);
        if (music != null) cir.setReturnValue(music);
    }
}
