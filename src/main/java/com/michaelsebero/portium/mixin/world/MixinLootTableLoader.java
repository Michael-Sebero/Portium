package com.michaelsebero.portium.mixin.world;

import com.google.gson.Gson;
import com.michaelsebero.portium.loot.DisabledLoot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTableManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Portium's built-in loot tables lose the entries of items the config switched off before 1.12 reads them. */
@Mixin(targets = "net.minecraft.world.storage.loot.LootTableManager$Loader")
public abstract class MixinLootTableLoader {

    @ModifyArg(method = "loadBuiltinLootTable", index = 2, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/common/ForgeHooks;loadLootTable(Lcom/google/gson/Gson;Lnet/minecraft/util/ResourceLocation;Ljava/lang/String;ZLnet/minecraft/world/storage/loot/LootTableManager;)Lnet/minecraft/world/storage/loot/LootTable;"))
    private String portium$pruneDisabledItems(Gson gson, ResourceLocation table, String json, boolean custom, LootTableManager manager) {
        return DisabledLoot.prune(table, json);
    }
}
