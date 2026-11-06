package com.michaelsebero.portium.entity.monster;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.RangedInteger;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.schedule.Activity;
import com.michaelsebero.portium.entity.ai.brain.task.AnimalBreedTask;
import com.michaelsebero.portium.entity.ai.brain.task.AttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.ChildFollowNearestAdultTask;
import com.michaelsebero.portium.entity.ai.brain.task.DummyTask;
import com.michaelsebero.portium.entity.ai.brain.task.FindNewAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.FirstShuffledTask;
import com.michaelsebero.portium.entity.ai.brain.task.ForgetAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookAtEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookTask;
import com.michaelsebero.portium.entity.ai.brain.task.MoveToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.PredicateTask;
import com.michaelsebero.portium.entity.ai.brain.task.RandomlyStopAttackingTask;
import com.michaelsebero.portium.entity.ai.brain.task.RunAwayTask;
import com.michaelsebero.portium.entity.ai.brain.task.RunSometimesTask;
import com.michaelsebero.portium.entity.ai.brain.task.SupplementedTask;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import com.michaelsebero.portium.entity.ai.brain.task.WalkRandomlyTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkTowardsLookTargetTask;
import com.michaelsebero.portium.entity.monster.piglin.EntityPiglin;
import com.michaelsebero.portium.init.PortiumSounds;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

/** 1.16's {@code HoglinTasks}: hoglins wander, breed on crimson fungus, attack players, and flee piglins in numbers and warped fungus. */
public final class HoglinTasks {

    private static final RangedInteger RETREAT_DURATION = RangedInteger.seconds(5, 20);
    private static final RangedInteger ADULT_FOLLOW_RANGE = RangedInteger.of(5, 16);

