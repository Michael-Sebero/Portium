package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.TargetPredicate;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.sensor.Sensor;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.monster.EntityHoglin;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What piglins look out for: withers and wither skeletons (nemeses), hoglins to hunt (and babies to ride), other
 * piglins, players with no gold on or holding gold, zombified mobs, and soul fire within 8 blocks.
 */
public class PiglinMobsSensor extends Sensor<EntityLivingBase> {

    public static final SensorType<PiglinMobsSensor> TYPE = new SensorType<>("piglin_specific_sensor", PiglinMobsSensor::new);

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.MOBS, MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
                MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM,
                MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
                MemoryModuleType.NEAREST_ADULT_PIGLINS, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
                MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED);
    }

    @Override
    protected void update(WorldServer world, EntityLivingBase entity) {
        Brain<?> brain = BrainUtil.brain(entity);
        brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, findNearestRepellent(world, entity));
        Optional<EntityLiving> nemesis = Optional.empty();
        Optional<EntityLivingBase> huntable = Optional.empty();
        Optional<EntityLivingBase> babyHoglin = Optional.empty();
        Optional<EntityLivingBase> babyPiglin = Optional.empty();
        Optional<EntityLivingBase> zombified = Optional.empty();
        Optional<EntityPlayer> noGold = Optional.empty();
        Optional<EntityPlayer> holdingLoved = Optional.empty();
        int adultHoglins = 0;
        List<EntityLivingBase> visibleAdultPiglins = new ArrayList<>();
        List<EntityLivingBase> adultPiglins = new ArrayList<>();
        for (EntityLivingBase mob : brain.getMemory(MemoryModuleType.VISIBLE_MOBS).orElse(ImmutableList.of())) {
            if (mob instanceof EntityHoglin) {
                EntityHoglin hoglin = (EntityHoglin) mob;
                if (hoglin.isChild() && !babyHoglin.isPresent()) {
                    babyHoglin = Optional.of(hoglin);
                } else if (hoglin.isAdult()) {
                    ++adultHoglins;
                    if (!huntable.isPresent() && hoglin.canBeHunted()) huntable = Optional.of(hoglin);
                }
            } else if (mob instanceof EntityAbstractPiglin && !(mob instanceof EntityPiglin)) {
                visibleAdultPiglins.add(mob);
            } else if (mob instanceof EntityPiglin) {
                EntityPiglin piglin = (EntityPiglin) mob;
                if (piglin.isChild() && !babyPiglin.isPresent()) {
                    babyPiglin = Optional.of(piglin);
                } else if (piglin.isAdult()) {
                    visibleAdultPiglins.add(piglin);
                }
            } else if (mob instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) mob;
                if (!noGold.isPresent() && TargetPredicate.canHostileTarget(mob) && !PiglinTasks.isWearingGold(player)) noGold = Optional.of(player);
                if (!holdingLoved.isPresent() && !player.isSpectator() && PiglinTasks.isPlayerHoldingLovedItem(player)) holdingLoved = Optional.of(player);
            } else if (!nemesis.isPresent() && (mob instanceof EntityWitherSkeleton || mob instanceof EntityWither)) {
                nemesis = Optional.of((EntityLiving) mob);
            } else if (!zombified.isPresent() && PiglinTasks.isZombified(mob)) {
                zombified = Optional.of(mob);
            }
        }
        for (EntityLivingBase mob : brain.getMemory(MemoryModuleType.MOBS).orElse(ImmutableList.of())) {
            if (mob instanceof EntityAbstractPiglin && ((EntityAbstractPiglin) mob).isAdult()) adultPiglins.add(mob);
        }
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, nemesis);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN, huntable);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN, babyHoglin);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, zombified);
        brain.setMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, noGold);
        brain.setMemory(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM, holdingLoved);
        brain.setMemory(MemoryModuleType.NEAREST_ADULT_PIGLINS, adultPiglins);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS, visibleAdultPiglins);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, visibleAdultPiglins.size());
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, adultHoglins);
    }

    /** 1.16's {@code BlockPos.getClosestMatchingPosition}: the nearest soul fire (lit soul campfire...) within 8 x 4 x 8. */
    static Optional<BlockPos> findNearestRepellent(WorldServer world, EntityLivingBase entity) {
        return closestMatching(new BlockPos(entity), 8, 4, pos -> PiglinTags.isPiglinRepellent(world.getBlockState(pos)));
    }

    /** Searches outward shell by shell (by Manhattan-like rings, as 1.16 does) for the first matching position. */
    public static Optional<BlockPos> closestMatching(BlockPos centre, int horizontal, int vertical, java.util.function.Predicate<BlockPos> test) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ring = 0; ring <= Math.max(horizontal, vertical); ++ring) {
            int ry = Math.min(ring, vertical);
            int rxz = Math.min(ring, horizontal);
            for (int dy = 0; dy <= ry; dy = dy > 0 ? -dy : 1 - dy) {
                for (int dx = 0; dx <= rxz; dx = dx > 0 ? -dx : 1 - dx) {
                    for (int dz = 0; dz <= rxz; dz = dz > 0 ? -dz : 1 - dz) {
                        if (Math.max(Math.max(Math.abs(dx), Math.abs(dz)), Math.abs(dy)) != ring) continue;
                        pos.setPos(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                        if (test.test(pos)) return Optional.of(pos.toImmutable());
                    }
                    if (rxz == 0) break;
                }
                if (ry == 0) break;
            }
        }
        return Optional.empty();
    }
}
