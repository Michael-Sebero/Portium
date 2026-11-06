package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.IBeeStingers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.15's {@code LivingEntity} bee stinger count: synced, and one falls out every 20 * (30 - count) ticks. */
@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBaseStingers implements IBeeStingers {

    @Unique
    private static final DataParameter<Integer> PORTIUM_STINGERS = EntityDataManager.createKey(EntityLivingBase.class, DataSerializers.VARINT);

    @Unique
    private int portium$stingerTimer;

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerStingers(CallbackInfo ci) {
        ((Entity) (Object) this).getDataManager().register(PORTIUM_STINGERS, 0);
    }

    @Override
    public int portium$getBeeStingers() {
        return ((Entity) (Object) this).getDataManager().get(PORTIUM_STINGERS);
    }

    @Override
    public void portium$setBeeStingers(int count) {
        ((Entity) (Object) this).getDataManager().set(PORTIUM_STINGERS, Math.max(0, count));
    }

    @Inject(method = "onLivingUpdate", at = @At("TAIL"))
    private void portium$dropStingers(CallbackInfo ci) {
        if (((Entity) (Object) this).world.isRemote) return;
        int count = this.portium$getBeeStingers();
        if (count <= 0) return;
        if (this.portium$stingerTimer <= 0) this.portium$stingerTimer = 20 * (30 - count);
        if (--this.portium$stingerTimer <= 0) this.portium$setBeeStingers(count - 1);
    }
}
