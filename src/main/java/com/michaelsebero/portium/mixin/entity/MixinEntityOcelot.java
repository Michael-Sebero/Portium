package com.michaelsebero.portium.mixin.entity;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.entity.ai.EntityAIBreed116;
import com.michaelsebero.portium.entity.passive.EntityTurtle;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIOcelotAttack;
import net.minecraft.entity.ai.EntityAISit;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;

/**
 * The ocelot as 1.14 made it once cats were split off (with mobs.cats on): wild only, never tamed. Fed raw cod or
 * salmon while it creeps up, it may come to trust the player (no more running away); trusting ocelots can be bred.
 * Its own sounds, and 1.14's movement-speed multipliers for creeping (0.6) and dashing off (1.33).
 */
@Mixin(EntityOcelot.class)
public abstract class MixinEntityOcelot extends EntityTameable {

    @Unique
    private static final DataParameter<Boolean> PORTIUM_TRUSTING = EntityDataManager.createKey(EntityOcelot.class, DataSerializers.BOOLEAN);

    @Shadow
    private EntityAITempt aiTempt;

    @Unique
    private EntityAIAvoidEntity<EntityPlayer> portium$avoidPlayers;

    protected MixinEntityOcelot(World world) {
        super(world);
    }

    @Unique
    private static boolean portium$split() {
        return PortiumConfig.mobs.cats;
    }

    @Unique
    private static boolean portium$isFish(ItemStack stack) {
        return stack.getItem() == Items.FISH && (stack.getMetadata() == 0 || stack.getMetadata() == 1);
    }

    @Unique
    private boolean portium$isTrusting() {
        return this.dataManager.get(PORTIUM_TRUSTING);
    }

    @Unique
    private void portium$setTrusting(boolean trusting) {
        this.dataManager.set(PORTIUM_TRUSTING, trusting);
        this.setupTamedAI();
    }

    @Inject(method = "entityInit", at = @At("TAIL"))
    private void portium$registerTrusting(CallbackInfo ci) {
        this.dataManager.register(PORTIUM_TRUSTING, false);
    }

