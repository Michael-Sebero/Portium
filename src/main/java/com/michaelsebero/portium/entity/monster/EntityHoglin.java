package com.michaelsebero.portium.entity.monster;

import com.google.common.collect.ImmutableList;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.Conversions116;
import com.michaelsebero.portium.entity.IShieldBlockReaction;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.IBrainHolder;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.monster.piglin.EntityAbstractPiglin;
import com.michaelsebero.portium.init.PortiumBlocks;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.16's hoglin: a hostile Nether beast that charges players and flings them, breeds on crimson fungus, keeps away from
 * warped fungus, portals and respawn anchors, flees piglins that outnumber it, and turns into a zoglin outside the
 * Nether. Run by its brain ({@link HoglinTasks}).
 */
public class EntityHoglin extends EntityAnimal implements IMob, IFlinging, IBrainHolder, IShieldBlockReaction {

    public static final ResourceLocation LOOT_TABLE = com.michaelsebero.portium.init.PortiumLootTables.HOGLIN;
    private static final DataParameter<Boolean> IMMUNE_TO_ZOMBIFICATION = EntityDataManager.createKey(EntityHoglin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> AGGRESSIVE = EntityDataManager.createKey(EntityHoglin.class, DataSerializers.BOOLEAN);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final List SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ADULT,
            HoglinMobsSensor.TYPE);
    private static final ImmutableList<MemoryModuleType<?>> MEMORIES = ImmutableList.of(MemoryModuleType.BREED_TARGET, MemoryModuleType.MOBS,
            MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
            MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH,
            MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN,
            MemoryModuleType.AVOID_TARGET, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
            MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.PACIFIED);

    private int attackAnimationTick;
    private int timeInOverworld;
    private boolean cannotBeHunted;
    private final Brain<EntityHoglin> brain;

    @SuppressWarnings("unchecked")
    public EntityHoglin(World world) {
        super(world);
        this.setSize(1.3964844F, 1.4F);
        this.experienceValue = 5;
        this.brain = HoglinTasks.makeBrain(new Brain<EntityHoglin>(MEMORIES, SENSORS));
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(IMMUNE_TO_ZOMBIFICATION, false);
        this.dataManager.register(AGGRESSIVE, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6D);
    }

    @Override
    public Brain<EntityHoglin> getBrain() {
        return this.brain;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return !this.getLeashed();
    }

