package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.client.ModernItemNames;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/** Puts Portium's answers for item models 1.13 renamed above every pack (see ModernItemNames). */
@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinSimpleReloadableResourceManager {

    @ModifyVariable(method = "reloadResources", at = @At("HEAD"), argsOnly = true)
    private List<IResourcePack> portium$modernItemNamesOnTop(List<IResourcePack> packs) {
        if (packs.contains(ModernItemNames.INSTANCE)) return packs;
        List<IResourcePack> withNames = new ArrayList<>(packs);
        withNames.add(ModernItemNames.INSTANCE);
        return withNames;
    }
}
