package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.audio.MovingSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.16's {@code TickableSound.finishPlaying()} sets what 1.12's moving sound keeps protected (see ClientShims116). */
@Mixin(MovingSound.class)
public interface MovingSoundAccessor {

    @Accessor("donePlaying")
    void portium$setDonePlaying(boolean done);
}
