package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.RangedInteger;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.schedule.Activity;
import com.michaelsebero.portium.entity.ai.brain.task.AttackStrafingTask;
import com.michaelsebero.portium.entity.ai.brain.task.AttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.DummyTask;
import com.michaelsebero.portium.entity.ai.brain.task.EndAttackTask;
import com.michaelsebero.portium.entity.ai.brain.task.FindInteractionAndLookTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.FindNewAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.FirstShuffledTask;
import com.michaelsebero.portium.entity.ai.brain.task.ForgetAttackTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.GetAngryTask;
import com.michaelsebero.portium.entity.ai.brain.task.HuntCelebrationTask;
import com.michaelsebero.portium.entity.ai.brain.task.InteractWithDoorTask;
import com.michaelsebero.portium.entity.ai.brain.task.InteractWithEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookAtEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookTask;
import com.michaelsebero.portium.entity.ai.brain.task.MoveToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.PickupWantedItemTask;
import com.michaelsebero.portium.entity.ai.brain.task.PiglinIdleActivityTask;
import com.michaelsebero.portium.entity.ai.brain.task.PredicateTask;
import com.michaelsebero.portium.entity.ai.brain.task.RideEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.RunAwayTask;
import com.michaelsebero.portium.entity.ai.brain.task.RunSometimesTask;
import com.michaelsebero.portium.entity.ai.brain.task.ShootTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.StopRidingEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.SupplementedTask;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import com.michaelsebero.portium.entity.ai.brain.task.WalkRandomlyTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkTowardsLookTargetTask;
import com.michaelsebero.portium.entity.monster.EntityHoglin;
import com.michaelsebero.portium.entity.monster.EntityZoglin;
import com.michaelsebero.portium.init.PortiumSounds;
import com.michaelsebero.portium.world.PortiumGameRules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * 1.16's {@code PiglinTasks}: a piglin's activities (admiring gold and bartering, fighting, hunting hoglins, fleeing
 * zombified mobs and soul fire, riding, celebrating), and how it reacts to being hurt, to gold, and to players opening
 * what it guards.
 */
public final class PiglinTasks {

    public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
    public static final ResourceLocation BARTERING_LOOT = com.michaelsebero.portium.init.PortiumLootTables.PIGLIN_BARTERING;
    private static final RangedInteger HUNT_COOLDOWN = RangedInteger.seconds(30, 120);
    private static final RangedInteger RIDE_COOLDOWN = RangedInteger.seconds(10, 40);
    private static final RangedInteger RIDE_DURATION = RangedInteger.seconds(10, 30);
    private static final RangedInteger AVOID_HOGLINS = RangedInteger.seconds(5, 20);
    private static final RangedInteger AVOID_ZOMBIFIED = RangedInteger.seconds(5, 7);
    private static final RangedInteger AVOID_NEMESIS = RangedInteger.seconds(5, 7);
    private static final Set<Item> FOOD = ImmutableSet.of(Items.PORKCHOP, Items.COOKED_PORKCHOP);

    // ---- Brain ----

    static Brain<EntityPiglin> makeBrain(EntityPiglin piglin, Brain<EntityPiglin> brain) {
        addCoreActivities(brain);
        addIdleActivities(brain);
        addAdmireItemActivities(brain);
        addFightActivities(piglin, brain);
        addCelebrateActivities(brain);
        addAvoidActivities(brain);
        addRideActivities(brain);
        brain.setPersistentActivities(ImmutableSet.of(Activity.CORE));
        brain.setFallbackActivity(Activity.IDLE);
        brain.switchToFallbackActivity();
        return brain;
    }

    /** A freshly spawned piglin has "hunted recently", so it doesn't go straight for hoglins. */
    static void initMemories(EntityPiglin piglin) {
        piglin.getBrain().replaceMemory(MemoryModuleType.HUNTED_RECENTLY, true, HUNT_COOLDOWN.getRandomWithinRange(piglin.world.rand));
    }

    private static void addCoreActivities(Brain<EntityPiglin> brain) {
        brain.registerActivity(Activity.CORE, 0, ImmutableList.<Task<? super EntityPiglin>>of(new LookTask(45, 90), new WalkToTargetTask(),
                new InteractWithDoorTask(), avoidNemesisIfBaby(), avoidZombified(), new StartAdmiringItemTask(), new AdmireItemTask(120),
                new EndAttackTask(300, PiglinTasks::wantsToDance), new GetAngryTask<>()));
    }

