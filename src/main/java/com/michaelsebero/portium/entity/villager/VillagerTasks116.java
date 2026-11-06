package com.michaelsebero.portium.entity.villager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.RangedInteger;
import com.michaelsebero.portium.entity.ai.brain.memory.BlockPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.EntityPosWrapper;
import com.michaelsebero.portium.entity.ai.brain.memory.GlobalPos;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.WalkTarget;
import com.michaelsebero.portium.entity.ai.brain.schedule.Activity;
import com.michaelsebero.portium.entity.ai.brain.schedule.Schedule;
import com.michaelsebero.portium.entity.ai.brain.task.ChildFollowNearestAdultTask;
import com.michaelsebero.portium.entity.ai.brain.task.FindInteractionAndLookTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.FirstShuffledTask;
import com.michaelsebero.portium.entity.ai.brain.task.InteractWithDoorTask;
import com.michaelsebero.portium.entity.ai.brain.task.InteractWithEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookAtEntityTask;
import com.michaelsebero.portium.entity.ai.brain.task.LookTask;
import com.michaelsebero.portium.entity.ai.brain.task.PickupWantedItemTask;
import com.michaelsebero.portium.entity.ai.brain.task.RunAwayTask;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import com.michaelsebero.portium.entity.ai.brain.task.WalkRandomlyTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkToTargetTask;
import com.michaelsebero.portium.entity.ai.brain.task.WalkTowardsPosTask;
import com.michaelsebero.portium.entity.ai.brain.task.WorkTask;
import com.michaelsebero.portium.world.poi.PoiManager116;
import com.michaelsebero.portium.world.poi.PoiType116;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 1.14's villager brain: core tasks always (swim, doors, look, walk, trade, find a job, a bed and the bell, panic), then
 * one activity at a time by its schedule: idle (wander, chat, breed, share food), work (at its job site: work sound,
 * restock, farming), meet (at the bell: gossip), rest (to bed, sleep), play (children) and panic (flee hostiles).
 */
public final class VillagerTasks116 {

