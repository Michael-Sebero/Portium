package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.raid.IRaider;
import com.michaelsebero.portium.entity.raid.RaiderSpawns;
import com.michaelsebero.portium.entity.raid.RaiderState;
import com.michaelsebero.portium.entity.raid.Raiders116;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** What 1.14's {@code PatrollerEntity} and {@code AbstractRaiderEntity} add to every raider: saving, spawning, despawning. */
@Mixin(EntityLiving.class)
public abstract class MixinEntityLivingRaider extends EntityLivingBase {

    private MixinEntityLivingRaider(World world) {
        super(world);
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void portium$writeRaider(NBTTagCompound tag, CallbackInfo ci) {
        if ((Object) this instanceof IRaider) ((IRaider) this).portium$raider().write(tag);
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void portium$readRaider(NBTTagCompound tag, CallbackInfo ci) {
        if ((Object) this instanceof IRaider) ((IRaider) this).portium$raider().read(tag);
    }

    @Inject(method = "onInitialSpawn", at = @At("HEAD"))
    private void portium$raiderSpawn(DifficultyInstance difficulty, IEntityLivingData data, CallbackInfoReturnable<IEntityLivingData> cir) {
        if ((Object) this instanceof IRaider) RaiderSpawns.onInitialSpawn((EntityLiving) (Object) this);
    }

    /**
     * Raiders in a raid never despawn; patrollers only once no player is within 128 blocks (1.14's
     * {@code canDespawn(distance)}), where 1.12 would also let them wander off at random.
     */
    @Inject(method = "despawnEntity", at = @At("HEAD"), cancellable = true)
    private void portium$raiderDespawn(CallbackInfo ci) {
        if (!((Object) this instanceof IRaider) || ((EntityLiving) (Object) this).isNoDespawnRequired()) return;
        EntityLiving self = (EntityLiving) (Object) this;
        RaiderState state = ((IRaider) this).portium$raider();
        if (Raiders116.hasRaid(self)) {
            this.idleTime = 0;
            ci.cancel();
        } else if (state.isPatrolling()) {
            EntityPlayer player = this.world.getClosestPlayerToEntity(this, -1.0D);
            if (player == null) return;
            double distanceSq = this.getDistanceSq(player);
            if (distanceSq > 16384.0D) this.setDead();
            else if (distanceSq < 1024.0D) this.idleTime = 0;
            ci.cancel();
        }
    }
}