    private static void addIdleActivities(Brain<EntityPiglin> brain) {
        brain.registerActivity(Activity.IDLE, 10, ImmutableList.<Task<? super EntityPiglin>>of(
                new LookAtEntityTask(PiglinTasks::isPlayerHoldingLovedItem, 14.0F),
                new ForgetAttackTargetTask<EntityPiglin>(EntityAbstractPiglin::isAdult, PiglinTasks::findNearestValidAttackTarget),
                new SupplementedTask<>(EntityPiglin::canHunt, new StartHuntTask<>()), avoidRepellent(), babySometimesRideBabyHoglin(),
                lookAround(), walkAround(), new FindInteractionAndLookTargetTask(EntityPlayer.class, 4)));
    }

    private static void addFightActivities(EntityPiglin piglin, Brain<EntityPiglin> brain) {
        brain.registerActivity(Activity.FIGHT, 10, ImmutableList.<Task<? super EntityPiglin>>of(
                new FindNewAttackTargetTask<EntityPiglin>(target -> !isNearestValidAttackTarget(piglin, target)),
                new SupplementedTask<EntityPiglin>(PiglinTasks::hasCrossbow, new AttackStrafingTask<>(5, 0.75F)), new MoveToTargetTask(1.0F),
                new AttackTargetTask(20), new ShootTargetTask<EntityPiglin>(), new FinishedHuntTask<>(),
                new PredicateTask<EntityPiglin>(PiglinTasks::isNearZombified, MemoryModuleType.ATTACK_TARGET)), MemoryModuleType.ATTACK_TARGET);
    }

    private static void addCelebrateActivities(Brain<EntityPiglin> brain) {
        brain.registerActivity(Activity.CELEBRATE, 10, ImmutableList.<Task<? super EntityPiglin>>of(avoidRepellent(),
                new LookAtEntityTask(PiglinTasks::isPlayerHoldingLovedItem, 14.0F),
                new ForgetAttackTargetTask<EntityPiglin>(EntityAbstractPiglin::isAdult, PiglinTasks::findNearestValidAttackTarget),
                new SupplementedTask<EntityPiglin>(piglin -> !piglin.isDancing(), new HuntCelebrationTask<>(2, 1.0F)),
                new SupplementedTask<EntityPiglin>(EntityPiglin::isDancing, new HuntCelebrationTask<>(4, 0.6F)),
                new FirstShuffledTask<EntityPiglin>(ImmutableList.of(Pair.of(new LookAtEntityTask(EntityPiglin.class, 8.0F), 1),
                        Pair.of(new WalkRandomlyTask(0.6F, 2, 1), 1), Pair.of(new DummyTask(10, 20), 1)))), MemoryModuleType.CELEBRATE_LOCATION);
    }

    private static void addAdmireItemActivities(Brain<EntityPiglin> brain) {
        brain.registerActivity(Activity.ADMIRE_ITEM, 10, ImmutableList.<Task<? super EntityPiglin>>of(
                new PickupWantedItemTask<EntityPiglin>(PiglinTasks::hasNotAdmiredItemInOffhand, 1.0F, true, 9), new ForgetAdmiredItemTask<>(9),
                new StopReachingItemTask<>(200, 200)), MemoryModuleType.ADMIRING_ITEM);
    }

    private static void addAvoidActivities(Brain<EntityPiglin> brain) {
        brain.registerActivity(Activity.AVOID, 10, ImmutableList.<Task<? super EntityPiglin>>of(
                RunAwayTask.fromEntity(MemoryModuleType.AVOID_TARGET, 1.0F, 12, true), lookAround(), walkAround(),
                new PredicateTask<EntityPiglin>(PiglinTasks::shouldStopAvoiding, MemoryModuleType.AVOID_TARGET)), MemoryModuleType.AVOID_TARGET);
    }

    private static void addRideActivities(Brain<EntityPiglin> brain) {
        brain.registerActivity(Activity.RIDE, 10, ImmutableList.<Task<? super EntityPiglin>>of(new RideEntityTask<>(0.8F),
                new LookAtEntityTask(PiglinTasks::isPlayerHoldingLovedItem, 8.0F),
                new SupplementedTask<EntityPiglin>(Entity::isRiding, lookAround()),
                new StopRidingEntityTask<EntityPiglin>(8, PiglinTasks::shouldDismount)), MemoryModuleType.RIDE_TARGET);
    }

