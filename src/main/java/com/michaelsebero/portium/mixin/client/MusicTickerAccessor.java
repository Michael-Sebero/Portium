package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MusicTicker.class)
public interface MusicTickerAccessor {

    @Accessor("currentMusic")
    ISound portium$getCurrentMusic();
}
