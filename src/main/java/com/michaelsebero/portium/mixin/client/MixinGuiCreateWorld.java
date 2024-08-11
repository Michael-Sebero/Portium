package com.michaelsebero.portium.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.world.IWorldGeneration;
import com.michaelsebero.portium.world.WorldGeneration;
import com.michaelsebero.portium.world.buffet.WorldTypeBuffet;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A "World Generation: 1.16.5 / 1.13.2 / 1.12.2" button on the Create World screen, directly below the game mode, whose
 * description lines make way for it. 1.12.2 leaves out Portium's 1.13 terrain, Buffet included; other mods'
 * generation is unaffected.
 */
@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorld extends GuiScreen {

    @Unique private static final int PORTIUM_GENERATION_BUTTON = 400;

    @Shadow private boolean inMoreWorldOptionsDisplay;
    @Shadow private int selectedIndex;
    @Shadow public String chunkProviderSettingsJson;
    @Shadow private String gameModeDesc1;
    @Shadow private String gameModeDesc2;

    @Shadow
    protected abstract void updateDisplayState();

    @Unique private String portium$generation = WorldGeneration.resolve(null);
    @Unique private GuiButton portium$generationButton;

    @Unique
    private String portium$generationLabel() {
        return I18n.format("selectWorld.portium.generation") + " " + this.portium$generation;
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void portium$addGenerationButton(CallbackInfo ci) {
        this.portium$generationButton = null;
        if (!PortiumConfig.worldGen.generationButton) return;
        this.portium$generationButton = this.addButton(new GuiButton(PORTIUM_GENERATION_BUTTON, this.width / 2 - 75, 139, 150, 20, this.portium$generationLabel()));
        this.portium$generationButton.visible = !this.inMoreWorldOptionsDisplay;
    }

    @Inject(method = "updateDisplayState", at = @At("TAIL"))
    private void portium$noGameModeDescription(CallbackInfo ci) {
        // 1.12 fills these in while the screen is set up, before the button exists, so this goes by the setting.
        if (!PortiumConfig.worldGen.generationButton) return;
        this.gameModeDesc1 = "";
        this.gameModeDesc2 = "";
    }

    @Inject(method = "showMoreWorldOptions", at = @At("TAIL"))
    private void portium$hideOnMoreOptions(boolean moreOptions, CallbackInfo ci) {
        if (this.portium$generationButton != null) this.portium$generationButton.visible = !moreOptions;
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void portium$toggleGeneration(GuiButton button, CallbackInfo ci) {
        if (button.id != PORTIUM_GENERATION_BUTTON || !button.enabled) return;
        this.portium$generation = WorldGeneration.next(this.portium$generation);
        button.displayString = this.portium$generationLabel();
        // Buffet is part of 1.13's generation.
        if (WorldGeneration.LEGACY.equals(this.portium$generation) && WorldType.WORLD_TYPES[this.selectedIndex] == WorldTypeBuffet.buffet) {
            this.selectedIndex = WorldType.DEFAULT.getId();
            this.chunkProviderSettingsJson = "";
            this.updateDisplayState();
        }
        ci.cancel();
    }

    @Inject(method = "canSelectCurWorldType", at = @At("RETURN"), cancellable = true)
    private void portium$noBuffetIn112(CallbackInfoReturnable<Boolean> cir) {
        if (WorldGeneration.LEGACY.equals(this.portium$generation) && WorldType.WORLD_TYPES[this.selectedIndex] == WorldTypeBuffet.buffet) cir.setReturnValue(false);
    }

    @Inject(method = "actionPerformed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;launchIntegratedServer(Ljava/lang/String;Ljava/lang/String;Lnet/minecraft/world/WorldSettings;)V"))
    private void portium$passGeneration(GuiButton button, CallbackInfo ci, @Local WorldSettings settings) {
        ((IWorldGeneration) (Object) settings).portium$setGeneration(this.portium$generation);
    }

    @Inject(method = "recreateFromExistingWorld", at = @At("TAIL"))
    private void portium$recreateGeneration(WorldInfo original, CallbackInfo ci) {
        this.portium$generation = WorldGeneration.of(original);
        if (this.portium$generationButton != null) this.portium$generationButton.displayString = this.portium$generationLabel();
    }
}