    private static FirstShuffledTask<EntityPiglin> lookAround() {
        return new FirstShuffledTask<>(ImmutableList.of(Pair.of(new LookAtEntityTask(EntityPlayer.class, 8.0F), 1),
                Pair.of(new LookAtEntityTask(EntityPiglin.class, 8.0F), 1), Pair.of(new LookAtEntityTask(8.0F), 1), Pair.of(new DummyTask(30, 60), 1)));
    }

    private static FirstShuffledTask<EntityPiglin> walkAround() {
        return new FirstShuffledTask<>(ImmutableList.of(Pair.of(new WalkRandomlyTask(0.6F), 2),
                Pair.of(InteractWithEntityTask.of(EntityPiglin.class, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
                Pair.of(new SupplementedTask<EntityPiglin>(PiglinTasks::isNotNearPlayerHoldingWantedItem, new WalkTowardsLookTargetTask(0.6F, 3)), 2),
                Pair.of(new DummyTask(30, 60), 1)));
    }

    private static RunAwayTask<?> avoidRepellent() {
        return RunAwayTask.fromBlock(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, false);
    }

    private static PiglinIdleActivityTask<EntityPiglin, EntityLivingBase> avoidNemesisIfBaby() {
        return new PiglinIdleActivityTask<EntityPiglin, EntityLivingBase>(EntityPiglin::isChild, MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
                MemoryModuleType.AVOID_TARGET, AVOID_NEMESIS);
    }

    private static PiglinIdleActivityTask<EntityPiglin, EntityLivingBase> avoidZombified() {
        return new PiglinIdleActivityTask<>(PiglinTasks::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET,
                AVOID_ZOMBIFIED);
    }

    private static RunSometimesTask<EntityPiglin> babySometimesRideBabyHoglin() {
        return new RunSometimesTask<>(new PiglinIdleActivityTask<EntityPiglin, Entity>(EntityPiglin::isChild, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
                MemoryModuleType.RIDE_TARGET, RIDE_DURATION), RIDE_COOLDOWN);
    }

    /** After the brain ticks: pick the activity, play its sound on a change, and sync aggression and dancing. */
    static void updateActivity(EntityPiglin piglin) {
        Brain<EntityPiglin> brain = piglin.getBrain();
        Activity before = brain.getTemporaryActivity().orElse(null);
        brain.switchActivities(ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE));
        Activity after = brain.getTemporaryActivity().orElse(null);
        if (before != after) getSoundForCurrentActivity(piglin).ifPresent(piglin::playActivitySound);
        piglin.setAggressive(brain.hasMemory(MemoryModuleType.ATTACK_TARGET));
        if (!brain.hasMemory(MemoryModuleType.RIDE_TARGET) && isBabyRidingBaby(piglin)) piglin.dismountRidingEntity();
        if (!brain.hasMemory(MemoryModuleType.CELEBRATE_LOCATION)) brain.removeMemory(MemoryModuleType.DANCING);
        piglin.setDancing(brain.hasMemory(MemoryModuleType.DANCING));
    }

    private static boolean isBabyRidingBaby(EntityPiglin piglin) {
        if (!piglin.isChild()) return false;
        Entity mount = piglin.getRidingEntity();
        return mount instanceof EntityPiglin && ((EntityPiglin) mount).isChild() || mount instanceof EntityHoglin && ((EntityHoglin) mount).isChild();
    }

    // ---- Items ----

    /** Picks up a wanted item: gold nuggets whole, anything else one at a time; loved items are admired, pork eaten. */
    static void pickUpItem(EntityPiglin piglin, EntityItem item) {
        stopWalking(piglin);
        // 1.16's thrown_item_picked_up_by_entity, for the player who threw it.
        if (item.getThrower() != null && piglin.world.getMinecraftServer() != null) {
            net.minecraft.entity.player.EntityPlayerMP thrower = piglin.world.getMinecraftServer().getPlayerList().getPlayerByUsername(item.getThrower());
            if (thrower != null) {
                com.michaelsebero.portium.advancements.ModernTriggers116.THROWN_ITEM_PICKED_UP.trigger(thrower, item.getItem().copy(), piglin, null, null,
                        0.0F, null);
            }
        }
        ItemStack stack;
        if (item.getItem().getItem() == Items.GOLD_NUGGET) {
            piglin.onItemPickup(item, item.getItem().getCount());
            stack = item.getItem();
            item.setDead();
        } else {
            piglin.onItemPickup(item, 1);
            stack = removeOneItemFromItemEntity(item);
        }
        if (isLovedItem(stack)) {
            piglin.getBrain().removeMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            holdInOffhand(piglin, stack);
            admireGoldItem(piglin);
        } else if (isFood(stack.getItem()) && !hasEatenRecently(piglin)) {
            eat(piglin);
        } else if (!piglin.equipIfBetter(stack)) {
            putInInventory(piglin, stack);
        }
    }

    private static void holdInOffhand(EntityPiglin piglin, ItemStack stack) {
        if (isHoldingItemInOffhand(piglin)) piglin.entityDropItem(piglin.getHeldItem(EnumHand.OFF_HAND), 0.0F);
        piglin.holdInOffHand(stack);
    }

    private static ItemStack removeOneItemFromItemEntity(EntityItem item) {
        ItemStack stack = item.getItem();
        ItemStack one = stack.splitStack(1);
        if (stack.isEmpty()) {
            item.setDead();
        } else {
            item.setItem(stack);
        }
        return one;
    }

    /** Done admiring: barter a gold ingot (if it wasn't interrupted), or keep, wear or pocket what it was holding. */
    static void stopHoldingOffHandItem(EntityPiglin piglin, boolean barter) {
        ItemStack stack = piglin.getHeldItem(EnumHand.OFF_HAND);
        piglin.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        if (piglin.isAdult()) {
            boolean currency = isBarteringItem(stack.getItem());
            if (barter && currency) {
                throwItems(piglin, getBarterResponseItems(piglin));
            } else if (!currency && !piglin.equipIfBetter(stack)) {
                putInInventory(piglin, stack);
            }
        } else if (!piglin.equipIfBetter(stack)) {
            ItemStack main = piglin.getHeldItemMainhand();
            if (isLovedItem(main)) {
                putInInventory(piglin, main);
            } else {
                throwItems(piglin, Collections.singletonList(main));
            }
            piglin.holdInMainHand(stack);
        }
    }

    /** On zombifying: drops what it was admiring. */
    static void cancelAdmiring(EntityPiglin piglin) {
        if (isAdmiringItem(piglin) && !piglin.getHeldItemOffhand().isEmpty()) {
            piglin.entityDropItem(piglin.getHeldItemOffhand(), 0.0F);
            piglin.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static void putInInventory(EntityPiglin piglin, ItemStack stack) {
        ItemStack left = piglin.addToInventory(stack);
        throwItemsTowardRandomPos(piglin, Collections.singletonList(left));
    }

    private static void throwItems(EntityPiglin piglin, List<ItemStack> stacks) {
        Optional<EntityPlayer> player = piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        if (player.isPresent()) {
            throwItemsTowardPos(piglin, stacks, player.get().getPositionVector());
        } else {
            throwItemsTowardRandomPos(piglin, stacks);
        }
    }

    private static void throwItemsTowardRandomPos(EntityPiglin piglin, List<ItemStack> stacks) {
        throwItemsTowardPos(piglin, stacks, getRandomNearbyPos(piglin));
    }

    private static void throwItemsTowardPos(EntityPiglin piglin, List<ItemStack> stacks, Vec3d pos) {
        if (stacks.isEmpty()) return;
        piglin.swingArm(EnumHand.OFF_HAND);
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) BrainUtil.spawnItemNearEntity(piglin, stack, pos.add(0.0D, 1.0D, 0.0D));
        }
    }

    private static List<ItemStack> getBarterResponseItems(EntityPiglin piglin) {
        WorldServer world = (WorldServer) piglin.world;
        LootTable table = world.getLootTableManager().getLootTableFromLocation(BARTERING_LOOT);
        return table.generateLootForPools(world.rand, new LootContext.Builder(world).withLootedEntity(piglin).build());
    }

    private static boolean wantsToDance(EntityLivingBase piglin, EntityLivingBase killed) {
        return killed instanceof EntityHoglin && new Random(piglin.world.getTotalWorldTime()).nextFloat() < 0.1F;
    }

    /** 1.16's {@code canPickUpItem}: what a piglin goes for. */
    static boolean wantsToPickUp(EntityPiglin piglin, ItemStack stack) {
        Item item = stack.getItem();
        if (PiglinTags.isRepellentItem(stack)) return false;
        if (isAdmiringDisabled(piglin) && piglin.getBrain().hasMemory(MemoryModuleType.ATTACK_TARGET)) return false;
        if (isBarteringItem(item)) return hasNotAdmiredItemInOffhand(piglin);
        boolean room = piglin.canAddToInventory(stack);
        if (item == Items.GOLD_NUGGET) return room;
        if (isFood(item)) return !hasEatenRecently(piglin) && room;
        if (!isLovedItem(stack)) return piglin.canReplaceCurrentItem(stack);
        return hasNotAdmiredItemInOffhand(piglin) && room;
    }

    public static boolean isLovedItem(ItemStack stack) {
        return PiglinTags.isLoved(stack);
    }

    private static boolean shouldDismount(EntityPiglin piglin, Entity mount) {
        if (!(mount instanceof EntityLiving)) return false;
        EntityLiving living = (EntityLiving) mount;
        return !living.isChild() || !living.isEntityAlive() || wasHurtRecently(piglin) || wasHurtRecently(living)
                || living instanceof EntityPiglin && living.getRidingEntity() == null;
    }

    private static boolean isNearestValidAttackTarget(EntityPiglin piglin, EntityLivingBase target) {
        return findNearestValidAttackTarget(piglin).filter(found -> found == target).isPresent();
    }

    private static boolean isNearZombified(EntityPiglin piglin) {
        Brain<EntityPiglin> brain = piglin.getBrain();
        if (!brain.hasMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) return false;
        EntityLivingBase zombified = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();
        return piglin.getDistanceSq(zombified) < 36.0D;
    }

    /** Whom to attack: who angered it, any player under universal anger, a wither (skeleton), or a player without gold. */
    private static Optional<? extends EntityLivingBase> findNearestValidAttackTarget(EntityPiglin piglin) {
        Brain<EntityPiglin> brain = piglin.getBrain();
        if (isNearZombified(piglin)) return Optional.empty();
        Optional<EntityLivingBase> angryAt = BrainUtil.getTargetFromMemory(piglin, MemoryModuleType.ANGRY_AT);
        if (angryAt.isPresent() && TargetPredicate.canHostileTarget(angryAt.get())) return angryAt;
        if (brain.hasMemory(MemoryModuleType.UNIVERSAL_ANGER)) {
            Optional<EntityPlayer> player = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER);
            if (player.isPresent()) return player;
        }
        Optional<? extends EntityLivingBase> nemesis = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
        if (nemesis.isPresent()) return nemesis;
        Optional<EntityPlayer> noGold = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
        return noGold.isPresent() && TargetPredicate.canHostileTarget(noGold.get()) ? noGold : Optional.empty();
    }