    static Brain<EntityHoglin> makeBrain(Brain<EntityHoglin> brain) {
        brain.registerActivity(Activity.CORE, 0, ImmutableList.<Task<? super EntityHoglin>>of(new LookTask(45, 90), new WalkToTargetTask()));
        brain.registerActivity(Activity.IDLE, 10, ImmutableList.<Task<? super EntityHoglin>>of(
                new RandomlyStopAttackingTask(MemoryModuleType.NEAREST_REPELLENT, 200),
                new AnimalBreedTask(EntityHoglin.class, 0.6F, (hoglin, mate) -> ((EntityHoglin) hoglin).spawnBaby((EntityHoglin) mate)),
                RunAwayTask.fromBlock(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, true),
                new ForgetAttackTargetTask<EntityHoglin>(HoglinTasks::findNearestValidAttackTarget),
                new SupplementedTask<EntityHoglin>(EntityHoglin::isAdult, RunAwayTask.fromEntity(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, 0.4F, 8, false)),
                new RunSometimesTask<>(new LookAtEntityTask(8.0F), RangedInteger.of(30, 60)),
                new ChildFollowNearestAdultTask<EntityHoglin>(ADULT_FOLLOW_RANGE, 0.6F), wander()));
        brain.registerActivity(Activity.FIGHT, 10, ImmutableList.<Task<? super EntityHoglin>>of(
                new RandomlyStopAttackingTask(MemoryModuleType.NEAREST_REPELLENT, 200),
                new AnimalBreedTask(EntityHoglin.class, 0.6F, (hoglin, mate) -> ((EntityHoglin) hoglin).spawnBaby((EntityHoglin) mate)),
                new MoveToTargetTask(1.0F), new SupplementedTask<EntityHoglin>(EntityHoglin::isAdult, new AttackTargetTask(40)),
                new SupplementedTask<EntityHoglin>(EntityHoglin::isChild, new AttackTargetTask(15)), new FindNewAttackTargetTask<>(),
                new PredicateTask<EntityHoglin>(HoglinTasks::isBreeding, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
        brain.registerActivity(Activity.AVOID, 10, ImmutableList.<Task<? super EntityHoglin>>of(
                RunAwayTask.fromEntity(MemoryModuleType.AVOID_TARGET, 1.3F, 15, false), wander(),
                new RunSometimesTask<>(new LookAtEntityTask(8.0F), RangedInteger.of(30, 60)),
                new PredicateTask<EntityHoglin>(HoglinTasks::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)), MemoryModuleType.AVOID_TARGET);
        brain.setPersistentActivities(ImmutableSet.of(Activity.CORE));
        brain.setFallbackActivity(Activity.IDLE);
        brain.switchToFallbackActivity();
        return brain;
    }

    private static FirstShuffledTask<EntityHoglin> wander() {
        return new FirstShuffledTask<>(ImmutableList.of(Pair.of(new WalkRandomlyTask(0.4F), 2), Pair.of(new WalkTowardsLookTargetTask(0.4F, 3), 2),
                Pair.of(new DummyTask(30, 60), 1)));
    }

    static void updateActivity(EntityHoglin hoglin) {
        Brain<EntityHoglin> brain = hoglin.getBrain();
        Activity before = brain.getTemporaryActivity().orElse(null);
        brain.switchActivities(ImmutableList.of(Activity.FIGHT, Activity.AVOID, Activity.IDLE));
        Activity after = brain.getTemporaryActivity().orElse(null);
        if (before != after) getSoundForCurrentActivity(hoglin).ifPresent(hoglin::playActivitySound);
        hoglin.setAggressive(brain.hasMemory(MemoryModuleType.ATTACK_TARGET));
    }

    /** Hit a piglin while piglins outnumber the hoglins: run, and tell the others. Otherwise call the others to the fight. */
    static void onHitTarget(EntityHoglin hoglin, EntityLivingBase target) {
        if (hoglin.isChild()) return;
        if (target instanceof EntityPiglin && piglinsOutnumberHoglins(hoglin)) {
            setAvoidTarget(hoglin, target);
            broadcastRetreat(hoglin, target);
        } else {
            broadcastAttackTarget(hoglin, target);
        }
    }

    private static void broadcastRetreat(EntityHoglin hoglin, EntityLivingBase target) {
        for (EntityLivingBase other : getVisibleAdultHoglins(hoglin)) retreatFromNearestTarget((EntityHoglin) other, target);
    }

    private static void retreatFromNearestTarget(EntityHoglin hoglin, EntityLivingBase target) {
        Brain<EntityHoglin> brain = hoglin.getBrain();
        EntityLivingBase nearest = BrainUtil.getNearestEntity(hoglin, brain.getMemory(MemoryModuleType.AVOID_TARGET), target);
        nearest = BrainUtil.getNearestEntity(hoglin, brain.getMemory(MemoryModuleType.ATTACK_TARGET), nearest);
        setAvoidTarget(hoglin, nearest);
    }

    private static void setAvoidTarget(EntityHoglin hoglin, EntityLivingBase target) {
        hoglin.getBrain().removeMemory(MemoryModuleType.ATTACK_TARGET);
        hoglin.getBrain().removeMemory(MemoryModuleType.WALK_TARGET);
        hoglin.getBrain().replaceMemory(MemoryModuleType.AVOID_TARGET, target, RETREAT_DURATION.getRandomWithinRange(hoglin.world.rand));
    }

    private static Optional<? extends EntityLivingBase> findNearestValidAttackTarget(EntityHoglin hoglin) {
        return !isPacified(hoglin) && !isBreeding(hoglin) ? hoglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER) : Optional.empty();
    }

    /** Within 8 blocks of the nearest repellent (pathing there costs -1, so it's avoided). */
    static boolean isPosNearNearestRepellent(EntityHoglin hoglin, BlockPos pos) {
        Optional<BlockPos> repellent = hoglin.getBrain().getMemory(MemoryModuleType.NEAREST_REPELLENT);
        return repellent.isPresent() && repellent.get().distanceSq(pos) < 64.0D;
    }

    private static boolean wantsToStopFleeing(EntityHoglin hoglin) {
        return hoglin.isAdult() && !piglinsOutnumberHoglins(hoglin);
    }

    private static boolean piglinsOutnumberHoglins(EntityHoglin hoglin) {
        if (hoglin.isChild()) return false;
        int piglins = hoglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0);
        int hoglins = hoglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0) + 1;
        return piglins > hoglins;
    }

