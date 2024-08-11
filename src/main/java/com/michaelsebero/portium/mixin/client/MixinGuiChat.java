package com.michaelsebero.portium.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.michaelsebero.portium.client.chat.ModernChat;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** Hands the chat screen's input, keys, clicks, scrolling and drawing to 1.13's command help ({@link ModernChat}). */
@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    @Shadow protected GuiTextField inputField;
    @Shadow private String defaultInputFieldText;

    @Unique @Nullable private ModernChat portium$chat;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void portium$attach(CallbackInfo ci) {
        if (this.portium$chat != null) this.portium$chat.detach();
        this.portium$chat = ModernChat.attach((GuiChat) (Object) this, this.inputField, this.defaultInputFieldText);
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"))
    private void portium$detach(CallbackInfo ci) {
        if (this.portium$chat != null) this.portium$chat.detach();
        this.portium$chat = null;
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void portium$keyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (this.portium$chat != null && this.portium$chat.keyPressed(keyCode)) ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void portium$mouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (this.portium$chat != null && this.portium$chat.mouseClicked(mouseX, mouseY, button)) ci.cancel();
    }

    @WrapWithCondition(method = "handleMouseInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiNewChat;scroll(I)V"))
    private boolean portium$scrollSuggestions(GuiNewChat chat, int amount) {
        if (this.portium$chat == null) return true;
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        return !this.portium$chat.mouseScrolled(Integer.signum(amount), mouseX, mouseY);
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V", shift = At.Shift.AFTER))
    private void portium$drawHelp(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (this.portium$chat != null) this.portium$chat.render(mouseX, mouseY);
    }
}