    /** A player opened or broke something piglins guard: those near (and, if {@code checkSight}, seeing it) get angry. */
    public static void angerNearbyPiglins(EntityPlayer player, boolean checkSight) {
        List<EntityPiglin> piglins = player.world.getEntitiesWithinAABB(EntityPiglin.class, player.getEntityBoundingBox().grow(16.0D));
        for (EntityPiglin piglin : piglins) {
            if (!isIdle(piglin) || checkSight && !BrainUtil.isMobVisible(piglin, player)) continue;
            if (PortiumGameRules.get(piglin.world, PortiumGameRules.UNIVERSAL_ANGER, false)) {
                setAngerTargetToNearestTargetablePlayerIfFound(piglin, player);
            } else {
                setAngerTarget(piglin, player);
            }
        }
    }

    /** A player right-clicks with gold: an adult not already admiring takes one ingot. */
    static boolean mobInteract(EntityPiglin piglin, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (!canAdmire(piglin, held)) return false;
        ItemStack one = held.splitStack(1);
        holdInOffhand(piglin, one);
        admireGoldItem(piglin);
        stopWalking(piglin);
        return true;
    }

    static boolean canAdmire(EntityPiglin piglin, ItemStack stack) {
        return !isAdmiringDisabled(piglin) && !isAdmiringItem(piglin) && piglin.isAdult() && isBarteringItem(stack.getItem());
    }

