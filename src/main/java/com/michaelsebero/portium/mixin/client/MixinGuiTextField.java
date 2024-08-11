package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.chat.IChatTextField;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/** 1.13's text field additions used by the chat screen: a text formatter, a suggestion shown after the cursor, a change listener. */
@Mixin(GuiTextField.class)
public abstract class MixinGuiTextField implements IChatTextField {

    @Shadow @Final private FontRenderer fontRenderer;
    @Shadow public int x;
    @Shadow public int y;
    @Shadow public int height;
    @Shadow private String text;
    @Shadow private int maxStringLength;
    @Shadow private boolean enableBackgroundDrawing;
    @Shadow private int lineScrollOffset;
    @Shadow private int cursorPosition;
    @Shadow private boolean visible;

    @Shadow public abstract int getWidth();

    @Unique @Nullable private BiFunction<String, Integer, String> portium$formatter;
    @Unique @Nullable private String portium$suggestion;
    @Unique @Nullable private Runnable portium$listener;
    @Unique private String portium$notifiedText = "";

    @Override
    public void portium$setFormatter(@Nullable BiFunction<String, Integer, String> formatter) {
        this.portium$formatter = formatter;
    }

    @Override
    public void portium$setSuggestion(@Nullable String suggestion) {
        this.portium$suggestion = suggestion;
    }

    @Override
    public void portium$setListener(@Nullable Runnable listener) {
        this.portium$listener = listener;
        this.portium$notifiedText = this.text;
    }

    @Redirect(method = "drawTextBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 0))
    private int portium$formatBeforeCursor(FontRenderer font, String visible, float x, float y, int color) {
        return font.drawStringWithShadow(this.portium$formatter == null ? visible : this.portium$formatter.apply(visible, this.lineScrollOffset), x, y, color);
    }

    @Redirect(method = "drawTextBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I", ordinal = 1))
    private int portium$formatAfterCursor(FontRenderer font, String visible, float x, float y, int color) {
        return font.drawStringWithShadow(this.portium$formatter == null ? visible : this.portium$formatter.apply(visible, this.cursorPosition), x, y, color);
    }

    @Inject(method = "drawTextBox", at = @At("TAIL"))
    private void portium$drawSuggestion(CallbackInfo ci) {
        if (!this.visible || this.portium$suggestion == null || this.cursorPosition < this.text.length() || this.text.length() >= this.maxStringLength) return;
        int scroll = Math.min(this.lineScrollOffset, this.text.length());
        String shown = this.fontRenderer.trimStringToWidth(this.text.substring(scroll), this.getWidth());
        if (this.cursorPosition - scroll > shown.length()) return;
        String before = this.text.substring(scroll, this.cursorPosition);
        int left = this.enableBackgroundDrawing ? this.x + 4 : this.x;
        int top = this.enableBackgroundDrawing ? this.y + (this.height - 8) / 2 : this.y;
        int at = before.isEmpty() ? left - 1 : left + this.fontRenderer.getStringWidth(before);
        this.fontRenderer.drawStringWithShadow(this.portium$suggestion, at, top, 0xFF808080);
    }

    @Inject(method = {"setText", "writeText", "deleteFromCursor"}, at = @At("TAIL"))
    private void portium$notifyChange(CallbackInfo ci) {
        if (this.portium$listener == null || this.text.equals(this.portium$notifiedText)) return;
        this.portium$notifiedText = this.text;
        this.portium$listener.run();
    }
}
