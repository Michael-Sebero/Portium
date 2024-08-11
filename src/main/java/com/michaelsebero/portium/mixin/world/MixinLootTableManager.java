package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.datapack.DataPackLootTables;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Loot tables in enabled data packs take precedence over the world's and the game's own (1.13). */
@Mixin(LootTableManager.class)
public abstract class MixinLootTableManager {

    @Unique
    private final Map<ResourceLocation, Optional<LootTable>> portium$dataPackTables = new HashMap<>();

    @Inject(method = "getLootTableFromLocation", at = @At("HEAD"), cancellable = true)
    private void portium$getFromDataPacks(ResourceLocation id, CallbackInfoReturnable<LootTable> cir) {
        if (DataPackManager.get() == null) return;
        Optional<LootTable> table = this.portium$dataPackTables.get(id);
        if (table == null) {
            table = Optional.ofNullable(DataPackLootTables.load(id, (LootTableManager) (Object) this));
            this.portium$dataPackTables.put(id, table);
        }
        table.ifPresent(cir::setReturnValue);
    }

    @Inject(method = "reloadLootTables", at = @At("HEAD"))
    private void portium$clearDataPackTables(CallbackInfo ci) {
        this.portium$dataPackTables.clear();
    }
}
