package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.entity.ICrossbowArrow;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import com.google.common.base.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Crossbow arrows (1.14): Piercing ones go through as many mobs as their level (plus one), sticking in none of them;
 * a crossbow's arrows make the crossbow hit sound; all of it ends when the arrow lands in a block.
 */
@Mixin(EntityArrow.class)
public abstract class MixinEntityArrowCrossbow extends Entity implements ICrossbowArrow {

    @Unique
    private static final DataParameter<Byte> PORTIUM_PIERCE_LEVEL = EntityDataManager.createKey(EntityArrow.class, DataSerializers.BYTE);

    @Unique
    private IntOpenHashSet portium$piercedEntities;
    @Unique
    private SoundEvent portium$hitSound = SoundEvents.ENTITY_ARROW_HIT;
    @Unique
    private boolean portium$shotFromCrossbow;

    protected MixinEntityArrowCrossbow(World world) {
        super(world);
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerPierce(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_PIERCE_LEVEL, (byte) 0);
    }

    @Override
    public void portium$setPierceLevel(byte level) {
        this.dataManager.set(PORTIUM_PIERCE_LEVEL, level);
    }

    @Override
    public byte portium$getPierceLevel() {
        return this.dataManager.get(PORTIUM_PIERCE_LEVEL);
    }

    @Override
    public void portium$setShotFromCrossbow(boolean shot) {
        this.portium$shotFromCrossbow = shot;
    }

    @Override
    public boolean portium$isShotFromCrossbow() {
        return this.portium$shotFromCrossbow;
    }

    @Override
    public void portium$setHitSound(SoundEvent sound) {
        this.portium$hitSound = sound;
    }

    /** Piercing: the mobs already pierced are passed by; one more than the level and the arrow is spent. */
    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void portium$pierce(RayTraceResult hit, CallbackInfo ci) {
        Entity entity = hit.entityHit;
        if (entity == null || this.portium$getPierceLevel() <= 0) return;
        if (this.portium$piercedEntities == null) this.portium$piercedEntities = new IntOpenHashSet(5);
        if (this.portium$piercedEntities.size() >= this.portium$getPierceLevel() + 1) {
            this.setDead();
            ci.cancel();
            return;
        }
        this.portium$piercedEntities.add(entity.getEntityId());
    }

    @Redirect(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;setArrowCountInEntity(I)V"))
    private void portium$noStuckArrowWhenPiercing(EntityLivingBase living, int count) {
        if (this.portium$getPierceLevel() <= 0) living.setArrowCountInEntity(count);
    }

    @Redirect(method = "onHit", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/entity/projectile/EntityArrow;setDead()V"))
    private void portium$keepFlyingWhenPiercing(EntityArrow arrow) {
        if (this.portium$getPierceLevel() <= 0) arrow.setDead();
    }

    @Redirect(method = "onHit", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/entity/projectile/EntityArrow;playSound(Lnet/minecraft/util/SoundEvent;FF)V"))
    private void portium$entityHitSound(EntityArrow arrow, SoundEvent sound, float volume, float pitch) {
        arrow.playSound(this.portium$hitSound, volume, pitch);
    }

    /** Landing in a block: its hit sound, and it's a plain arrow again. */
    @Redirect(method = "onHit", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/entity/projectile/EntityArrow;playSound(Lnet/minecraft/util/SoundEvent;FF)V"))
    private void portium$blockHitSound(EntityArrow arrow, SoundEvent sound, float volume, float pitch) {
        arrow.playSound(this.portium$hitSound, volume, pitch);
        this.portium$setPierceLevel((byte) 0);
        this.portium$hitSound = SoundEvents.ENTITY_ARROW_HIT;
        this.portium$shotFromCrossbow = false;
        if (this.portium$piercedEntities != null) this.portium$piercedEntities.clear();
    }

    @Redirect(method = "findEntityOnPath", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getEntitiesInAABBexcluding(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Lcom/google/common/base/Predicate;)Ljava/util/List;"))
    private List<Entity> portium$skipPierced(World world, Entity self, AxisAlignedBB box, Predicate<? super Entity> predicate) {
        List<Entity> entities = world.getEntitiesInAABBexcluding(self, box, predicate);
        if (this.portium$piercedEntities != null && !this.portium$piercedEntities.isEmpty()) {
            entities.removeIf(entity -> this.portium$piercedEntities.contains(entity.getEntityId()));
        }
        return entities;
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void portium$writeCrossbow(NBTTagCompound compound, CallbackInfo ci) {
        compound.setByte("PierceLevel", this.portium$getPierceLevel());
        compound.setString("SoundEvent", String.valueOf(SoundEvent.REGISTRY.getNameForObject(this.portium$hitSound)));
        compound.setBoolean("ShotFromCrossbow", this.portium$shotFromCrossbow);
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void portium$readCrossbow(NBTTagCompound compound, CallbackInfo ci) {
        this.portium$setPierceLevel(compound.getByte("PierceLevel"));
        if (compound.hasKey("SoundEvent", 8)) {
            SoundEvent sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(compound.getString("SoundEvent")));
            this.portium$hitSound = sound != null ? sound : SoundEvents.ENTITY_ARROW_HIT;
        }
        this.portium$shotFromCrossbow = compound.getBoolean("ShotFromCrossbow");
    }
}