    /** Hurt: calm no more, stop breeding; a baby flees, an adult fights back (unless fleeing piglins) and rallies the herd. */
    static void wasHurtBy(EntityHoglin hoglin, EntityLivingBase attacker) {
        Brain<EntityHoglin> brain = hoglin.getBrain();
        brain.removeMemory(MemoryModuleType.PACIFIED);
        brain.removeMemory(MemoryModuleType.BREED_TARGET);
        if (hoglin.isChild()) {
            retreatFromNearestTarget(hoglin, attacker);
        } else {
            maybeRetaliate(hoglin, attacker);
        }
    }

    private static void maybeRetaliate(EntityHoglin hoglin, EntityLivingBase attacker) {
        if (hoglin.getBrain().hasActivity(Activity.AVOID) && attacker instanceof EntityPiglin) return;
        if (!TargetPredicate.canHostileTarget(attacker) || attacker instanceof EntityHoglin) return;
        if (BrainUtil.isTargetWithinDistance(hoglin, attacker, 4.0D)) return;
        setAttackTarget(hoglin, attacker);
        broadcastAttackTarget(hoglin, attacker);
    }

    private static void setAttackTarget(EntityHoglin hoglin, EntityLivingBase target) {
        Brain<EntityHoglin> brain = hoglin.getBrain();
        brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        brain.removeMemory(MemoryModuleType.BREED_TARGET);
        brain.replaceMemory(MemoryModuleType.ATTACK_TARGET, target, 200L);
    }

    private static void broadcastAttackTarget(EntityHoglin hoglin, EntityLivingBase target) {
        for (EntityLivingBase other : getVisibleAdultHoglins(hoglin)) setAttackTargetIfCloserThanCurrent((EntityHoglin) other, target);
    }

    private static void setAttackTargetIfCloserThanCurrent(EntityHoglin hoglin, EntityLivingBase target) {
        if (isPacified(hoglin)) return;
        Optional<EntityLivingBase> current = hoglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        setAttackTarget(hoglin, BrainUtil.getNearestEntity(hoglin, current, target));
    }

    static Optional<SoundEvent> getSoundForCurrentActivity(EntityHoglin hoglin) {
        return hoglin.getBrain().getTemporaryActivity().map(activity -> getSoundForActivity(hoglin, activity));
    }

    private static SoundEvent getSoundForActivity(EntityHoglin hoglin, Activity activity) {
        if (activity == Activity.AVOID || hoglin.isConverting()) return PortiumSounds.ENTITY_HOGLIN_RETREAT;
        if (activity == Activity.FIGHT) return PortiumSounds.ENTITY_HOGLIN_ANGRY;
        return isNearRepellent(hoglin) ? PortiumSounds.ENTITY_HOGLIN_RETREAT : PortiumSounds.ENTITY_HOGLIN_AMBIENT;
    }

    private static List<EntityLivingBase> getVisibleAdultHoglins(EntityHoglin hoglin) {
        return hoglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS).orElse(ImmutableList.of());
    }

    private static boolean isNearRepellent(EntityHoglin hoglin) {
        return hoglin.getBrain().hasMemory(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean isBreeding(EntityHoglin hoglin) {
        return hoglin.getBrain().hasMemory(MemoryModuleType.BREED_TARGET);
    }

    static boolean isPacified(EntityHoglin hoglin) {
        return hoglin.getBrain().hasMemory(MemoryModuleType.PACIFIED);
    }

    private HoglinTasks() {}
}
