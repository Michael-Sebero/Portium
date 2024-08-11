package com.michaelsebero.portium.mixin.item;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.init.PortiumItems;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Elytra can be repaired with phantom membranes (1.13). Leather still works, for existing recipes and habits. */
@Mixin(ItemElytra.class)
public abstract class MixinItemElytra {

    @Inject(method = "getIsRepairable", at = @At("HEAD"), cancellable = true)
    private void portium$phantomMembrane(ItemStack toRepair, ItemStack repair, CallbackInfoReturnable<Boolean> cir) {
        if (PortiumConfig.mechanics.elytraMembraneRepair && PortiumItems.PHANTOM_MEMBRANE != null && repair.getItem() == PortiumItems.PHANTOM_MEMBRANE) {
            cir.setReturnValue(true);
        }
    }
}
