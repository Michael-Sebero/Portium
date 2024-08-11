package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.api.IDrowningZombie;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityZombie.class)
public abstract class MixinEntityZombie extends EntityMob implements IDrowningZombie {

    @Unique
    private static final DataParameter<Boolean> PORTIUM_DROWNING = EntityDataManager.createKey(EntityZombie.class, DataSerializers.BOOLEAN);

    private MixinEntityZombie(World world) {
        super(world);
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerDrowning(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_DROWNING, false);
    }

    @Override
    public boolean portium$isDrowning() {
        return this.dataManager.get(PORTIUM_DROWNING);
    }

    @Override
    public void portium$setDrowning(boolean drowning) {
        this.dataManager.set(PORTIUM_DROWNING, drowning);
    }
}
