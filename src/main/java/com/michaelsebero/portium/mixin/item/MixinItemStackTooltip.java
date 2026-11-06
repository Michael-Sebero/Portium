package com.michaelsebero.portium.mixin.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.12's tooltips are lines of text; newer mods' code (1.13 and 1.16) fills tooltips with text components. Their
 * item code and their tooltip event listeners get a list that turns components into text as they're added, but a
 * component can still get in by other ways, and 1.12 then casts every line to text and crashes (hovering an item in
 * the creative inventory). The tooltip's lines are made text before anything reads them.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackTooltip {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void portium$linesAsText(@Nullable EntityPlayer player, ITooltipFlag flag, CallbackInfoReturnable<List<Object>> cir) {
        List<Object> lines = cir.getReturnValue();
        if (lines == null) return;
        for (int i = 0; i < lines.size(); i++) {
            Object line = lines.get(i);
            if (line instanceof ITextComponent) {
                lines.set(i, ((ITextComponent) line).getFormattedText());
            } else if (line != null && !(line instanceof String)) {
                lines.set(i, line.toString());
            }
        }
    }
}
