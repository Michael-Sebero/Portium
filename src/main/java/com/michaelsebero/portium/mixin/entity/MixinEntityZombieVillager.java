package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.villager.IVillagerDataHolder;
import com.michaelsebero.portium.entity.villager.VillagerData116;
import com.michaelsebero.portium.entity.villager.VillagerEvents116;
import com.michaelsebero.portium.entity.villager.VillagerProfession116;
import com.michaelsebero.portium.entity.villager.VillagerType116;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Zombie villagers in 1.16.5 worlds: they carry the 1.14 villager data they show (type, profession, level; empty in
 * other worlds, which keep 1.12's look), and a cured one comes back as a 1.14 villager, as the villager it was.
 */
@Mixin(EntityZombieVillager.class)
public abstract class MixinEntityZombieVillager implements IVillagerDataHolder {

    @Unique
    private static final DataParameter<NBTTagCompound> PORTIUM_VILLAGER_DATA = EntityDataManager.createKey(EntityZombieVillager.class,
            DataSerializers.COMPOUND_TAG);

    @Shadow
    private UUID converstionStarter;

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerVillagerData(CallbackInfo ci) {
        ((Entity) (Object) this).getDataManager().register(PORTIUM_VILLAGER_DATA, new NBTTagCompound());
    }

    @Inject(method = "finishConversion", at = @At("HEAD"), cancellable = true)
    private void portium$cure116(CallbackInfo ci) {
        if (VillagerEvents116.cure((EntityZombieVillager) (Object) this, this.converstionStarter)) ci.cancel();
    }

    @Override
    public VillagerData116 getVillagerData() {
        NBTTagCompound tag = ((Entity) (Object) this).getDataManager().get(PORTIUM_VILLAGER_DATA);
        return tag.isEmpty() ? new VillagerData116(VillagerType116.PLAINS, VillagerProfession116.NONE, 1) : VillagerData116.read(tag);
    }

    @Override
    public void setVillagerData(VillagerData116 data) {
        ((Entity) (Object) this).getDataManager().set(PORTIUM_VILLAGER_DATA, data.write());
    }

    @Override
    public boolean hasVillagerData() {
        return !((Entity) (Object) this).getDataManager().get(PORTIUM_VILLAGER_DATA).isEmpty();
    }
}
