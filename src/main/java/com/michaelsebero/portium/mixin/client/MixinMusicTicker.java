package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.NetherAmbience;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Crossing into another Nether biome lets the playing track finish, as 1.16's biome music does. */
@Mixin(MusicTicker.class)
public abstract class MixinMusicTicker {

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ResourceLocation;equals(Ljava/lang/Object;)Z"))
    private boolean portium$keepMusic(ResourceLocation wanted, Object playing) {
        return wanted.equals(playing) || NetherAmbience.keepsPlayingMusic(wanted);
    }
}
