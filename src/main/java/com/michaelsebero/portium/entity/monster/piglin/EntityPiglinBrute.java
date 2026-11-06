package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.GlobalPos;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.schedule.Activity;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.ai.brain.task.AttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.DummyTask;
import com.michaelsebero.portium.entity.ai.brain.task.FindInteractionAndLookTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.FindNewAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.FirstShuffledTask;
import com.michaelsebero.portium.entity.ai.brain.task.ForgetAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.GetAngryTask;
import com.michaelsebero.portium.entity.ai.brain.task.InteractWithDoorTask;
import com.michaelsebero.portium.entity.ai.brain.task.InteractWithEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookAtEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookTask;
import com.michaelsebero.portium.entity.ai.brain.task.MoveToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import com.michaelsebero.portium.entity.ai.brain.task.WalkRandomlyTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkTowardsPosTask;
import com.michaelsebero.portium.entity.ai.brain.task.WorkTask;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 1.16.2's piglin brute: a bastion guard with a golden axe that attacks players on sight (gold or not), and withers;
 * it doesn't barter, hunt or flee, and keeps near the spot it spawned.
 */
public class EntityPiglinBrute extends EntityAbstractPiglin {

    @SuppressWarnings({"rawtypes"})
    private static final List SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS,
            SensorType.HURT_BY, PiglinBruteSensor.TYPE);
    private static final ImmutableList<MemoryModuleType<?>> MEMORIES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.OPENED_DOORS,
            MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER,
            MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, MemoryModuleType.NEAREST_ADULT_PIGLINS, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN,
            MemoryModuleType.INTERACTION_TARGET, MemoryModuleType.PATH, MemoryModuleType.ANGRY_AT, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.HOME);

    private final Brain<EntityPiglinBrute> brain;

    @SuppressWarnings("unchecked")
    public EntityPiglinBrute(World world) {
        super(world);
        this.setSize(0.6F, 1.95F);
        this.experienceValue = 20;
        this.brain = makeBrain(this, new Brain<EntityPiglinBrute>(MEMORIES, SENSORS));
    }

    private static Brain<EntityPiglinBrute> makeBrain(EntityPiglinBrute brute, Brain<EntityPiglinBrute> brain) {
        brain.registerActivity(Activity.CORE, 0, ImmutableList.<Task<? super EntityPiglinBrute>>of(new LookTask(45, 90), new WalkToTargetTask(),
                new InteractWithDoorTask(), new GetAngryTask<>()));
        brain.registerActivity(Activity.IDLE, 10, ImmutableList.<Task<? super EntityPiglinBrute>>of(
                new ForgetAttackTargetTask<EntityPiglinBrute>(EntityPiglinBrute::findNearestValidAttackTarget),
                new FirstShuffledTask<EntityPiglinBrute>(ImmutableList.of(Pair.of(new LookAtEntityTask(EntityPlayer.class, 8.0F), 1),
                        Pair.of(new LookAtEntityTask(EntityPiglin.class, 8.0F), 1), Pair.of(new LookAtEntityTask(EntityPiglinBrute.class, 8.0F), 1),
                        Pair.of(new LookAtEntityTask(8.0F), 1), Pair.of(new DummyTask(30, 60), 1))),
                new FirstShuffledTask<EntityPiglinBrute>(ImmutableList.of(Pair.of(new WalkRandomlyTask(0.6F), 2),
                        Pair.of(InteractWithEntityTask.of(EntityPiglin.class, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
                        Pair.of(InteractWithEntityTask.of(EntityPiglinBrute.class, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
                        Pair.of(new WalkTowardsPosTask(MemoryModuleType.HOME, 0.6F, 2, 100), 2), Pair.of(new WorkTask(MemoryModuleType.HOME, 0.6F, 5), 2),
                        Pair.of(new DummyTask(30, 60), 1))),
                new FindInteractionAndLookTargetTask(EntityPlayer.class, 4)));
        brain.registerActivity(Activity.FIGHT, 10, ImmutableList.<Task<? super EntityPiglinBrute>>of(
                new FindNewAttackTargetTask<EntityPiglinBrute>(target -> !isNearestValidAttackTarget(brute, target)), new MoveToTargetTask(1.0F),
                new AttackTargetTask(20)), MemoryModuleType.ATTACK_TARGET);
        brain.setPersistentActivities(ImmutableSet.of(Activity.CORE));
        brain.setFallbackActivity(Activity.IDLE);
        brain.switchToFallbackActivity();
        return brain;
    }

    private static boolean isNearestValidAttackTarget(EntityPiglinBrute brute, EntityLivingBase target) {
        return findNearestValidAttackTarget(brute).filter(found -> found == target).isPresent();
    }

    /** Who angered it, else the nearest targetable player within 12 blocks, else a wither (skeleton). */
    private static Optional<? extends EntityLivingBase> findNearestValidAttackTarget(EntityPiglinBrute brute) {
        Optional<EntityLivingBase> angryAt = BrainUtil.getTargetFromMemory(brute, MemoryModuleType.ANGRY_AT);
        if (angryAt.isPresent() && TargetPredicate.canHostileTarget(angryAt.get())) return angryAt;
        Optional<EntityPlayer> player = brute.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER)
                .filter(found -> found.getDistanceSq(brute) < 144.0D);
        return player.isPresent() ? player : brute.brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(7.0D);
    }

    @Override
    public Brain<EntityPiglinBrute> getBrain() {
        return this.brain;
    }

    @Nullable
    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.brain.setMemory(MemoryModuleType.HOME, GlobalPos.of(this.world.provider.getDimension(), new BlockPos(this)));
        this.setEquipmentBasedOnDifficulty(difficulty);
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
    }

    @Override
    public boolean canHunt() {
        return false;
    }

    /** Only a better golden axe. */
    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return stack.getItem() == Items.GOLDEN_AXE && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.world, this)
                && EntityPiglin.isBetterGear(stack, this.getHeldItemMainhand());
    }

    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        if (this.wantsToPickUp(item.getItem())) super.updateEquipmentIfNeeded(item);
    }

    @Override
    protected void updateAITasks() {
        this.world.profiler.startSection("piglinBruteBrain");
        this.brain.tick((WorldServer) this.world, this);
        this.world.profiler.endSection();
        Activity before = this.brain.getTemporaryActivity().orElse(null);
        this.brain.switchActivities(ImmutableList.of(Activity.FIGHT, Activity.IDLE));
        Activity after = this.brain.getTemporaryActivity().orElse(null);
        if (before != after) this.playAngryIfFighting();
        this.setAggressive(this.brain.hasMemory(MemoryModuleType.ATTACK_TARGET));
        if (this.world.rand.nextFloat() < 0.0125F) this.playAngryIfFighting();
        super.updateAITasks();
    }

    private void playAngryIfFighting() {
        if (this.brain.getTemporaryActivity().orElse(null) == Activity.FIGHT) {
            this.playSound(PortiumSounds.ENTITY_PIGLIN_BRUTE_ANGRY, 1.0F, this.getSoundPitch());
        }
    }

    @Override
    public PiglinAction getAction() {
        return this.isAggressive() && this.holdsMeleeWeapon() ? PiglinAction.ATTACKING_WITH_MELEE_WEAPON : PiglinAction.DEFAULT;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        boolean hurt = super.attackEntityFrom(source, amount);
        if (this.world.isRemote) return false;
        if (hurt && source.getTrueSource() instanceof EntityLivingBase && !(source.getTrueSource() instanceof EntityAbstractPiglin)) {
            PiglinTasks.maybeRetaliate(this, (EntityLivingBase) source.getTrueSource());
        }
        return hurt;
    }

    @Override
    public float getEyeHeight() {
        return 1.74F;
    }

    @Override
    public double getMountedYOffset() {
        return this.height * 0.92D;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return this.experienceValue;
    }

    @Nullable
    @Override
    protected ResourceLocation getLootTable() {
        return com.michaelsebero.portium.init.PortiumLootTables.PIGLIN_BRUTE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PortiumSounds.ENTITY_PIGLIN_BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PortiumSounds.ENTITY_PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PortiumSounds.ENTITY_PIGLIN_BRUTE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, Block block) {
        this.playSound(PortiumSounds.ENTITY_PIGLIN_BRUTE_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void playConvertedSound() {
        this.playSound(PortiumSounds.ENTITY_PIGLIN_BRUTE_CONVERTED_TO_ZOMBIFIED, 1.0F, this.getSoundPitch());
    }
}
