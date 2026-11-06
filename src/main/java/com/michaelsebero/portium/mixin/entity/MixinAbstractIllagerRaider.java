package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.RaiderState;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.monster.EntityEvoker;
import net.minecraft.entity.monster.EntityIllusionIllager;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityVindicator;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** 1.12's illagers (and Portium's pillager under them) as 1.14 raiders: patrol/raid state, celebrating, aggression. */
@Mixin(AbstractIllager.class)
public abstract class MixinAbstractIllagerRaider extends EntityMob implements IRaider {

    @Shadow
    @Final
    protected static DataParameter<Byte> AGGRESSIVE;

    @Unique
    private static final DataParameter<Boolean> PORTIUM_CELEBRATING = EntityDataManager.createKey(AbstractIllager.class, DataSerializers.BOOLEAN);

    @Unique
    private final RaiderState portium$raider = new RaiderState();

    private MixinAbstractIllagerRaider(World world) {
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

    /** The bit 1.12's vindicator already uses for "attacking". */
    @Override
    public boolean portium$isAggressive() {
        return (this.dataManager.get(AGGRESSIVE) & 1) != 0;
    }

    @Override
    public void portium$setAggressive(boolean aggressive) {
        byte flags = this.dataManager.get(AGGRESSIVE);
        this.dataManager.set(AGGRESSIVE, (byte) (aggressive ? flags | 1 : flags & ~1));
    }

    @Nullable
    @Override
    public SoundEvent portium$getCelebrationSound() {
        Object self = this;
        if (self instanceof EntityVindicator) return PortiumSounds.ENTITY_VINDICATOR_CELEBRATE;
        if (self instanceof EntityEvoker) return PortiumSounds.ENTITY_EVOKER_CELEBRATE;
        if (self instanceof EntityIllusionIllager) return SoundEvents.ENTITY_ILLUSION_ILLAGER_AMBIENT;
        return null;
    }
}
