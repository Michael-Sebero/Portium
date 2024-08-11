package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.GuiModernWorld;
import com.michaelsebero.portium.compat.ModernSaves;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListWorldSelectionEntry;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.world.storage.WorldSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Worlds 1.13 saved get Portium's conversion prompt, with a backup offer, instead of 1.12's version warning. */
@Mixin(GuiListWorldSelectionEntry.class)
public abstract class MixinGuiListWorldSelectionEntry {

    @Shadow @Final private Minecraft client;
    @Shadow @Final private GuiWorldSelection worldSelScreen;
    @Shadow @Final private WorldSummary worldSummary;

    @Shadow
    protected abstract void loadWorld();

    @Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
    private void portium$offerConversion(CallbackInfo ci) {
        if (!ModernSaves.isModern(((WorldSummaryAccessor) this.worldSummary).portium$getVersionId())) return;
        this.client.displayGuiScreen(new GuiModernWorld(this.worldSelScreen, this.worldSummary.getFileName(), this.worldSummary.getVersionName(), this::portium$load));
        ci.cancel();
    }

    @Unique
    private void portium$load() {
        this.loadWorld();
    }
}
