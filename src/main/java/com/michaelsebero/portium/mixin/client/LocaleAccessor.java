package com.michaelsebero.portium.mixin.client;

import net.minecraft.client.resources.Locale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(Locale.class)
public interface LocaleAccessor {

    @Accessor("properties")
    Map<String, String> portium$getProperties();
}
