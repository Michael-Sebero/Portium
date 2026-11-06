package com.michaelsebero.portium.entity.monster;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.Conversions116;
import com.michaelsebero.portium.entity.IShieldBlockReaction;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.IBrainHolder;
import com.michaelsebero.portium.entity.ai.brain.RangedInteger;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.schedule.Activity;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.ai.brain.task.AttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.DummyTask;
import com.michaelsebero.portium.entity.ai.brain.task.FindNewAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.FirstShuffledTask;
import com.michaelsebero.portium.entity.ai.brain.task.ForgetAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookAtEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookTask;
import com.michaelsebero.portium.entity.ai.brain.task.MoveToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.RunSometimesTask;
import com.michaelsebero.portium.entity.ai.brain.task.SupplementedTask;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import com.michaelsebero.portium.entity.ai.brain.task.WalkRandomlyTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkTowardsLookTargetTask;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/** 1.16's zoglin: a zombified hoglin that attacks anything living but creepers and other zoglins. */
public class EntityZoglin extends EntityMob implements IFlinging, IBrainHolder, IShieldBlockReaction, Conversions116.IChildSettable {

    public static final ResourceLocation LOOT_TABLE = com.michaelsebero.portium.init.PortiumLootTables.ZOGLIN;
    private static final DataParameter<Boolean> BABY = EntityDataManager.createKey(EntityZoglin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> AGGRESSIVE = EntityDataManager.createKey(EntityZoglin.class, DataSerializers.BOOLEAN);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final List SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS);
    private static final ImmutableList<MemoryModuleType<?>> MEMORIES = ImmutableList.of(MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER, MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET,
            MemoryModuleType.ATTACK_COOLING_DOWN);

    private int attackAnimationTick;
    private final Brain<EntityZoglin> brain;

    @SuppressWarnings("unchecked")
    public EntityZoglin(World world) {
        super(world);
        this.setSize(1.3964844F, 1.4F);
        this.experienceValue = 5;
        this.brain = makeBrain(new Brain<EntityZoglin>(MEMORIES, SENSORS));
    }

    private static Brain<EntityZoglin> makeBrain(Brain<EntityZoglin> brain) {
        brain.registerActivity(Activity.CORE, 0, ImmutableList.<Task<? super EntityZoglin>>of(new LookTask(45, 90), new WalkToTargetTask()));
        brain.registerActivity(Activity.IDLE, 10, ImmutableList.<Task<? super EntityZoglin>>of(
                new ForgetAttackTargetTask<EntityZoglin>(EntityZoglin::findNearestValidAttackTarget),
                new RunSometimesTask<>(new LookAtEntityTask(8.0F), RangedInteger.of(30, 60)),
                new FirstShuffledTask<EntityZoglin>(ImmutableList.of(Pair.of(new WalkRandomlyTask(0.4F), 2),
                        Pair.of(new WalkTowardsLookTargetTask(0.4F, 3), 2), Pair.of(new DummyTask(30, 60), 1)))));
        brain.registerActivity(Activity.FIGHT, 10, ImmutableList.<Task<? super EntityZoglin>>of(new MoveToTargetTask(1.0F),
                new SupplementedTask<EntityZoglin>(EntityZoglin::isAdult, new AttackTargetTask(40)),
                new SupplementedTask<EntityZoglin>(EntityZoglin::isChild, new AttackTargetTask(15)), new FindNewAttackTargetTask<>()),
                MemoryModuleType.ATTACK_TARGET);
        brain.setPersistentActivities(ImmutableSet.of(Activity.CORE));
        brain.setFallbackActivity(Activity.IDLE);
        brain.switchToFallbackActivity();
        return brain;
    }

    private Optional<? extends EntityLivingBase> findNearestValidAttackTarget() {
        return this.brain.getMemory(MemoryModuleType.VISIBLE_MOBS).orElse(ImmutableList.of()).stream().filter(EntityZoglin::isTargetable).findFirst();
    }

