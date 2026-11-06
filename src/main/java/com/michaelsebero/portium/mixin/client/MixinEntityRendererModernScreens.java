package com.michaelsebero.portium.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.michaelsebero.portium.diagnostics.ProblemReport;
import com.michaelsebero.portium.modern.ModernTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.EntityRenderer;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Newer mods' screens are kept from crashing the game while drawn: one that reaches a part of newer Minecraft that
 * Portium doesn't cover yet is closed and reported, as their tile entities' ticks are switched off. Other errors
 * are the mod's own and crash as usual.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererModernScreens {

    @WrapOperation(method = "updateCameraAndRender", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/GuiScreen;IIF)V", remap = false))
    private void portium$drawModernScreen(GuiScreen screen, int mouseX, int mouseY, float partialTicks, Operation<Void> original) {
        if (screen.getClass().getClassLoader() == GuiScreen.class.getClassLoader()) {
            original.call(screen, mouseX, mouseY, partialTicks);
            return;
        }
        try {
            original.call(screen, mouseX, mouseY, partialTicks);
        } catch (LinkageError e) {
            portium$close(screen, e);
        } catch (RuntimeException e) {
            if (!ModernTicks.isLayerFault(e)) throw e;
            portium$close(screen, e);
        }
    }

    private static void portium$close(GuiScreen screen, Throwable error) {
        LogManager.getLogger("Portium/Mods").error("Mod problem: the screen {} was closed; it needs something Portium doesn't provide yet",
                screen.getClass().getName(), error);
        ProblemReport.record("The screen " + screen.getClass().getName() + " was closed while being drawn", error);
        // What the screen left of its drawing state: 1.12 draws the next frame from a clean one.
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        try {
            tessellator.getBuffer().finishDrawing();
        } catch (IllegalStateException notDrawing) {
            // Nothing was being drawn.
        }
        Minecraft.getMinecraft().displayGuiScreen(null);
    }
}
