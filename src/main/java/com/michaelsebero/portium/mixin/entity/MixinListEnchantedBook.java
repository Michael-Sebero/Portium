package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.init.PortiumEnchantments;
import net.minecraft.util.registry.RegistryNamespaced;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

/** 1.16: librarians don't sell Soul Speed books. */
@Mixin(targets = "net.minecraft.entity.passive.EntityVillager$ListEnchantedBookForEmeralds")
public abstract class MixinListEnchantedBook {

    @SuppressWarnings("rawtypes")
    @Redirect(method = "addMerchantRecipe", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/registry/RegistryNamespaced;getRandomObject(Ljava/util/Random;)Ljava/lang/Object;"))
    private Object portium$noSoulSpeed(RegistryNamespaced registry, Random random) {
        Object enchantment = registry.getRandomObject(random);
        for (int tries = 0; enchantment == PortiumEnchantments.SOUL_SPEED && tries < 32; ++tries) enchantment = registry.getRandomObject(random);
        return enchantment;
    }
}
