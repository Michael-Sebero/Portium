package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(I18n.class)
public interface I18nAccessor {

    @Accessor("i18nLocale")
    static Locale portium$getLocale() {
        throw new AssertionError();
    }
}
