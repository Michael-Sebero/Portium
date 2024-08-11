package com.michaelsebero.portium.mixin.modern;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

/**
 * Forge 1.16's registry delegates are suppliers of what they stand for, and newer mods use them as such; Forge 1.12's
 * are made so too. (The class is package-private, so it's named.)
 */
@Mixin(targets = "net.minecraftforge.registries.RegistryDelegate", remap = false)
public abstract class MixinRegistryDelegate<T> implements Supplier<T> {

    @Shadow
    public abstract T get();
}
