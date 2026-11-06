package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.init.PortiumEnchantments;
import net.minecraft.world.storage.loot.functions.EnchantRandomly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/** 1.16: Soul Speed can't generate in loot (only piglin bartering and bastion tables name it outright). */
@Mixin(EnchantRandomly.class)
public abstract class MixinEnchantRandomly {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    private boolean portium$noSoulSpeed(List list, Object enchantment) {
        return enchantment != PortiumEnchantments.SOUL_SPEED && list.add(enchantment);
    }
}
