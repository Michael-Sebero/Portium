package com.michaelsebero.portium.entity.monster;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.init.PortiumEntities;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.world.WorldGeneration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.16's zombified piglin: 1.12's zombie pigman under its 1.16 name, look and sounds, with 1.16's anger: it goes for
 * the player who angered it (any nearby player with {@code universalAnger}), stays angry while chasing a player, calls
 * idle zombified piglins in range every 4 to 6 seconds while it can see its target, and (with
 * {@code forgiveDeadPlayers}) forgives a player who dies. In worlds made with 1.16.5's
 * generation every zombie pigman that appears (spawning, portals, lightning, piglins zombifying, old saves) becomes one
 * on the next tick; other worlds keep their pigmen.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public class EntityZombifiedPiglin extends EntityPigZombie {

    private static final List<EntityPigZombie> PENDING = new ArrayList<>();

    public EntityZombifiedPiglin(World world) {
        super(world);
    }

    private int ticksUntilNextAlert;

    /** 1.12 attacks whichever player is nearest while angry; 1.16 only the one it's angry at (unless universalAnger). */
    @Override
    protected void applyEntityAI() {
        super.applyEntityAI();
        this.targetTasks.taskEntries.removeIf(entry -> entry.action instanceof net.minecraft.entity.ai.EntityAINearestAttackableTarget
                && entry.action.getClass().getEnclosingClass() == EntityPigZombie.class);
        this.targetTasks.addTask(2, new net.minecraft.entity.ai.EntityAINearestAttackableTarget<>(this, net.minecraft.entity.player.EntityPlayer.class, 10,
                true, false, player -> player != null && this.isAngry() && (com.michaelsebero.portium.world.PortiumGameRules.get(this.world,
                com.michaelsebero.portium.world.PortiumGameRules.UNIVERSAL_ANGER, false) || player.getUniqueID().equals(this.anger().portium$getAngerTarget()))));
    }

    private com.michaelsebero.portium.mixin.accessor.EntityPigZombieAccessor anger() {
        return (com.michaelsebero.portium.mixin.accessor.EntityPigZombieAccessor) this;
    }

    @Override
    protected void updateAITasks() {
        net.minecraft.entity.EntityLivingBase target = this.getAttackTarget();
        // 1.16's anger only wears off while it isn't after a player (1.12 counts down regardless).
        if (target instanceof net.minecraft.entity.player.EntityPlayer && this.isAngry()) this.anger().portium$setAngerLevel(this.anger().portium$getAngerLevel() + 1);
        super.updateAITasks();
        target = this.getAttackTarget();
        if (target == null) return;
        if (this.ticksUntilNextAlert > 0) {
            --this.ticksUntilNextAlert;
            return;
        }
        if (this.getEntitySenses().canSee(target)) this.alertOthers(target);
        this.ticksUntilNextAlert = 80 + this.rand.nextInt(41);
    }

    /** 1.16's {@code alertOthers}: idle zombified piglins within follow range (10 up or down) take on its target. */
    private void alertOthers(net.minecraft.entity.EntityLivingBase target) {
        double range = this.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.FOLLOW_RANGE).getAttributeValue();
        net.minecraft.util.math.AxisAlignedBB box = new net.minecraft.util.math.AxisAlignedBB(this.posX, this.posY, this.posZ, this.posX, this.posY, this.posZ)
                .grow(range, 10.0D, range);
        for (EntityZombifiedPiglin other : this.world.getEntitiesWithinAABB(EntityZombifiedPiglin.class, box)) {
            if (other == this || other.getAttackTarget() != null || other.isOnSameTeam(target)) continue;
            other.angerAt(target);
        }
    }

    /** Angry at {@code target} for 20 to 40 seconds and after it, as 1.16's anger timer starts. */
    private void angerAt(net.minecraft.entity.EntityLivingBase target) {
        this.anger().portium$setAngerLevel(400 + this.rand.nextInt(400));
        this.anger().portium$setAngerTarget(target.getUniqueID());
        this.setRevengeTarget(target);
        this.setAttackTarget(target);
    }

    /** 1.16's {@code forgiveDeadPlayers}: zombified piglins within 32 blocks stop being angry at a player who dies. */
    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof net.minecraft.entity.player.EntityPlayer) || event.getEntityLiving().world.isRemote) return;
        net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) event.getEntityLiving();
        if (!com.michaelsebero.portium.world.PortiumGameRules.get(player.world, com.michaelsebero.portium.world.PortiumGameRules.FORGIVE_DEAD_PLAYERS, true)) return;
        for (EntityZombifiedPiglin piglin : player.world.getEntitiesWithinAABB(EntityZombifiedPiglin.class, player.getEntityBoundingBox().grow(32.0D, 10.0D, 32.0D))) {
            if (!player.getUniqueID().equals(piglin.anger().portium$getAngerTarget())) continue;
            piglin.anger().portium$setAngerLevel(0);
            piglin.anger().portium$setAngerTarget(null);
            piglin.setAttackTarget(null);
            piglin.setRevengeTarget(null);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isAngry() ? PortiumSounds.ENTITY_ZOMBIFIED_PIGLIN_ANGRY : PortiumSounds.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_ZOMBIFIED_PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_ZOMBIFIED_PIGLIN_DEATH;
    }

    /** 1.12 plays the pigman's angry sound directly; this one has its own. */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound == SoundEvents.ENTITY_ZOMBIE_PIG_ANGRY ? PortiumSounds.ENTITY_ZOMBIFIED_PIGLIN_ANGRY : sound, volume, pitch);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || entity.getClass() != EntityPigZombie.class || !PortiumConfig.mobs.zombifiedPiglins) return;
        if (!PortiumEntities.isRegistered(EntityZombifiedPiglin.class) || !WorldGeneration.is116(entity.world)) return;
        synchronized (PENDING) {
            PENDING.add((EntityPigZombie) entity);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        List<EntityPigZombie> pigmen;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            pigmen = new ArrayList<>(PENDING);
            PENDING.clear();
        }
        for (EntityPigZombie pigman : pigmen) {
            if (pigman.isDead || pigman.world.getEntityByID(pigman.getEntityId()) != pigman) continue;
            NBTTagCompound data = pigman.writeToNBT(new NBTTagCompound());
            Entity mount = pigman.getRidingEntity();
            List<Entity> riders = new ArrayList<>(pigman.getPassengers());
            pigman.dismountRidingEntity();
            pigman.removePassengers();
            pigman.setDead();
            EntityZombifiedPiglin piglin = new EntityZombifiedPiglin(pigman.world);
            data.removeTag("UUIDMost");
            data.removeTag("UUIDLeast");
            piglin.readFromNBT(data);
            pigman.world.spawnEntity(piglin);
            if (mount != null) piglin.startRiding(mount, true);
            for (Entity rider : riders) rider.startRiding(piglin, true);
        }
    }
}
