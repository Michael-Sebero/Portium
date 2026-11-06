package com.michaelsebero.portium.mixin.core;

import com.google.common.collect.BiMap;
import com.michaelsebero.portium.init.PortiumNames;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Portium's old portium: names find what Portium now registers under minecraft: (honey block, cornflower, its
 * sounds...). Forge's aliases were added for this but don't outlast startup, so lookups by portium: name in Portium's
 * code, data and commands found nothing.
 */
@Mixin(value = ForgeRegistry.class, remap = false)
public abstract class MixinForgeRegistryPortiumNames<V extends IForgeRegistryEntry<V>> {

    @Shadow
    @Final
    private BiMap<ResourceLocation, V> names;

    @Shadow
    public abstract Class<V> getRegistrySuperType();

    @Inject(method = "getValue(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/registries/IForgeRegistryEntry;", at = @At("HEAD"),
            cancellable = true)
    private void portium$movedValue(ResourceLocation key, CallbackInfoReturnable<V> cir) {
        V moved = this.portium$moved(key);
        if (moved != null) cir.setReturnValue(moved);
    }

    @Inject(method = "containsKey", at = @At("HEAD"), cancellable = true)
    private void portium$movedKey(ResourceLocation key, CallbackInfoReturnable<Boolean> cir) {
        if (this.portium$moved(key) != null) cir.setReturnValue(true);
    }

    private V portium$moved(ResourceLocation key) {
        if (key == null || !"portium".equals(key.getNamespace()) || this.names.containsKey(key)) return null;
        ResourceLocation moved = PortiumNames.movedFrom(this.getRegistrySuperType(), key);
        return moved == null ? null : this.names.get(moved);
    }
}
