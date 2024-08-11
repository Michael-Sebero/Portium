package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.datapack.DataPackAdvancements;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Map;

/** Data pack advancements join, and override, the world's and the game's own when advancements load (1.13). */
@Mixin(AdvancementManager.class)
public abstract class MixinAdvancementManager {

    @ModifyArg(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementList;loadAdvancements(Ljava/util/Map;)V"))
    private Map<ResourceLocation, Advancement.Builder> portium$addDataPackAdvancements(Map<ResourceLocation, Advancement.Builder> builders) {
        DataPackAdvancements.addTo(builders);
        return builders;
    }
}