    @Override
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        if (forSpawnCount && this.isNoDespawnRequired()) return false;
        return type == EnumCreatureType.MONSTER;
    }

    // ---- Fighting ----

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (!(target instanceof EntityLivingBase)) return false;
        this.attackAnimationTick = 10;
        this.world.setEntityState(this, (byte) 4);
        this.playSound(PortiumSounds.ENTITY_HOGLIN_ATTACK, 1.0F, this.getSoundPitch());
        HoglinTasks.onHitTarget(this, (EntityLivingBase) target);
        return IFlinging.hurtAndThrowTarget(this, (EntityLivingBase) target);
    }

    @Override
    public void onBlockedByShield(EntityLivingBase blocker) {
        if (this.isAdult()) IFlinging.throwTarget(this, blocker);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (this.world.isRemote) return false;
        if (hurt && source.getTrueSource() instanceof EntityLivingBase) HoglinTasks.wasHurtBy(this, (EntityLivingBase) source.getTrueSource());
        return hurt;
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 4) {
            this.attackAnimationTick = 10;
            this.playSound(PortiumSounds.ENTITY_HOGLIN_ATTACK, 1.0F, this.getSoundPitch());
        } else {
            super.handleStatusUpdate(id);
        }
    }

    @Override
    public int getAttackAnimationTick() {
        return this.attackAnimationTick;
    }

    public void setAggressive(boolean aggressive) {
        this.dataManager.set(AGGRESSIVE, aggressive);
    }

    public boolean isAggressive() {
        return this.dataManager.get(AGGRESSIVE);
    }

    // ---- Brain, zombification ----

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("hoglinBrain");
        this.brain.tick((WorldServer) this.world, this);
        this.world.profiler.endSection();
        HoglinTasks.updateActivity(this);
        if (this.isConverting()) {
            ++this.timeInOverworld;
            if (this.timeInOverworld > 300) {
                this.playActivitySound(PortiumSounds.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED);
                EntityZoglin zoglin = Conversions116.convertTo(this, EntityZoglin::new, true);
                if (zoglin != null) zoglin.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
            }
        } else {
            this.timeInOverworld = 0;
        }
        super.updateAITasks();
    }

    @Override
    public void onLivingUpdate() {
        if (this.attackAnimationTick > 0) --this.attackAnimationTick;
        super.onLivingUpdate();
    }

    public boolean isConverting() {
        return !EntityAbstractPiglin.isPiglinSafe(this.world) && !this.dataManager.get(IMMUNE_TO_ZOMBIFICATION) && !this.isAIDisabled();
    }

    public void setImmuneToZombification(boolean immune) {
        this.dataManager.set(IMMUNE_TO_ZOMBIFICATION, immune);
    }

    public boolean isAdult() {
        return !this.isChild();
    }

    /** An adult that hasn't been marked as never to be hunted (piglins hunt these). */
    public boolean canBeHunted() {
        return this.isAdult() && !this.cannotBeHunted;
    }

    @Override
    protected void onGrowingAdult() {
        super.onGrowingAdult();
        this.applyAgeStats();
    }

    private void applyAgeStats() {
        if (this.isChild()) {
            this.experienceValue = 3;
            this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(0.5D);
        } else {
            this.experienceValue = 5;
            this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
        }
    }

    /** 1.16 applies the baby or adult stats whenever the age crosses zero, either way. */
    @Override
    public void setGrowingAge(int age) {
        boolean wasChild = this.isChild();
        super.setGrowingAge(age);
        if (!this.world.isRemote && wasChild != this.isChild()) this.applyAgeStats();
    }

    // ---- Spawning and breeding ----

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        if (this.world.rand.nextFloat() < 0.2F) this.setGrowingAge(-24000);
        return super.onInitialSpawn(difficulty, data);
    }

    /** 1.16's spawn rule: anywhere but on a nether wart block, with room. */
    @Override
    public boolean getCanSpawnHere() {
        Block below = this.world.getBlockState(new BlockPos(this).down()).getBlock();
        return below != Blocks.NETHER_WART_BLOCK && this.world.checkNoEntityCollision(this.getEntityBoundingBox())
                && this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty() && !this.world.containsAnyLiquid(this.getEntityBoundingBox());
    }

    @Override
    protected boolean canDespawn() {
        return true;
    }

    /** Keeps away from repellents; likes crimson nylium. */
    @Override
    public float getBlockPathWeight(BlockPos pos) {
        if (HoglinTasks.isPosNearNearestRepellent(this, pos)) return -1.0F;
        return PortiumBlocks.CRIMSON_NYLIUM != null && this.world.getBlockState(pos.down()).getBlock() == PortiumBlocks.CRIMSON_NYLIUM ? 10.0F : 0.0F;
    }

    @Override
    public double getMountedYOffset() {
        return this.height - (this.isChild() ? 0.2D : 0.15D);
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return PortiumBlocks.CRIMSON_FUNGUS != null && stack.getItem() == Item.getItemFromBlock(PortiumBlocks.CRIMSON_FUNGUS);
    }

    /** Feeding one makes it persistent; a calmed hoglin won't fall in love. */
    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (this.isBreedingItem(stack) && this.getGrowingAge() == 0 && HoglinTasks.isPacified(this)) return false;
        boolean result = super.processInteract(player, hand);
        if (result) this.enablePersistence();
        return result;
    }

    @Nullable
    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        EntityHoglin child = new EntityHoglin(this.world);
        child.enablePersistence();
        return child;
    }

    /** 1.16's {@code AnimalEntity.spawnBabyAnimal}, from the brain's breed task (1.12's EntityAIMate.spawnBaby). */
    void spawnBaby(EntityHoglin mate) {
        EntityAgeable child = this.createChild(mate);
        net.minecraftforge.event.entity.living.BabyEntitySpawnEvent event = new net.minecraftforge.event.entity.living.BabyEntitySpawnEvent(this, mate, child);
        boolean cancelled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
        child = event.getChild();
        if (cancelled) {
            this.setGrowingAge(6000);
            mate.setGrowingAge(6000);
            this.resetInLove();
            mate.resetInLove();
            return;
        }
        if (child == null) return;
        EntityPlayerMP player = this.getLoveCause();
        if (player == null && mate.getLoveCause() != null) player = mate.getLoveCause();
        if (player != null) {
            player.addStat(StatList.ANIMALS_BRED);
            net.minecraft.advancements.CriteriaTriggers.BRED_ANIMALS.trigger(player, this, mate, child);
        }
        this.setGrowingAge(6000);
        mate.setGrowingAge(6000);
        this.resetInLove();
        mate.resetInLove();
        child.setGrowingAge(-24000);
        child.setLocationAndAngles(this.posX, this.posY, this.posZ, 0.0F, 0.0F);
        this.world.spawnEntity(child);
        this.world.setEntityState(this, (byte) 18);
        if (this.world.getGameRules().getBoolean("doMobLoot")) {
            this.world.spawnEntity(new EntityXPOrb(this.world, this.posX, this.posY, this.posZ, this.rand.nextInt(7) + 1));
        }
    }

    // ---- Saving ----

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (this.dataManager.get(IMMUNE_TO_ZOMBIFICATION)) compound.setBoolean("IsImmuneToZombification", true);
        compound.setInteger("TimeInOverworld", this.timeInOverworld);
        if (this.cannotBeHunted) compound.setBoolean("CannotBeHunted", true);
        compound.setTag("Brain", this.brain.write());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setImmuneToZombification(compound.getBoolean("IsImmuneToZombification"));
        this.timeInOverworld = compound.getInteger("TimeInOverworld");
        this.cannotBeHunted = compound.getBoolean("CannotBeHunted");
        if (compound.hasKey("Brain", 10)) this.brain.read(compound.getCompoundTag("Brain"));
        this.applyAgeStats();
    }

    public void setCannotBeHunted(boolean cannotBeHunted) {
        this.cannotBeHunted = cannotBeHunted;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return LOOT_TABLE;
    }

    @Override
    protected boolean canDropLoot() {
        return true;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return this.experienceValue;
    }

    // ---- Sounds ----

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.world.isRemote ? null : HoglinTasks.getSoundForCurrentActivity(this).orElse(null);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_HOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_HOGLIN_DEATH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_HOSTILE_SWIM;
    }

    @Override
    protected SoundEvent getSplashSound() {
        return SoundEvents.ENTITY_HOSTILE_SPLASH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(PortiumSounds.ENTITY_HOGLIN_STEP, 0.15F, 1.0F);
    }

    void playActivitySound(SoundEvent sound) {
        this.playSound(sound, this.getSoundVolume(), this.getSoundPitch());
    }
}
