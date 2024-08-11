package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.loot.LootOrigin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records which container is being filled, for loot functions that need a position. */
@Mixin(TileEntityLockableLoot.class)
public abstract class MixinTileEntityLockableLoot {

    @Inject(method = "fillWithLoot", at = @At("HEAD"))
    private void portium$recordOrigin(EntityPlayer player, CallbackInfo ci) {
        LootOrigin.set(((TileEntity) (Object) this).getPos());
    }

    @Inject(method = "fillWithLoot", at = @At("RETURN"))
    private void portium$clearOrigin(EntityPlayer player, CallbackInfo ci) {
        LootOrigin.set(null);
    }
}
