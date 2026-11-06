package com.michaelsebero.portium.entity.villager;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.Brain;
import com.michaelsebero.portium.entity.ai.brain.BrainUtil;
import com.michaelsebero.portium.entity.ai.brain.memory.GlobalPos;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.sensor.Sensor;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import net.minecraft.block.BlockFarmland;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** 1.14's villager sensors. */
public final class VillagerSensors116 {

    public static final SensorType<Hostiles> HOSTILES = new SensorType<>("villager_hostiles", Hostiles::new);
    public static final SensorType<Babies> BABIES = new SensorType<>("villager_babies", Babies::new);
    public static final SensorType<SecondaryPois> SECONDARY_POIS = new SensorType<>("secondary_pois", SecondaryPois::new);
    public static final SensorType<GolemSeen> GOLEM = new SensorType<>("golem_last_seen", GolemSeen::new);

    /** The nearest seen hostile within its own scare distance: 1.16's VillagerHostilesSensor. */
    public static final class Hostiles extends Sensor<EntityLivingBase> {

        private static final Map<String, Float> DISTANCES = new HashMap<>();

        static {
            DISTANCES.put("drowned", 8.0F);
            DISTANCES.put("evocation_illager", 12.0F);
            DISTANCES.put("evoker", 12.0F);
            DISTANCES.put("husk", 8.0F);
            DISTANCES.put("illusion_illager", 12.0F);
            DISTANCES.put("illusioner", 12.0F);
            DISTANCES.put("pillager", 15.0F);
            DISTANCES.put("ravager", 12.0F);
            DISTANCES.put("vex", 8.0F);
            DISTANCES.put("vindication_illager", 10.0F);
            DISTANCES.put("vindicator", 10.0F);
            DISTANCES.put("zoglin", 10.0F);
            DISTANCES.put("zombie", 8.0F);
            DISTANCES.put("zombie_villager", 8.0F);
        }

        static float distance(EntityLivingBase entity) {
            ResourceLocation id = EntityList.getKey(entity);
            if (id == null) return -1.0F;
            Float distance = DISTANCES.get(id.getPath());
            return distance == null ? -1.0F : distance;
        }

        @Override
        protected void update(WorldServer world, EntityLivingBase entity) {
            Brain<?> brain = BrainUtil.brain(entity);
            Optional<EntityLivingBase> nearest = brain.getMemory(MemoryModuleType.VISIBLE_MOBS).orElse(Collections.emptyList()).stream()
                    .filter(other -> {
                        float distance = distance(other);
                        return distance > 0 && other.getDistanceSq(entity) <= distance * distance;
                    }).findFirst();
            brain.setMemory(MemoryModuleType.NEAREST_HOSTILE, nearest);
        }

        @Override
        public Set<MemoryModuleType<?>> getUsedMemories() {
            return ImmutableSet.of(MemoryModuleType.NEAREST_HOSTILE);
        }
    }

    /** Villager children in sight. */
    public static final class Babies extends Sensor<EntityLivingBase> {

        @Override
        protected void update(WorldServer world, EntityLivingBase entity) {
            Brain<?> brain = BrainUtil.brain(entity);
            List<EntityLivingBase> babies = brain.getMemory(MemoryModuleType.VISIBLE_MOBS).orElse(Collections.emptyList()).stream()
                    .filter(other -> other instanceof EntityVillager116 && other.isChild()).collect(Collectors.toList());
            brain.setMemory(MemoryModuleType.VISIBLE_VILLAGER_BABIES, babies);
        }

        @Override
        public Set<MemoryModuleType<?>> getUsedMemories() {
            return ImmutableSet.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES);
        }
    }

    /** A farmer's farmland near its job site, which it tends while working. */
    public static final class SecondaryPois extends Sensor<EntityVillager116> {

        public SecondaryPois() {
            super(40);
        }

        @Override
        protected void update(WorldServer world, EntityVillager116 villager) {
            Brain<?> brain = villager.getBrain();
            if (villager.getVillagerData().getProfession() != VillagerProfession116.FARMER) {
                brain.removeMemory(MemoryModuleType.SECONDARY_JOB_SITE);
                return;
            }
            BlockPos center = new BlockPos(villager);
            List<GlobalPos> found = new ArrayList<>();
            for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-4, -2, -4), center.add(4, 2, 4))) {
                if (world.getBlockState(pos).getBlock() instanceof BlockFarmland) found.add(GlobalPos.of(world.provider.getDimension(), pos.toImmutable()));
            }
            if (found.isEmpty()) {
                brain.removeMemory(MemoryModuleType.SECONDARY_JOB_SITE);
            } else {
                brain.setMemory(MemoryModuleType.SECONDARY_JOB_SITE, found);
            }
        }

        @Override
        public Set<MemoryModuleType<?>> getUsedMemories() {
            return ImmutableSet.of(MemoryModuleType.SECONDARY_JOB_SITE);
        }
    }

    /** Remembers for 30 seconds having seen an iron golem (villagers then don't summon another). */
    public static final class GolemSeen extends Sensor<EntityLivingBase> {

        public GolemSeen() {
            super(200);
        }

        @Override
        protected void update(WorldServer world, EntityLivingBase entity) {
            Brain<?> brain = BrainUtil.brain(entity);
            boolean seen = brain.getMemory(MemoryModuleType.MOBS).orElse(Collections.emptyList()).stream()
                    .anyMatch(other -> other instanceof EntityIronGolem);
            if (seen) brain.replaceMemory(MemoryModuleType.GOLEM_DETECTED_RECENTLY, true, 600L);
        }

        @Override
        public Set<MemoryModuleType<?>> getUsedMemories() {
            return ImmutableSet.of(MemoryModuleType.GOLEM_DETECTED_RECENTLY);
        }
    }

    private VillagerSensors116() {}
}