    /** Hurt by something: drop what it's admiring, then flee (babies, or hoglins outnumbering it) or fight back. */
    static void wasHurtBy(EntityPiglin piglin, EntityLivingBase attacker) {
        if (attacker instanceof EntityPiglin) return;
        if (isHoldingItemInOffhand(piglin)) stopHoldingOffHandItem(piglin, false);
        Brain<EntityPiglin> brain = piglin.getBrain();
        brain.removeMemory(MemoryModuleType.CELEBRATE_LOCATION);
        brain.removeMemory(MemoryModuleType.DANCING);
        brain.removeMemory(MemoryModuleType.ADMIRING_ITEM);
        if (attacker instanceof EntityPlayer) brain.replaceMemory(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
        getAvoidTarget(piglin).ifPresent(avoided -> {
            if (avoided.getClass() != attacker.getClass()) brain.removeMemory(MemoryModuleType.AVOID_TARGET);
        });
        if (piglin.isChild()) {
            brain.replaceMemory(MemoryModuleType.AVOID_TARGET, attacker, 100L);
            if (TargetPredicate.canHostileTarget(attacker)) broadcastAngerTarget(piglin, attacker);
        } else if (attacker instanceof EntityHoglin && hoglinsOutnumberPiglins(piglin)) {
            setAvoidTargetAndDontHuntForAWhile(piglin, attacker);
            broadcastRetreat(piglin, attacker);
        } else {
            maybeRetaliate(piglin, attacker);
        }
    }

    static void maybeRetaliate(EntityAbstractPiglin piglin, EntityLivingBase attacker) {
        if (piglin.getBrain().hasActivity(Activity.AVOID) || !TargetPredicate.canHostileTarget(attacker)) return;
        if (BrainUtil.isTargetWithinDistance(piglin, attacker, 4.0D)) return;
        if (attacker instanceof EntityPlayer && PortiumGameRules.get(piglin.world, PortiumGameRules.UNIVERSAL_ANGER, false)) {
            setAngerTargetToNearestTargetablePlayerIfFound(piglin, attacker);
            broadcastUniversalAnger(piglin);
        } else {
            setAngerTarget(piglin, attacker);
            broadcastAngerTarget(piglin, attacker);
        }
    }

    // ---- Sounds ----

    static Optional<SoundEvent> getSoundForCurrentActivity(EntityPiglin piglin) {
        return piglin.getBrain().getTemporaryActivity().map(activity -> getSoundForActivity(piglin, activity));
    }

    private static SoundEvent getSoundForActivity(EntityPiglin piglin, Activity activity) {
        if (activity == Activity.FIGHT) return PortiumSounds.ENTITY_PIGLIN_ANGRY;
        if (piglin.isConverting()) return PortiumSounds.ENTITY_PIGLIN_RETREAT;
        if (activity == Activity.AVOID && isNearAvoidTarget(piglin)) return PortiumSounds.ENTITY_PIGLIN_RETREAT;
        if (activity == Activity.ADMIRE_ITEM) return PortiumSounds.ENTITY_PIGLIN_ADMIRING_ITEM;
        if (activity == Activity.CELEBRATE) return PortiumSounds.ENTITY_PIGLIN_CELEBRATE;
        if (seesPlayerHoldingLovedItem(piglin)) return PortiumSounds.ENTITY_PIGLIN_JEALOUS;
        return isNearRepellent(piglin) ? PortiumSounds.ENTITY_PIGLIN_RETREAT : PortiumSounds.ENTITY_PIGLIN_AMBIENT;
    }

    private static boolean isNearAvoidTarget(EntityPiglin piglin) {
        Brain<EntityPiglin> brain = piglin.getBrain();
        return brain.hasMemory(MemoryModuleType.AVOID_TARGET) && brain.getMemory(MemoryModuleType.AVOID_TARGET).get().getDistanceSq(piglin) < 144.0D;
    }

    // ---- Hunting and anger ----

    static boolean hasAnyoneNearbyHuntedRecently(EntityPiglin piglin) {
        return piglin.getBrain().hasMemory(MemoryModuleType.HUNTED_RECENTLY) || getVisibleAdultPiglins(piglin).stream()
                .anyMatch(other -> BrainUtil.brain(other).hasMemory(MemoryModuleType.HUNTED_RECENTLY));
    }

    private static List<EntityLivingBase> getVisibleAdultPiglins(EntityPiglin piglin) {
        return piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    private static List<EntityLivingBase> getAdultPiglins(EntityAbstractPiglin piglin) {
        return piglin.getBrain().getMemory(MemoryModuleType.NEAREST_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    /** Wears any piece of gold armour. */
    public static boolean isWearingGold(EntityLivingBase entity) {
        for (ItemStack stack : entity.getArmorInventoryList()) {
            if (stack.getItem() instanceof ItemArmor && ((ItemArmor) stack.getItem()).getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD) return true;
        }
        return false;
    }

    private static void stopWalking(EntityPiglin piglin) {
        piglin.getBrain().removeMemory(MemoryModuleType.WALK_TARGET);
        piglin.getNavigator().clearPath();
    }

    static void broadcastAngerTarget(EntityAbstractPiglin piglin, EntityLivingBase target) {
        for (EntityLivingBase other : getAdultPiglins(piglin)) {
            if (!(other instanceof EntityAbstractPiglin)) continue;
            EntityAbstractPiglin ally = (EntityAbstractPiglin) other;
            if (!(target instanceof EntityHoglin) || ally.canHunt() && ((EntityHoglin) target).canBeHunted()) setAngerTargetIfCloserThanCurrent(ally, target);
        }
    }

    static void broadcastUniversalAnger(EntityAbstractPiglin piglin) {
        for (EntityLivingBase other : getAdultPiglins(piglin)) {
            if (other instanceof EntityAbstractPiglin) {
                getNearestVisibleTargetablePlayer((EntityAbstractPiglin) other).ifPresent(player -> setAngerTarget((EntityAbstractPiglin) other, player));
            }
        }
    }

    static void broadcastDontKillAnyMoreHoglinsForAWhile(EntityPiglin piglin) {
        for (EntityLivingBase other : getVisibleAdultPiglins(piglin)) {
            if (other instanceof EntityAbstractPiglin) dontKillAnyMoreHoglinsForAWhile((EntityAbstractPiglin) other);
        }
    }

    static void setAngerTarget(EntityAbstractPiglin piglin, EntityLivingBase target) {
        if (!TargetPredicate.canHostileTarget(target)) return;
        Brain<? extends EntityAbstractPiglin> brain = piglin.getBrain();
        brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        brain.replaceMemory(MemoryModuleType.ANGRY_AT, target.getUniqueID(), 600L);
        if (target instanceof EntityHoglin && piglin.canHunt()) dontKillAnyMoreHoglinsForAWhile(piglin);
        if (target instanceof EntityPlayer && PortiumGameRules.get(piglin.world, PortiumGameRules.UNIVERSAL_ANGER, false)) {
            brain.replaceMemory(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
        }
    }

    private static void setAngerTargetToNearestTargetablePlayerIfFound(EntityAbstractPiglin piglin, EntityLivingBase fallback) {
        Optional<EntityPlayer> player = getNearestVisibleTargetablePlayer(piglin);
        if (player.isPresent()) {
            setAngerTarget(piglin, player.get());
        } else {
            setAngerTarget(piglin, fallback);
        }
    }

    private static void setAngerTargetIfCloserThanCurrent(EntityAbstractPiglin piglin, EntityLivingBase target) {
        Optional<EntityLivingBase> current = BrainUtil.getTargetFromMemory(piglin, MemoryModuleType.ANGRY_AT);
        EntityLivingBase nearest = BrainUtil.getNearestEntity(piglin, current, target);
        if (!current.isPresent() || current.get() != nearest) setAngerTarget(piglin, nearest);
    }

    public static Optional<EntityLivingBase> getAvoidTarget(EntityPiglin piglin) {
        return piglin.getBrain().getMemory(MemoryModuleType.AVOID_TARGET);
    }

    public static Optional<EntityPlayer> getNearestVisibleTargetablePlayer(EntityAbstractPiglin piglin) {
        return piglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER);
    }

    private static void broadcastRetreat(EntityPiglin piglin, EntityLivingBase from) {
        for (EntityLivingBase other : getVisibleAdultPiglins(piglin)) {
            if (other instanceof EntityPiglin) retreatFromNearestTarget((EntityPiglin) other, from);
        }
    }

    private static void retreatFromNearestTarget(EntityPiglin piglin, EntityLivingBase from) {
        Brain<EntityPiglin> brain = piglin.getBrain();
        EntityLivingBase nearest = BrainUtil.getNearestEntity(piglin, brain.getMemory(MemoryModuleType.AVOID_TARGET), from);
        nearest = BrainUtil.getNearestEntity(piglin, brain.getMemory(MemoryModuleType.ATTACK_TARGET), nearest);
        setAvoidTargetAndDontHuntForAWhile(piglin, nearest);
    }

    /** Whether the avoided thing no longer needs avoiding (outnumbered hoglins are fought; zombified mobs only fled up close). */
    private static boolean shouldStopAvoiding(EntityPiglin piglin) {
        Brain<EntityPiglin> brain = piglin.getBrain();
        if (!brain.hasMemory(MemoryModuleType.AVOID_TARGET)) return true;
        EntityLivingBase avoided = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
        if (avoided instanceof EntityHoglin) return !hoglinsOutnumberPiglins(piglin);
        if (isZombified(avoided)) return !brain.hasMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, avoided);
        return false;
    }

    private static boolean hoglinsOutnumberPiglins(EntityPiglin piglin) {
        int piglins = piglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
        int hoglins = piglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
        return hoglins > piglins;
    }

    private static void setAvoidTargetAndDontHuntForAWhile(EntityPiglin piglin, EntityLivingBase target) {
        piglin.getBrain().removeMemory(MemoryModuleType.ANGRY_AT);
        piglin.getBrain().removeMemory(MemoryModuleType.ATTACK_TARGET);
        piglin.getBrain().removeMemory(MemoryModuleType.WALK_TARGET);
        piglin.getBrain().replaceMemory(MemoryModuleType.AVOID_TARGET, target, AVOID_HOGLINS.getRandomWithinRange(piglin.world.rand));
        dontKillAnyMoreHoglinsForAWhile(piglin);
    }

    static void dontKillAnyMoreHoglinsForAWhile(EntityAbstractPiglin piglin) {
        piglin.getBrain().replaceMemory(MemoryModuleType.HUNTED_RECENTLY, true, HUNT_COOLDOWN.getRandomWithinRange(piglin.world.rand));
    }

    private static void eat(EntityPiglin piglin) {
        piglin.getBrain().replaceMemory(MemoryModuleType.ATE_RECENTLY, true, 200L);
    }

    private static Vec3d getRandomNearbyPos(EntityPiglin piglin) {
        Vec3d pos = RandomPositionGenerator.getLandPos(piglin, 4, 2);
        return pos == null ? piglin.getPositionVector() : pos;
    }

    private static boolean hasEatenRecently(EntityPiglin piglin) {
        return piglin.getBrain().hasMemory(MemoryModuleType.ATE_RECENTLY);
    }

    static boolean isIdle(EntityAbstractPiglin piglin) {
        return piglin.getBrain().hasActivity(Activity.IDLE);
    }

    private static boolean hasCrossbow(EntityLivingBase piglin) {
        return BrainUtil.isCrossbow(piglin.getHeldItemMainhand()) || BrainUtil.isCrossbow(piglin.getHeldItemOffhand());
    }

    private static void admireGoldItem(EntityLivingBase piglin) {
        BrainUtil.brain(piglin).replaceMemory(MemoryModuleType.ADMIRING_ITEM, true, 120L);
    }

    static boolean isAdmiringItem(EntityPiglin piglin) {
        return piglin.getBrain().hasMemory(MemoryModuleType.ADMIRING_ITEM);
    }

    static boolean isBarteringItem(Item item) {
        return item == BARTERING_ITEM;
    }

    private static boolean isFood(Item item) {
        return FOOD.contains(item);
    }

    private static boolean isNearRepellent(EntityPiglin piglin) {
        return piglin.getBrain().hasMemory(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean seesPlayerHoldingLovedItem(EntityLivingBase piglin) {
        return BrainUtil.brain(piglin).hasMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static boolean isNotNearPlayerHoldingWantedItem(EntityLivingBase piglin) {
        return !seesPlayerHoldingLovedItem(piglin);
    }

    /** A player holding something piglins love (they stare). */
    public static boolean isPlayerHoldingLovedItem(EntityLivingBase entity) {
        return entity instanceof EntityPlayer && (isLovedItem(entity.getHeldItemMainhand()) || isLovedItem(entity.getHeldItemOffhand()));
    }

    private static boolean isAdmiringDisabled(EntityPiglin piglin) {
        return piglin.getBrain().hasMemory(MemoryModuleType.ADMIRING_DISABLED);
    }

    private static boolean wasHurtRecently(EntityLivingBase entity) {
        return entity instanceof com.michaelsebero.portium.entity.ai.brain.IBrainHolder && BrainUtil.brain(entity).hasMemory(MemoryModuleType.HURT_BY);
    }

    private static boolean isHoldingItemInOffhand(EntityPiglin piglin) {
        return !piglin.getHeldItemOffhand().isEmpty();
    }

    static boolean hasNotAdmiredItemInOffhand(EntityPiglin piglin) {
        return piglin.getHeldItemOffhand().isEmpty() || !isLovedItem(piglin.getHeldItemOffhand());
    }

    /** Zombified piglins (1.12's zombie pigmen) and zoglins: what piglins flee. */
    public static boolean isZombified(Entity entity) {
        return entity instanceof EntityPigZombie || entity instanceof EntityZoglin;
    }

    private PiglinTasks() {}
}
