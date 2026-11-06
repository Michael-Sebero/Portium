package com.michaelsebero.portium.entity.monster;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.sensor.Sensor;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.monster.piglin.EntityPiglin;
import com.michaelsebero.portium.entity.monster.piglin.PiglinMobsSensor;
import com.michaelsebero.portium.entity.monster.piglin.PiglinTags;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** What hoglins look out for: piglins (and how many), other adult hoglins, and warped fungus or portals to keep away from. */
public class HoglinMobsSensor extends Sensor<EntityHoglin> {

    public static final SensorType<HoglinMobsSensor> TYPE = new SensorType<>("hoglin_specific_sensor", HoglinMobsSensor::new);

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN,
                MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT);
    }

    @Override
    protected void update(WorldServer world, EntityHoglin hoglin) {
        Brain<EntityHoglin> brain = hoglin.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_REPELLENT, findNearestRepellent(world, hoglin));
        Optional<EntityLivingBase> piglin = Optional.empty();
        int piglins = 0;
        List<EntityLivingBase> hoglins = new ArrayList<>();
        for (EntityLivingBase mob : brain.getMemory(MemoryModuleType.VISIBLE_MOBS).orElse(Collections.emptyList())) {
            if (mob instanceof EntityPiglin && !mob.isChild()) {
                ++piglins;
                if (!piglin.isPresent()) piglin = Optional.of(mob);
            }
            if (mob instanceof EntityHoglin && !mob.isChild()) hoglins.add(mob);
        }
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, piglin);
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, hoglins);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, piglins);
        brain.setMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, hoglins.size());
    }

    private static Optional<BlockPos> findNearestRepellent(WorldServer world, EntityHoglin hoglin) {
        return PiglinMobsSensor.closestMatching(new BlockPos(hoglin), 8, 4, pos -> PiglinTags.isHoglinRepellent(world.getBlockState(pos)));
    }
}