    @Inject(method = "initEntityAI", at = @At("HEAD"), cancellable = true)
    private void portium$ai116(CallbackInfo ci) {
        if (!portium$split()) return;
        ci.cancel();
        this.aiSit = new EntityAISit(this);
        this.aiTempt = new EntityAITempt(this, 0.6D, true, new HashSet<>(Collections.singletonList(Items.FISH))) {
            @Override
            protected boolean isTempting(ItemStack stack) {
                return portium$isFish(stack);
            }
        };
        this.tasks.addTask(1, new EntityAISwimming(this));
        this.tasks.addTask(3, this.aiTempt);
        this.tasks.addTask(7, new EntityAILeapAtTarget(this, 0.3F));
        this.tasks.addTask(8, new EntityAIOcelotAttack(this));
        this.tasks.addTask(9, new EntityAIBreed116(this, 0.8D));
        this.tasks.addTask(10, new EntityAIWanderAvoidWater(this, 0.8D, 1.0000001E-5F));
        this.tasks.addTask(11, new EntityAIWatchClosest(this, EntityPlayer.class, 10.0F));
        this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityChicken.class, false));
        this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityTurtle.class, 10, false, false, EntityTurtle.TARGET_DRY_BABY::test));
    }

    /** Untrusting ocelots run from players, whatever 1.12 thought of taming. */
    @Inject(method = "setupTamedAI", at = @At("HEAD"), cancellable = true)
    private void portium$avoidUnlessTrusting(CallbackInfo ci) {
        if (!portium$split()) return;
        ci.cancel();
        if (this.portium$avoidPlayers == null) {
            this.portium$avoidPlayers = new EntityAIAvoidEntity<EntityPlayer>(this, EntityPlayer.class, EntitySelectors.CAN_AI_TARGET::apply, 16.0F, 0.8D, 1.33D) {
                @Override
                public boolean shouldExecute() {
                    return !portium$isTrusting() && super.shouldExecute();
                }

                @Override
                public boolean shouldContinueExecuting() {
                    return !portium$isTrusting() && super.shouldContinueExecuting();
                }
            };
        }
        this.tasks.removeTask(this.portium$avoidPlayers);
        if (!this.portium$isTrusting()) this.tasks.addTask(4, this.portium$avoidPlayers);
    }

    @Inject(method = "updateAITasks", at = @At("HEAD"), cancellable = true)
    private void portium$speeds116(CallbackInfo ci) {
        if (!portium$split()) return;
        ci.cancel();
        if (this.getMoveHelper().isUpdating()) {
            double speed = this.getMoveHelper().getSpeed();
            this.setSneaking(speed == 0.6D);
            this.setSprinting(speed == 1.33D);
        } else {
            this.setSneaking(false);
            this.setSprinting(false);
        }
    }

    @Inject(method = "canDespawn", at = @At("HEAD"), cancellable = true)
    private void portium$despawn(CallbackInfoReturnable<Boolean> cir) {
        if (portium$split()) cir.setReturnValue(!this.portium$isTrusting() && this.ticksExisted > 2400);
    }

    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    private void portium$writeTrusting(NBTTagCompound compound, CallbackInfo ci) {
        compound.setBoolean("Trusting", this.portium$isTrusting());
    }

    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    private void portium$readTrusting(NBTTagCompound compound, CallbackInfo ci) {
        this.portium$setTrusting(compound.getBoolean("Trusting"));
    }

    /** Raw cod or salmon, while it creeps close: a one in three chance it trusts the player from then on. */
    @Inject(method = "processInteract", at = @At("HEAD"), cancellable = true)
    private void portium$trust(EntityPlayer player, EnumHand hand, CallbackInfoReturnable<Boolean> cir) {
        if (!portium$split()) return;
        ItemStack stack = player.getHeldItem(hand);
        if ((this.aiTempt == null || this.aiTempt.isRunning()) && !this.portium$isTrusting() && portium$isFish(stack) && player.getDistanceSq(this) < 9.0D) {
            this.consumeItemFromStack(player, stack);
            if (!this.world.isRemote) {
                if (this.rand.nextInt(3) == 0) {
                    this.portium$setTrusting(true);
                    this.portium$trustParticles(true);
                    this.world.setEntityState(this, (byte) 41);
                } else {
                    this.portium$trustParticles(false);
                    this.world.setEntityState(this, (byte) 40);
                }
            }
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(super.processInteract(player, hand));
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 41 && portium$split()) this.portium$trustParticles(true);
        else if (id == 40 && portium$split()) this.portium$trustParticles(false);
        else super.handleStatusUpdate(id);
    }

    @Unique
    private void portium$trustParticles(boolean trusted) {
        EnumParticleTypes particle = trusted ? EnumParticleTypes.HEART : EnumParticleTypes.SMOKE_NORMAL;
        for (int i = 0; i < 7; ++i) {
            double dx = this.rand.nextGaussian() * 0.02D;
            double dy = this.rand.nextGaussian() * 0.02D;
            double dz = this.rand.nextGaussian() * 0.02D;
            this.world.spawnParticle(particle, this.posX + (this.rand.nextDouble() * 2.0D - 1.0D) * this.width, this.posY + this.rand.nextDouble() * this.height + 0.5D,
                    this.posZ + (this.rand.nextDouble() * 2.0D - 1.0D) * this.width, dx, dy, dz);
        }
    }

    @Inject(method = "isBreedingItem", at = @At("HEAD"), cancellable = true)
    private void portium$breedingFish(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (portium$split()) cir.setReturnValue(portium$isFish(stack));
    }

    @Inject(method = "canMateWith", at = @At("HEAD"), cancellable = true)
    private void portium$mateUntamed(EntityAnimal other, CallbackInfoReturnable<Boolean> cir) {
        if (portium$split()) cir.setReturnValue(other != this && other instanceof EntityOcelot && this.isInLove() && other.isInLove());
    }

    /** No kittens alongside a wild ocelot spawning (1.12 added two, one time in seven). */
    @Inject(method = "onInitialSpawn", at = @At("HEAD"), cancellable = true)
    private void portium$noExtraKittens(DifficultyInstance difficulty, @Nullable IEntityLivingData data, CallbackInfoReturnable<IEntityLivingData> cir) {
        if (portium$split()) cir.setReturnValue(super.onInitialSpawn(difficulty, data));
    }

    @Inject(method = "getAmbientSound", at = @At("HEAD"), cancellable = true)
    private void portium$ambient(CallbackInfoReturnable<SoundEvent> cir) {
        if (portium$split()) cir.setReturnValue(PortiumSounds.ENTITY_OCELOT_AMBIENT);
    }

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void portium$hurt(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        if (portium$split()) cir.setReturnValue(PortiumSounds.ENTITY_OCELOT_HURT);
    }

    @Inject(method = "getDeathSound", at = @At("HEAD"), cancellable = true)
    private void portium$death(CallbackInfoReturnable<SoundEvent> cir) {
        if (portium$split()) cir.setReturnValue(PortiumSounds.ENTITY_OCELOT_DEATH);
    }

    @Override
    public int getTalkInterval() {
        return portium$split() ? 900 : super.getTalkInterval();
    }
}