    private static boolean isTargetable(EntityLivingBase entity) {
        return !(entity instanceof EntityZoglin) && !(entity instanceof EntityCreeper) && TargetPredicate.canHostileTarget(entity);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(BABY, false);
        this.dataManager.register(AGGRESSIVE, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(40.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
        this.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(0.6D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }

    @Override
    public Brain<EntityZoglin> getBrain() {
        return this.brain;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return !this.getLeashed();
    }

    public boolean isAdult() {
        return !this.isChild();
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (!(target instanceof EntityLivingBase)) return false;
        this.attackAnimationTick = 10;
        this.world.setEntityState(this, (byte) 4);
        this.playSound(PortiumSounds.ENTITY_ZOGLIN_ATTACK, 1.0F, this.getSoundPitch());
        return IFlinging.hurtAndThrowTarget(this, (EntityLivingBase) target);
    }

    @Override
    public void onBlockedByShield(EntityLivingBase blocker) {
        if (!this.isChild()) IFlinging.throwTarget(this, blocker);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (this.world.isRemote) return false;
        if (hurt && source.getTrueSource() instanceof EntityLivingBase) {
            EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
            if (TargetPredicate.canHostileTarget(attacker) && !BrainUtil.isTargetWithinDistance(this, attacker, 4.0D)) {
                this.brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                this.brain.replaceMemory(MemoryModuleType.ATTACK_TARGET, attacker, 200L);
            }
        }
        return hurt;
    }

    @Nullable
    @Override
    public EntityLivingBase getAttackTarget() {
        return this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
    }

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("zoglinBrain");
        this.brain.tick((WorldServer) this.world, this);
        this.world.profiler.endSection();
        Activity before = this.brain.getTemporaryActivity().orElse(null);
        this.brain.switchActivities(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        Activity after = this.brain.getTemporaryActivity().orElse(null);
        if (after == Activity.FIGHT && before != Activity.FIGHT) this.playSound(PortiumSounds.ENTITY_ZOGLIN_ANGRY, 1.0F, this.getSoundPitch());
        this.dataManager.set(AGGRESSIVE, this.brain.hasMemory(MemoryModuleType.ATTACK_TARGET));
    }

    public boolean isAggressive() {
        return this.dataManager.get(AGGRESSIVE);
    }

    @Override
    public void onLivingUpdate() {
        if (this.attackAnimationTick > 0) --this.attackAnimationTick;
        super.onLivingUpdate();
    }

    @Override
    public void handleStatusUpdate(byte id) {
        if (id == 4) {
            this.attackAnimationTick = 10;
            this.playSound(PortiumSounds.ENTITY_ZOGLIN_ATTACK, 1.0F, this.getSoundPitch());
        } else {
            super.handleStatusUpdate(id);
        }
    }

    @Override
    public int getAttackAnimationTick() {
        return this.attackAnimationTick;
    }

    // ---- Baby ----

    @Override
    public void setChild116(boolean child) {
        this.dataManager.set(BABY, child);
        if (!this.world.isRemote && child) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(0.5D);
        this.updateSize();
    }

    @Override
    public boolean isChild() {
        return this.dataManager.get(BABY);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (BABY.equals(key)) this.updateSize();
    }

    private void updateSize() {
        float scale = this.isChild() ? 0.5F : 1.0F;
        this.setSize(1.3964844F * scale, 1.4F * scale);
    }

    @Override
    public double getMountedYOffset() {
        return this.height - (this.isChild() ? 0.2D : 0.15D);
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (this.isChild()) compound.setBoolean("IsBaby", true);
        compound.setTag("Brain", this.brain.write());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.getBoolean("IsBaby")) this.setChild116(true);
        if (compound.hasKey("Brain", 10)) this.brain.read(compound.getCompoundTag("Brain"));
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return LOOT_TABLE;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return this.experienceValue;
    }

    // ---- Sounds ----

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.world.isRemote) return null;
        return this.brain.hasMemory(MemoryModuleType.ATTACK_TARGET) ? PortiumSounds.ENTITY_ZOGLIN_ANGRY : PortiumSounds.ENTITY_ZOGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_ZOGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_ZOGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(PortiumSounds.ENTITY_ZOGLIN_STEP, 0.15F, 1.0F);
    }
}
