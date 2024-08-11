package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.client.BiomeBlend;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;
import java.util.Objects;

/** A Biome Blend slider at the end of the Video Settings, where 1.13 has it. */
@Mixin(GuiVideoSettings.class)
public abstract class MixinGuiVideoSettings {

    @ModifyArg(method = "initGui", index = 6, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiOptionsRowList;<init>(Lnet/minecraft/client/Minecraft;IIIII[Lnet/minecraft/client/settings/GameSettings$Options;)V"))
    private GameSettings.Options[] portium$addBiomeBlend(GameSettings.Options[] options) {
        if (!PortiumConfig.visuals.biomeBlend) return options;
        // Without VBOs 1.12 leaves an empty slot where it dropped that option; the slider goes after the real ones.
        GameSettings.Options[] present = Arrays.stream(options).filter(Objects::nonNull).toArray(GameSettings.Options[]::new);
        GameSettings.Options[] extended = Arrays.copyOf(present, present.length + 1);
        extended[present.length] = BiomeBlend.option();
        return extended;
    }
}
