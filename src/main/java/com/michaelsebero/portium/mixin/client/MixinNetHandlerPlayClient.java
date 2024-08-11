package com.michaelsebero.portium.mixin.client;

import com.michaelsebero.portium.entity.ItemFrames;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reads the facing of item frames on floors and ceilings from their spawn packet. */
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {

    @Redirect(method = "handleSpawnObject", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/EnumFacing;byHorizontalIndex(I)Lnet/minecraft/util/EnumFacing;"))
    private EnumFacing portium$frameFacing(int data) {
        return ItemFrames.fromSpawnData(data);
    }
}
