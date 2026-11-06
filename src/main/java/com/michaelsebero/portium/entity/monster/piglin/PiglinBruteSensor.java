package com.michaelsebero.portium.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.sensor.Sensor;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 1.16's {@code PiglinBruteSpecificSensor}: withers and wither skeletons seen, and adult piglins about. */
public class PiglinBruteSensor extends Sensor<EntityLivingBase> {

    public static final SensorType<PiglinBruteSensor> TYPE = new SensorType<>("piglin_brute_specific_sensor", PiglinBruteSensor::new);

    @Override
    public Set<MemoryModuleType<?>> getUsedMemories() {
        return ImmutableSet.of(MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.NEAREST_ADULT_PIGLINS);
    }

    @Override
    protected void update(WorldServer world, EntityLivingBase entity) {
        Brain<?> brain = BrainUtil.brain(entity);
        Optional<EntityLiving> nemesis = Optional.empty();
        for (EntityLivingBase mob : brain.getMemory(MemoryModuleType.VISIBLE_MOBS).orElse(ImmutableList.of())) {
            if (mob instanceof EntityWitherSkeleton || mob instanceof EntityWither) {
                nemesis = Optional.of((EntityLiving) mob);
                break;
            }
        }
        List<EntityLivingBase> adults = new ArrayList<>();
        for (EntityLivingBase mob : brain.getMemory(MemoryModuleType.MOBS).orElse(ImmutableList.of())) {
            if (mob instanceof EntityAbstractPiglin && ((EntityAbstractPiglin) mob).isAdult()) adults.add(mob);
        }
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS, nemesis);
        brain.setMemory(MemoryModuleType.NEAREST_ADULT_PIGLINS, adults);
    }
}
