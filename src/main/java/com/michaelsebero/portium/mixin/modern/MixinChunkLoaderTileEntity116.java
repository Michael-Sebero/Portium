package com.michaelsebero.portium.mixin.modern;

import com.michaelsebero.portium.mods116.adapter.TileEntity116;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.16 loads a tile entity with its block's state; 1.12 reads tile entities before their chunk is in the world, so
 * the chunk being read is noted for {@link TileEntity116} to take the state from. Forge's method, unobfuscated.
 */
@Mixin(AnvilChunkLoader.class)
public abstract class MixinChunkLoaderTileEntity116 {

    @Inject(method = "loadEntities", at = @At("HEAD"), remap = false)
    private void portium$startLoading(World world, NBTTagCompound compound, Chunk chunk, CallbackInfo ci) {
        TileEntity116.loading(chunk);
    }

    @Inject(method = "loadEntities", at = @At("RETURN"), remap = false)
    private void portium$endLoading(World world, NBTTagCompound compound, Chunk chunk, CallbackInfo ci) {
        TileEntity116.loading(null);
    }
}
