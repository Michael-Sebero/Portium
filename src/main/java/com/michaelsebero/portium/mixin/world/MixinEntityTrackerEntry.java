package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.entity.ItemFrames;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Item frames facing up or down reach clients with a facing 1.12's wall index can't express. */
@Mixin(EntityTrackerEntry.class)
public abstract class MixinEntityTrackerEntry {

    @Redirect(method = "createSpawnPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/EnumFacing;getHorizontalIndex()I"))
    private int portium$frameSpawnData(EnumFacing facing) {
        return ItemFrames.toSpawnData(facing);
    }
}
