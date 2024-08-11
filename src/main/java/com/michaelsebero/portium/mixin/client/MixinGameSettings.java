package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.BiomeBlend;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;

/** The Biome Blend setting as one of the game's options: its slider value, its label, and its line in options.txt. */
@Mixin(GameSettings.class)
public abstract class MixinGameSettings {

    @Shadow protected Minecraft mc;
    @Shadow private File optionsFile;

    @Inject(method = "setOptionFloatValue", at = @At("HEAD"), cancellable = true)
    private void portium$setBiomeBlend(GameSettings.Options option, float value, CallbackInfo ci) {
        if (!BiomeBlend.isOption(option)) return;
        // Rebuilds the world's meshes with the new colours, as 1.13 does; only when the value changes, as the slider
        // reports every drag.
        if (BiomeBlend.setRadius((int) value) && this.mc.renderGlobal != null) this.mc.renderGlobal.loadRenderers();
        ci.cancel();
    }

    @Inject(method = "getOptionFloatValue", at = @At("HEAD"), cancellable = true)
    private void portium$getBiomeBlend(GameSettings.Options option, CallbackInfoReturnable<Float> cir) {
        if (BiomeBlend.isOption(option)) cir.setReturnValue((float) BiomeBlend.getRadius());
    }

    @Inject(method = "getKeyBinding", at = @At("HEAD"), cancellable = true)
    private void portium$labelBiomeBlend(GameSettings.Options option, CallbackInfoReturnable<String> cir) {
        if (BiomeBlend.isOption(option)) cir.setReturnValue(BiomeBlend.label());
    }

    @Inject(method = "loadOptions", at = @At("TAIL"))
    private void portium$loadBiomeBlend(CallbackInfo ci) {
        BiomeBlend.load(this.optionsFile);
    }

    @Redirect(method = "saveOptions", at = @At(value = "INVOKE", target = "Lorg/apache/commons/io/IOUtils;closeQuietly(Ljava/io/Writer;)V", remap = false))
    private void portium$saveBiomeBlend(Writer writer) {
        if (writer instanceof PrintWriter) ((PrintWriter) writer).println(BiomeBlend.KEY + ":" + BiomeBlend.getRadius());
        IOUtils.closeQuietly(writer);
    }
}
