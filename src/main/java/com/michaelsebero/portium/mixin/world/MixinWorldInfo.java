package com.michaelsebero.portium.mixin.world;

import com.michaelsebero.portium.api.IDataPackSettings;
import com.michaelsebero.portium.world.IWorldGeneration;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(WorldInfo.class)
public abstract class MixinWorldInfo implements IDataPackSettings, IWorldGeneration {

    @Unique
    private NBTTagCompound portium$dataPacks = new NBTTagCompound();
    @Unique
    private NBTTagCompound portium$customBossEvents = new NBTTagCompound();
    @Unique
    @Nullable
    private String portium$generation;

    @Inject(method = "<init>(Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("RETURN"))
    private void portium$readDataPacks(NBTTagCompound nbt, CallbackInfo ci) {
        this.portium$dataPacks = nbt.getCompoundTag("DataPacks");
        this.portium$customBossEvents = nbt.getCompoundTag("CustomBossEvents");
        // Worlds from before the choice: Portium and 1.13 save a data pack list, 1.12 doesn't.
        if (nbt.hasKey(WorldGeneration.TAG, 8)) this.portium$generation = WorldGeneration.resolve(nbt.getString(WorldGeneration.TAG));
        else this.portium$generation = nbt.hasKey("DataPacks", 10) ? WorldGeneration.MODERN : WorldGeneration.LEGACY;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/WorldSettings;Ljava/lang/String;)V", at = @At("RETURN"))
    private void portium$generationFromSettings(WorldSettings settings, String name, CallbackInfo ci) {
        this.portium$generation = WorldGeneration.resolve(((IWorldGeneration) (Object) settings).portium$getGeneration());
    }

    @Inject(method = "<init>(Lnet/minecraft/world/storage/WorldInfo;)V", at = @At("RETURN"))
    private void portium$copyDataPacks(WorldInfo other, CallbackInfo ci) {
        this.portium$dataPacks = IDataPackSettings.of(other).portium$getDataPacks().copy();
        this.portium$customBossEvents = IDataPackSettings.of(other).portium$getCustomBossEvents().copy();
        this.portium$generation = ((IWorldGeneration) other).portium$getGeneration();
    }

    @Inject(method = "updateTagCompound", at = @At("TAIL"))
    private void portium$writeDataPacks(NBTTagCompound nbt, NBTTagCompound playerNbt, CallbackInfo ci) {
        if (!this.portium$dataPacks.isEmpty()) nbt.setTag("DataPacks", this.portium$dataPacks.copy());
        if (!this.portium$customBossEvents.isEmpty()) nbt.setTag("CustomBossEvents", this.portium$customBossEvents.copy());
        nbt.setString(WorldGeneration.TAG, WorldGeneration.resolve(this.portium$generation));
    }

    @Nullable
    @Override
    public String portium$getGeneration() {
        return this.portium$generation;
    }

    @Override
    public void portium$setGeneration(@Nullable String generation) {
        this.portium$generation = generation;
    }

    @Override
    public NBTTagCompound portium$getDataPacks() {
        return this.portium$dataPacks;
    }

    @Override
    public void portium$setDataPacks(NBTTagCompound dataPacks) {
        this.portium$dataPacks = dataPacks;
    }

    @Override
    public NBTTagCompound portium$getCustomBossEvents() {
        return this.portium$customBossEvents;
    }

    @Override
    public void portium$setCustomBossEvents(NBTTagCompound bossEvents) {
        this.portium$customBossEvents = bossEvents;
    }
}
