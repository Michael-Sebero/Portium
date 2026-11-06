package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

/** As in 1.14, a JSON custom name's colours stay italic in the tooltip (1.12 puts the italics before the colour code). */
@Mixin(ItemStack.class)
public abstract class MixinItemStackJsonNameTooltip {

    @Shadow
    @Nullable
    public abstract NBTTagCompound getSubCompound(String key);

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void portium$italicJsonName(@Nullable EntityPlayer player, ITooltipFlag flag, CallbackInfoReturnable<List<String>> cir) {
        List<String> lines = cir.getReturnValue();
        NBTTagCompound display = this.getSubCompound("display");
        if (lines == null || lines.isEmpty() || display == null || !display.getString("Name").startsWith("{")) return;
        lines.set(0, lines.get(0).replaceAll("(\u00a7[0-9a-fA-F])", "$1\u00a7o"));
    }
}
