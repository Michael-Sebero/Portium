package com.michaelsebero.portium.mixin.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * 1.16's items give their attribute modifiers by attribute (1.12's attributes, to them), where 1.12 keys them by the
 * attribute's name; the method is the same one (same SRG id), so 1.12 gets attributes where it wants names. Turned
 * into names here.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStackAttributes {

    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void portium$namesForAttributes(EntityEquipmentSlot slot, CallbackInfoReturnable<Multimap<String, AttributeModifier>> cir) {
        Multimap<?, AttributeModifier> modifiers = cir.getReturnValue();
        if (modifiers == null || modifiers.isEmpty()) return;
        boolean named = true;
        for (Object key : modifiers.keySet()) {
            if (!(key instanceof String)) {
                named = false;
                break;
            }
        }
        if (named) return;
        Multimap<String, AttributeModifier> byName = HashMultimap.create();
        for (Map.Entry<?, AttributeModifier> entry : modifiers.entries()) {
            Object key = entry.getKey();
            byName.put(key instanceof IAttribute ? ((IAttribute) key).getName() : String.valueOf(key), entry.getValue());
        }
        cir.setReturnValue(byName);
    }
}
