package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.RaiderState;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Witches as 1.14 raiders. */
@Mixin(EntityWitch.class)
public abstract class MixinEntityWitchRaider extends EntityMob implements IRaider {

    @Unique
    private static final DataParameter<Boolean> PORTIUM_CELEBRATING = EntityDataManager.createKey(EntityWitch.class, DataSerializers.BOOLEAN);

    @Unique
    private final RaiderState portium$raider = new RaiderState();
    @Unique
    private boolean portium$aggressive;

    private MixinEntityWitchRaider(World world) {
        super(world);
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerCelebrating(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_CELEBRATING, false);
    }

    @Override
    public RaiderState portium$raider() {
        return this.portium$raider;
    }

    @Override
    public boolean portium$isCelebrating() {
        return this.dataManager.get(PORTIUM_CELEBRATING);
    }

    @Override
    public void portium$setCelebrating(boolean celebrating) {
        this.dataManager.set(PORTIUM_CELEBRATING, celebrating);
    }

    @Override
    public boolean portium$isAggressive() {
        return this.portium$aggressive;
    }

    @Override
    public void portium$setAggressive(boolean aggressive) {
        this.portium$aggressive = aggressive;
    }

    @Override
    public SoundEvent portium$getCelebrationSound() {
        return PortiumSounds.ENTITY_WITCH_CELEBRATE;
    }
}
