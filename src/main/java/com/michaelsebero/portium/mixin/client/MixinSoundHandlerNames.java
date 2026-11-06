package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Portium's sound definitions (its sounds.json) belong to its sound events, which are named {@code minecraft:} as 1.13
 * to 1.16 name them: each is filed under its event's name as it loads.
 */
@Mixin(SoundHandler.class)
public abstract class MixinSoundHandlerNames {

    @ModifyVariable(method = "loadSoundResource", at = @At("HEAD"), argsOnly = true)
    private ResourceLocation portium$eventName(ResourceLocation location) {
        return PortiumSounds.definitionName(location);
    }
}
