package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.compat.LegacyItemNbt;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Custom names stored as 1.13+ JSON text (what Portium gives the ominous banner) show as their text, translated for
 * the reader. The tooltip side is {@code client.MixinItemStackJsonNameTooltip}.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackJsonName {

    @Shadow
    @Nullable
    public abstract NBTTagCompound getSubCompound(String key);

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void portium$jsonName(CallbackInfoReturnable<String> cir) {
        String name = cir.getReturnValue();
        if (name != null && name.startsWith("{") && this.portium$hasJsonName()) cir.setReturnValue(LegacyItemNbt.textToLegacy(name));
    }

    private boolean portium$hasJsonName() {
        NBTTagCompound display = this.getSubCompound("display");
        return display != null && display.hasKey("Name", 8) && display.getString("Name").startsWith("{");
    }
}