    static Brain<EntityVillager116> makeBrain(EntityVillager116 villager, Brain<EntityVillager116> brain) {
        float speed = 0.5F;
        brain.setSchedule(villager.isChild() ? Schedule.VILLAGER_BABY : Schedule.VILLAGER_DEFAULT);
        brain.registerActivity(Activity.CORE, 0, ImmutableList.of(new SwimTask(), new InteractWithDoorTask(), new LookTask(45, 90), new PanicTriggerTask(),
                new HideTriggerTask(),
                new WakeUpTask(), new WalkToTargetTask(), new TradeTask(), new ValidatePoisTask(), new FindJobSiteTask(), new ClaimJobSiteTask(),
                new FindPoiTask(MemoryModuleType.HOME, PoiType116.HOME, true), new FindPoiTask(MemoryModuleType.MEETING_POINT, PoiType116.MEETING, false),
                new PickupWantedItemTask<>(speed, false, 4)));
        brain.registerActivity(Activity.IDLE, 2, ImmutableList.of(
                new FirstShuffledTask<>(ImmutableList.of(
                        Pair.of(InteractWithEntityTask.of(EntityVillager116.class, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2),
                        // 1.14: a villager able to breed picks another as its breed target (BreedTask takes it from there).
                        Pair.of(new InteractWithEntityTask<EntityVillager116, net.minecraft.entity.EntityAgeable>(EntityVillager116.class, 8,
                                EntityVillager116::canBreed, target -> target instanceof EntityVillager116 && ((EntityVillager116) target).canBreed(),
                                MemoryModuleType.BREED_TARGET, speed, 2), 1),
                        // 1.14's idle wanders; it goes to the bell only in the meeting hours (MEET), not whenever idle.
                        Pair.of(new WalkRandomlyTask(speed), 1),
                        Pair.of(new IdleTask(), 1))),
                new GiveHeroGiftsTask(), new GossipTask(), new ShareFoodTask(), new BreedTask(),
                new FindInteractionAndLookTargetTask(EntityPlayer.class, 4),
                new LookAtEntityTask(EntityVillager116.class, 8.0F)));
        brain.registerActivity(Activity.WORK, 2, ImmutableList.of(new WorkAtPoiTask(), new FarmTask(), new GiveHeroGiftsTask(),
                new FirstShuffledTask<>(ImmutableList.of(
                        Pair.of(new WalkTowardsPosTask(MemoryModuleType.JOB_SITE, speed, 1, 48), 5),
                        Pair.of(new WorkTask(MemoryModuleType.JOB_SITE, speed * 0.8F, 4), 2),
                        Pair.of(new IdleTask(), 1))),
                new FindInteractionAndLookTargetTask(EntityPlayer.class, 4),
                new LookAtEntityTask(EntityPlayer.class, 8.0F)), MemoryModuleType.JOB_SITE);
        brain.registerActivity(Activity.MEET, 2, ImmutableList.of(
                new GiveHeroGiftsTask(),
                new WalkTowardsPosTask(MemoryModuleType.MEETING_POINT, speed, 6, 100),
                new FirstShuffledTask<>(ImmutableList.of(
                        Pair.of(InteractWithEntityTask.of(EntityVillager116.class, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2),
                        Pair.of(new WorkTask(MemoryModuleType.MEETING_POINT, speed * 0.8F, 6), 2),
                        Pair.of(new IdleTask(), 1))),
                new GossipTask(), new ShareFoodTask(),
                new LookAtEntityTask(EntityVillager116.class, 8.0F)), MemoryModuleType.MEETING_POINT);
        brain.registerActivity(Activity.REST, 2, ImmutableList.of(new WalkTowardsPosTask(MemoryModuleType.HOME, speed, 1, 150), new SleepTask(),
                new FirstShuffledTask<>(ImmutableList.of(Pair.of(new IdleTask(), 1)))));
        brain.registerActivity(Activity.PLAY, 2, ImmutableList.of(
                new FirstShuffledTask<>(ImmutableList.of(
                        Pair.of(new WalkRandomlyTask(speed * 1.2F), 2),
                        Pair.of(InteractWithEntityTask.of(EntityVillager116.class, 8, MemoryModuleType.INTERACTION_TARGET, speed * 1.2F, 2), 2),
                        Pair.of(new ChildFollowNearestAdultTask(RangedInteger.of(5, 16), speed), 1),
                        Pair.of(new IdleTask(), 1))),
                new LookAtEntityTask(EntityVillager116.class, 8.0F)));
        brain.registerActivity(Activity.PANIC, 0, ImmutableList.of(new CalmDownTask(),
                RunAwayTask.fromEntity(MemoryModuleType.NEAREST_HOSTILE, speed * 1.5F, 6, false),
                RunAwayTask.fromEntity(MemoryModuleType.HURT_BY_ENTITY, speed * 1.5F, 6, false),
                new WalkRandomlyTask(speed * 1.5F, 2, 2)));
        for (Activity hiding : new Activity[] {Activity.RAID, Activity.HIDE}) {
            brain.registerActivity(hiding, 0, ImmutableList.of(new WalkTowardsPosTask(MemoryModuleType.HOME, speed * 1.2F, 1, 100),
                    RunAwayTask.fromEntity(MemoryModuleType.NEAREST_HOSTILE, speed * 1.5F, 6, false), new IdleTask()));
        }
        brain.setPersistentActivities(ImmutableSet.of(Activity.CORE));
        brain.setFallbackActivity(Activity.IDLE);
        brain.switchToFallbackActivity();
        return brain;
    }

    /** Follows the schedule, except while panicking (which ends on its own once calm). */
    static void updateActivities(EntityVillager116 villager) {
        Brain<EntityVillager116> brain = villager.getBrain();
        Schedule wanted = villager.isChild() ? Schedule.VILLAGER_BABY : Schedule.VILLAGER_DEFAULT;
        if (brain.getSchedule() != wanted) brain.setSchedule(wanted);
        if (brain.hasActivity(Activity.PANIC) || brain.hasActivity(Activity.RAID) || brain.hasActivity(Activity.HIDE)) return;
        brain.updateActivity(villager.world.getWorldTime(), villager.world.getTotalWorldTime());
    }

    private static PoiManager116 pois(WorldServer world) {
        return PoiManager116.get(world);
    }

    private static GlobalPos global(WorldServer world, BlockPos pos) {
        return GlobalPos.of(world.provider.getDimension(), pos);
    }

    // ---- Core ----

    /** Keeps its head above water. */
    static final class SwimTask extends Task<EntityVillager116> {
        SwimTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            return villager.isInWater() || villager.isInLava();
        }

        @Override
        protected boolean shouldContinueExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            return this.shouldExecute(world, villager);
        }

        @Override
        protected void updateTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            if (villager.getRNG().nextFloat() < 0.8F) villager.getJumpHelper().setJumping();
        }
    }

    /** Panics when hurt or a hostile comes near. */
    static final class PanicTriggerTask extends Task<EntityVillager116> {
        PanicTriggerTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            Brain<EntityVillager116> brain = villager.getBrain();
            return !brain.hasActivity(Activity.PANIC) && (brain.hasMemory(MemoryModuleType.NEAREST_HOSTILE)
                    || brain.hasMemory(MemoryModuleType.HURT_BY_ENTITY) && !(brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).get() instanceof EntityPlayer));
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            Brain<EntityVillager116> brain = villager.getBrain();
            if (villager.isSleeping()) villager.wakeUp();
            brain.removeMemory(MemoryModuleType.PATH);
            brain.removeMemory(MemoryModuleType.WALK_TARGET);
            brain.removeMemory(MemoryModuleType.LOOK_TARGET);
            brain.removeMemory(MemoryModuleType.BREED_TARGET);
            brain.removeMemory(MemoryModuleType.INTERACTION_TARGET);
            brain.switchTo(Activity.PANIC);
        }
    }

    /**
     * 1.14's raid and bell reactions: during a raid it hides at home (RAID); for fifteen seconds after hearing a bell
     * rung (HIDE). Back to the schedule after.
     */
    static final class HideTriggerTask extends Task<EntityVillager116> {
        HideTriggerTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            return world.getTotalWorldTime() % 20L == villager.getEntityId() % 20L;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            Brain<EntityVillager116> brain = villager.getBrain();
            if (brain.hasActivity(Activity.PANIC)) return;
            com.michaelsebero.portium.entity.raid.RaidManager116 raids = com.michaelsebero.portium.entity.raid.RaidManager116.get(world);
            com.michaelsebero.portium.entity.raid.Raid116 raid = raids == null ? null : raids.findRaid(new BlockPos(villager), 64.0D);
            boolean raiding = raid != null && raid.isStarted() && !raid.isOver();
            long heard = com.michaelsebero.portium.entity.HeardBell.lastHeard(villager);
            boolean bell = heard >= 0L && gameTime - heard < 300L;
            if (raiding) {
                if (!brain.hasActivity(Activity.RAID)) {
                    if (villager.isSleeping()) villager.wakeUp();
                    brain.removeMemory(MemoryModuleType.WALK_TARGET);
                    brain.switchTo(Activity.RAID);
                }
            } else if (bell) {
                if (!brain.hasActivity(Activity.HIDE)) {
                    brain.removeMemory(MemoryModuleType.WALK_TARGET);
                    brain.switchTo(Activity.HIDE);
                }
            } else if (brain.hasActivity(Activity.RAID) || brain.hasActivity(Activity.HIDE)) {
                brain.switchToFallbackActivity();
                updateActivities(villager);
            }
        }
    }

    /** Back to the schedule once nothing is chasing it; panicking villagers may summon a golem. */
    static final class CalmDownTask extends Task<EntityVillager116> {
        CalmDownTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            return true;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            Brain<EntityVillager116> brain = villager.getBrain();
            boolean hurt = brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).map(attacker -> attacker.isEntityAlive()
                    && attacker.getDistanceSq(villager) <= 36.0D).orElse(false);
            if (!hurt) brain.removeMemory(MemoryModuleType.HURT_BY_ENTITY);
            if (!hurt && !brain.hasMemory(MemoryModuleType.NEAREST_HOSTILE)) {
                brain.removeMemory(MemoryModuleType.HURT_BY);
                brain.switchToFallbackActivity();
                updateActivities(villager);
            } else if (gameTime % 100L == 0L) {
                VillagerGolems116.trySummonFromPanic(villager, gameTime);
            }
        }
    }

    /** Gets out of bed once it's not time to rest. */
    static final class WakeUpTask extends Task<EntityVillager116> {
        WakeUpTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            return villager.isSleeping() && !villager.getBrain().hasActivity(Activity.REST);
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            villager.wakeUp();
        }
    }

    /** Stands still facing its customer while trading. */
    static final class TradeTask extends Task<EntityVillager116> {
        TradeTask() {
            super(ImmutableMap.of(), Integer.MAX_VALUE);
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            EntityPlayer customer = villager.getCustomer();
            return customer != null && villager.isEntityAlive() && !villager.isInWater() && customer.getDistanceSq(villager) <= 16.0D;
        }

        @Override
        protected boolean shouldContinueExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            return this.shouldExecute(world, villager);
        }

        @Override
        protected void updateTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            Brain<EntityVillager116> brain = villager.getBrain();
            brain.removeMemory(MemoryModuleType.WALK_TARGET);
            villager.getNavigator().clearPath();
            EntityPlayer customer = villager.getCustomer();
            if (customer != null) brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityPosWrapper(customer, true));
        }

        @Override
        protected void resetTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            if (villager.getCustomer() != null && villager.getCustomer().getDistanceSq(villager) > 16.0D) villager.setCustomer(null);
        }
    }

    /**
     * Drops points that are gone: a job site whose workstation was broken (a villager that never traded loses its
     * job with it), a bed or bell that's gone. Also claims again what a loaded villager remembers.
     */
    static final class ValidatePoisTask extends Task<EntityVillager116> {
        private boolean reclaimed;

        ValidatePoisTask() {
            super(ImmutableMap.of());
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            return !this.reclaimed || world.getTotalWorldTime() % 20L == villager.getEntityId() % 20L;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            if (!this.reclaimed) {
                this.reclaimed = true;
                villager.reclaimPois();
            }
            Brain<EntityVillager116> brain = villager.getBrain();
            PoiManager116 pois = pois(world);
            brain.getMemory(MemoryModuleType.JOB_SITE).ifPresent(site -> {
                if (!sameWorld(world, site)) return;
                PoiType116 wanted = PoiType116.workstation(villager.getVillagerData().getProfession());
                if (!villager.world.isBlockLoaded(site.getPos())) return;
                if (wanted == null || pois.getType(site.getPos()) != wanted) {
                    villager.releasePoi(MemoryModuleType.JOB_SITE);
                    if (!villager.hasTraded()) villager.setVillagerData(villager.getVillagerData().withProfession(VillagerProfession116.NONE));
                    world.setEntityState(villager, (byte) 13);
                }
            });
            brain.getMemory(MemoryModuleType.HOME).ifPresent(home -> {
                if (sameWorld(world, home) && villager.world.isBlockLoaded(home.getPos()) && pois.getType(home.getPos()) != PoiType116.HOME) {
                    villager.releasePoi(MemoryModuleType.HOME);
                }
            });
            brain.getMemory(MemoryModuleType.MEETING_POINT).ifPresent(bell -> {
                if (sameWorld(world, bell) && villager.world.isBlockLoaded(bell.getPos()) && pois.getType(bell.getPos()) != PoiType116.MEETING) {
                    brain.removeMemory(MemoryModuleType.MEETING_POINT);
                }
            });
            // A villager with a profession but no job site and nothing traded goes back to being jobless after a while.
            VillagerProfession116 profession = villager.getVillagerData().getProfession();
            if (profession.hasWorkstation() && !brain.hasMemory(MemoryModuleType.JOB_SITE) && !brain.hasMemory(MemoryModuleType.POTENTIAL_JOB_SITE)
                    && !villager.hasTraded()) {
                villager.setVillagerData(villager.getVillagerData().withProfession(VillagerProfession116.NONE));
            }
        }
    }

    static boolean sameWorld(WorldServer world, GlobalPos pos) {
        return pos.getDimension() == world.provider.getDimension();
    }

    /** Looks for a free workstation within 48 blocks: of its own kind if it has a profession, any kind if jobless. */
    static final class FindJobSiteTask extends Task<EntityVillager116> {
        private long nextRun;

        FindJobSiteTask() {
            super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryModuleStatus.VALUE_ABSENT, MemoryModuleType.POTENTIAL_JOB_SITE,
                    MemoryModuleStatus.VALUE_ABSENT));
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            if (villager.isChild() || villager.isSleeping() || world.getTotalWorldTime() < this.nextRun) return false;
            return villager.getVillagerData().getProfession() != VillagerProfession116.NITWIT;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            this.nextRun = gameTime + 20L + villager.getRNG().nextInt(20);
            VillagerProfession116 profession = villager.getVillagerData().getProfession();
            PoiType116 own = PoiType116.workstation(profession);
            pois(world).take(type -> own != null ? type == own : type.isWorkstation(), pos -> true, new BlockPos(villager), 48, villager.getUniqueID())
                    .ifPresent(pos -> {
                        villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, global(world, pos));
                        BrainUtil.setTargetPosition(villager, pos, 0.5F, 1);
                    });
        }
    }

    /** Takes the workstation it walked to, and its profession; gives it up if it couldn't get there. */
    static final class ClaimJobSiteTask extends Task<EntityVillager116> {
        ClaimJobSiteTask() {
            super(ImmutableMap.of(MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleStatus.VALUE_PRESENT));
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            return !villager.isSleeping();
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            Brain<EntityVillager116> brain = villager.getBrain();
            GlobalPos site = brain.getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();
            if (!sameWorld(world, site)) {
                brain.removeMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
                return;
            }
            PoiType116 type = pois(world).getType(site.getPos());
            if (type == null || !type.isWorkstation()) {
                villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
                return;
            }
            if (site.getPos().distanceSqToCenter(villager.posX, villager.posY, villager.posZ) <= 4.0D) {
                brain.removeMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
                brain.setMemory(MemoryModuleType.JOB_SITE, site);
                villager.takeProfession(type.getProfession());
                world.setEntityState(villager, (byte) 14);
                return;
            }
            long stuckSince = brain.getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE).orElse(gameTime);
            if (gameTime - stuckSince > 600L) {
                villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
                brain.removeMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            } else {
                // Heads for it every tick, as 1.14's potential job site task does: idle wandering (which sets walk
                // targets of its own) mustn't keep a jobless villager from ever reaching its workstation.
                BrainUtil.setTargetPosition(villager, site.getPos(), 0.5F, 1);
            }
        }
    }

    /** Remembers the nearest free bed (claiming it) or bell within 48 blocks. */
    static final class FindPoiTask extends Task<EntityVillager116> {
        private final MemoryModuleType<GlobalPos> memory;
        private final PoiType116 type;
        private final boolean claim;
        private long nextRun;

        FindPoiTask(MemoryModuleType<GlobalPos> memory, PoiType116 type, boolean claim) {
            super(ImmutableMap.of(memory, MemoryModuleStatus.VALUE_ABSENT));
            this.memory = memory;
            this.type = type;
            this.claim = claim;
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            return !(this.claim && villager.isChild()) && world.getTotalWorldTime() >= this.nextRun;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            this.nextRun = gameTime + 40L + villager.getRNG().nextInt(40);
            PoiManager116 pois = pois(world);
            Optional<BlockPos> found = this.claim ? pois.take(type -> type == this.type, pos -> !occupied(world, pos), new BlockPos(villager), 48,
                    villager.getUniqueID()) : pois.findNearest(type -> type == this.type, new BlockPos(villager), 48, false);
            found.ifPresent(pos -> villager.getBrain().setMemory(this.memory, global(world, pos)));
        }

        private static boolean occupied(WorldServer world, BlockPos pos) {
            IBlockState state = world.getBlockState(pos);
            return state.getBlock() instanceof BlockBed && state.getValue(BlockBed.OCCUPIED);
        }
    }

    // ---- Idle and meeting ----

    /** Stands about for a while. */
    static final class IdleTask extends Task<EntityLivingBase> {
        IdleTask() {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.VALUE_ABSENT), 30, 60);
        }

        @Override
        protected boolean shouldContinueExecuting(WorldServer world, EntityLivingBase entity, long gameTime) {
            return true;
        }
    }

    /** Talks with the villager it's interacting with when close: gossip passes both ways. */
    static final class GossipTask extends Task<EntityVillager116> {
        GossipTask() {
            super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryModuleStatus.VALUE_PRESENT));
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            EntityLivingBase target = villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
            return target instanceof EntityVillager116 && target.getDistanceSq(villager) <= 5.0D;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            EntityVillager116 other = (EntityVillager116) villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
            BrainUtil.lookApproachEachOther(villager, other, 0.5F);
            villager.gossipWith(other, gameTime);
        }
    }

    /** Throws spare food (or a farmer's spare wheat) to the villager it's talking to, so that one can breed too. */
    static final class ShareFoodTask extends Task<EntityVillager116> {
        ShareFoodTask() {
            super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryModuleStatus.VALUE_PRESENT));
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            EntityLivingBase target = villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
            return target instanceof EntityVillager116 && target.getDistanceSq(villager) <= 5.0D && villager.hasFoodToShare()
                    && !((EntityVillager116) target).hasFoodToShare();
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            EntityVillager116 other = (EntityVillager116) villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
            InventoryBasic inventory = villager.getVillagerInventory();
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                Integer value = EntityVillager116.FOOD_VALUES.get(stack.getItem());
                if (value == null || stack.getCount() < 2) continue;
                int give = stack.getCount() / 2;
                ItemStack thrown = stack.splitStack(give);
                BrainUtil.spawnItemNearEntity(villager, thrown, other.getPositionVector());
                return;
            }
        }
    }

    /**
     * 1.14's {@code GiveHeroGiftsTask}: a villager seeing a player with Hero of the Village walks up and throws them a gift
     * from its profession's loot table ({@code gameplay/hero_of_the_village/<profession>_gift}; a child throws a poppy,
     * one without a trade wheat seeds), then waits 30 seconds to five and a half minutes before the next.
     */
    static final class GiveHeroGiftsTask extends Task<EntityVillager116> {
        private static final java.util.Set<String> GIFTED = com.google.common.collect.ImmutableSet.of("armorer", "butcher", "cartographer", "cleric",
                "farmer", "fisherman", "fletcher", "leatherworker", "librarian", "mason", "shepherd", "toolsmith", "weaponsmith");
        private int cooldown = 600;
        private boolean done;
        private long startTime;

        GiveHeroGiftsTask() {
            super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED,
                    MemoryModuleType.INTERACTION_TARGET, MemoryModuleStatus.REGISTERED, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleStatus.VALUE_PRESENT), 100);
        }

        private static Optional<EntityPlayer> hero(EntityVillager116 villager) {
            Optional<EntityPlayer> player = villager.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
            return player.filter(p -> com.michaelsebero.portium.init.PortiumPotions.HERO_OF_THE_VILLAGE.getRegistryName() != null
                    && p.isPotionActive(com.michaelsebero.portium.init.PortiumPotions.HERO_OF_THE_VILLAGE));
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            if (!hero(villager).isPresent()) return false;
            if (this.cooldown > 0) {
                --this.cooldown;
                return false;
            }
            return true;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            this.done = false;
            this.startTime = gameTime;
            BrainUtil.setTargetEntity(villager, hero(villager).get(), 0.5F, 5);
        }

        @Override
        protected boolean shouldContinueExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            return hero(villager).isPresent() && !this.done;
        }

        @Override
        protected void updateTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            EntityPlayer player = hero(villager).get();
            BrainUtil.lookAt(villager, player);
            if (villager.getDistanceSq(player) <= 25.0D) {
                if (gameTime - this.startTime > 20L) {
                    for (ItemStack gift : gifts(world, villager)) BrainUtil.spawnItemNearEntity(villager, gift, player.getPositionVector());
                    this.done = true;
                }
            } else {
                BrainUtil.setTargetEntity(villager, player, 0.5F, 5);
            }
        }

        @Override
        protected void resetTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            this.cooldown = 600 + world.rand.nextInt(6001);
            villager.getBrain().removeMemory(MemoryModuleType.INTERACTION_TARGET);
            villager.getBrain().removeMemory(MemoryModuleType.WALK_TARGET);
            villager.getBrain().removeMemory(MemoryModuleType.LOOK_TARGET);
        }

        private static List<ItemStack> gifts(WorldServer world, EntityVillager116 villager) {
            if (villager.isChild()) return java.util.Collections.singletonList(new ItemStack(Item.getItemFromBlock(Blocks.RED_FLOWER)));
            String profession = villager.getVillagerData().getProfession().getName();
            if (!GIFTED.contains(profession)) return java.util.Collections.singletonList(new ItemStack(Items.WHEAT_SEEDS));
            net.minecraft.world.storage.loot.LootTable table = world.getLootTableManager().getLootTableFromLocation(
                    new net.minecraft.util.ResourceLocation("gameplay/hero_of_the_village/" + profession + "_gift"));
            com.michaelsebero.portium.loot.LootOrigin.set(new BlockPos(villager));
            try {
                return table.generateLootForPools(world.rand, new net.minecraft.world.storage.loot.LootContext.Builder(world).withLootedEntity(villager).build());
            } finally {
                com.michaelsebero.portium.loot.LootOrigin.set(null);
            }
        }
    }

    /** Two well-fed adults with a free bed nearby make a child (1.14's CreateBabyVillagerTask). */
    static final class BreedTask extends Task<EntityVillager116> {
        private long breedAt;

        BreedTask() {
            super(ImmutableMap.of(MemoryModuleType.BREED_TARGET, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS,
                    MemoryModuleStatus.VALUE_PRESENT), 350, 350);
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            EntityLivingBase target = villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).orElse(null);
            return target instanceof EntityVillager116 && villager.canBreed() && ((EntityVillager116) target).canBreed() && hasFreeBed(world, villager);
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            EntityVillager116 other = (EntityVillager116) villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
            BrainUtil.lookApproachEachOther(villager, other, 0.5F);
            world.setEntityState(villager, (byte) 18);
            world.setEntityState(other, (byte) 18);
            this.breedAt = gameTime + 40L + villager.getRNG().nextInt(50);
        }

        @Override
        protected boolean shouldContinueExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            Optional<?> target = villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET);
            return target.isPresent() && ((EntityVillager116) target.get()).isEntityAlive() && villager.canBreed();
        }

        @Override
        protected void updateTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            EntityVillager116 other = (EntityVillager116) villager.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
            if (villager.getDistanceSq(other) > 5.0D) return;
            BrainUtil.lookApproachEachOther(villager, other, 0.5F);
            if (gameTime >= this.breedAt) {
                villager.eatAndBreed();
                other.eatAndBreed();
                EntityVillager116 child = (EntityVillager116) villager.createChild(other);
                if (child != null) {
                    villager.setGrowingAge(6000);
                    other.setGrowingAge(6000);
                    child.setLocationAndAngles(villager.posX, villager.posY, villager.posZ, 0.0F, 0.0F);
                    world.spawnEntity(child);
                    world.setEntityState(child, (byte) 12);
                }
                villager.getBrain().removeMemory(MemoryModuleType.BREED_TARGET);
                other.getBrain().removeMemory(MemoryModuleType.BREED_TARGET);
            }
        }

        @Override
        protected void resetTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            villager.getBrain().removeMemory(MemoryModuleType.BREED_TARGET);
        }

        private static boolean hasFreeBed(WorldServer world, EntityVillager116 villager) {
            return pois(world).findNearest(type -> type == PoiType116.HOME, new BlockPos(villager), 48, true).isPresent();
        }
    }

    // ---- Work ----

    /** At its job site: works there (its sound), restocks trades when they're used up, twice a day. */
    static final class WorkAtPoiTask extends Task<EntityVillager116> {
        private long nextWork;

        WorkAtPoiTask() {
            super(ImmutableMap.of(MemoryModuleType.JOB_SITE, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.LOOK_TARGET, MemoryModuleStatus.REGISTERED));
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            if (world.getTotalWorldTime() < this.nextWork) return false;
            GlobalPos site = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
            return sameWorld(world, site) && site.getPos().distanceSqToCenter(villager.posX, villager.posY, villager.posZ) <= 4.0D;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            this.nextWork = gameTime + 300L;
            BlockPos site = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).get().getPos();
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosWrapper(site));
            villager.getBrain().setMemory(MemoryModuleType.LAST_WORKED_AT_POI, gameTime);
            villager.playWorkSound();
            if (villager.canRestock()) villager.restock();
            if (villager.getVillagerData().getProfession() == VillagerProfession116.FARMER) compost(world, villager, site);
        }

        /** A farmer puts spare seeds in its composter (1.16's FarmerWorkTask), keeping ten of each for planting. */
        private static void compost(WorldServer world, EntityVillager116 villager, BlockPos site) {
            InventoryBasic inventory = villager.getVillagerInventory();
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if ((stack.getItem() == Items.WHEAT_SEEDS || stack.getItem() == Items.BEETROOT_SEEDS) && stack.getCount() > 10) {
                    int extra = Math.min(stack.getCount() - 10, 8);
                    for (int n = 0; n < extra; n++) {
                        if (!com.michaelsebero.portium.block.workstation.BlockComposter.villagerCompost(world, site)) return;
                        stack.shrink(1);
                    }
                }
            }
        }
    }

    /** A farmer harvests ripe crops on farmland near its job site and plants seeds on bare farmland. */
    static final class FarmTask extends Task<EntityVillager116> {
        private BlockPos target;
        private long nextAction;

        FarmTask() {
            super(ImmutableMap.of(MemoryModuleType.SECONDARY_JOB_SITE, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.WALK_TARGET,
                    MemoryModuleStatus.VALUE_ABSENT), 200, 200);
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            if (villager.getVillagerData().getProfession() != VillagerProfession116.FARMER
                    || !net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(world, villager)) {
                return false;
            }
            List<GlobalPos> farmland = villager.getBrain().getMemory(MemoryModuleType.SECONDARY_JOB_SITE).orElse(Collections.emptyList());
            List<BlockPos> work = new java.util.ArrayList<>();
            for (GlobalPos pos : farmland) {
                BlockPos crop = pos.getPos().up();
                if (needsWork(world, villager, crop)) work.add(crop);
            }
            if (work.isEmpty()) return false;
            this.target = work.get(villager.getRNG().nextInt(work.size()));
            return true;
        }

        private static boolean needsWork(WorldServer world, EntityVillager116 villager, BlockPos crop) {
            IBlockState state = world.getBlockState(crop);
            Block block = state.getBlock();
            if (block instanceof BlockCrops) return ((BlockCrops) block).isMaxAge(state);
            return state.getBlock().isAir(state, world, crop) && hasSeeds(villager);
        }

        private static boolean hasSeeds(EntityVillager116 villager) {
            InventoryBasic inventory = villager.getVillagerInventory();
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                Item item = inventory.getStackInSlot(i).getItem();
                if (item == Items.WHEAT_SEEDS || item == Items.POTATO || item == Items.CARROT || item == Items.BEETROOT_SEEDS) return true;
            }
            return false;
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosWrapper(this.target));
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new BlockPosWrapper(this.target), 0.5F, 1));
            this.nextAction = gameTime + 40L;
        }

        @Override
        protected boolean shouldContinueExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            return this.target != null;
        }

        @Override
        protected void updateTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            if (this.target == null || gameTime < this.nextAction) return;
            if (this.target.distanceSqToCenter(villager.posX, villager.posY + 0.5D, villager.posZ) > 2.0D) {
                if (!villager.getBrain().hasMemory(MemoryModuleType.WALK_TARGET)) this.target = null;
                return;
            }
            IBlockState state = world.getBlockState(this.target);
            if (state.getBlock() instanceof BlockCrops && ((BlockCrops) state.getBlock()).isMaxAge(state)) {
                world.destroyBlock(this.target, true);
                state = world.getBlockState(this.target);
            }
            if (state.getBlock().isAir(state, world, this.target) && world.getBlockState(this.target.down()).getBlock() == Blocks.FARMLAND) plant(world, villager);
            this.nextAction = gameTime + 20L;
            this.target = null;
        }

        private void plant(WorldServer world, EntityVillager116 villager) {
            InventoryBasic inventory = villager.getVillagerInventory();
            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                Block crop = stack.getItem() == Items.WHEAT_SEEDS ? Blocks.WHEAT : stack.getItem() == Items.POTATO ? Blocks.POTATOES
                        : stack.getItem() == Items.CARROT ? Blocks.CARROTS : stack.getItem() == Items.BEETROOT_SEEDS ? Blocks.BEETROOTS : null;
                if (crop == null) continue;
                world.setBlockState(this.target, crop.getDefaultState(), 3);
                world.playSound(null, this.target, net.minecraft.init.SoundEvents.ITEM_HOE_TILL, net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
                stack.shrink(1);
                return;
            }
        }

        @Override
        protected void resetTask(WorldServer world, EntityVillager116 villager, long gameTime) {
            villager.getBrain().removeMemory(MemoryModuleType.LOOK_TARGET);
            this.target = null;
        }
    }

    // ---- Rest ----

    /** Sleeps in its bed at night once beside it, if no one else is in it. */
    static final class SleepTask extends Task<EntityVillager116> {
        SleepTask() {
            super(ImmutableMap.of(MemoryModuleType.HOME, MemoryModuleStatus.VALUE_PRESENT, MemoryModuleType.LAST_WOKEN, MemoryModuleStatus.REGISTERED));
        }

        @Override
        protected boolean shouldExecute(WorldServer world, EntityVillager116 villager) {
            if (villager.isRiding() || villager.isSleeping()) return false;
            Optional<Long> woken = villager.getBrain().getMemory(MemoryModuleType.LAST_WOKEN);
            if (woken.isPresent() && world.getTotalWorldTime() - woken.get() < 100L) return false;
            GlobalPos home = villager.getBrain().getMemory(MemoryModuleType.HOME).get();
            if (!sameWorld(world, home) || home.getPos().distanceSqToCenter(villager.posX, villager.posY, villager.posZ) > 4.0D) return false;
            IBlockState state = world.getBlockState(home.getPos());
            return state.getBlock() instanceof BlockBed && !state.getValue(BlockBed.OCCUPIED);
        }

        @Override
        protected void startExecuting(WorldServer world, EntityVillager116 villager, long gameTime) {
            villager.startSleeping(villager.getBrain().getMemory(MemoryModuleType.HOME).get().getPos());
        }
    }

    // ---- Shared helpers ----

    /** Particles of a villager's mood, sent as 1.12's entity states (12 angry... 14 happy, 13 angry, 18 hearts). */
    static void particles(WorldServer world, EntityVillager116 villager, EnumParticleTypes type) {
        Vec3d at = villager.getPositionVector().add(0.0D, villager.height + 0.5D, 0.0D);
        world.spawnParticle(type, at.x, at.y, at.z, 5, 0.3D, 0.3D, 0.3D, 0.0D);
    }

    /** Items dropped near a villager for it to pick up. */
    static boolean isWanted(EntityVillager116 villager, EntityItem item) {
        return villager.wantsToPickUp(item.getItem());
    }

    private VillagerTasks116() {}
}
