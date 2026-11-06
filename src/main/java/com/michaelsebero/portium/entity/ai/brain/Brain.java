package com.michaelsebero.portium.entity.ai.brain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleStatus;
import com.michaelsebero.portium.entity.ai.brain.memory.MemoryModuleType;
import com.michaelsebero.portium.entity.ai.brain.memory.MemorySerializer;
import com.michaelsebero.portium.entity.ai.brain.schedule.Activity;
import com.michaelsebero.portium.entity.ai.brain.schedule.Schedule;
import com.michaelsebero.portium.entity.ai.brain.sensor.Sensor;
import com.michaelsebero.portium.entity.ai.brain.sensor.SensorType;
import com.michaelsebero.portium.entity.ai.brain.task.Task;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * 1.14-1.16's brain: memories that sensors fill and tasks read, and tasks grouped by priority and activity. Ticked
 * from the mob's {@code updateAITasks}, after 1.12's goal selectors, as 1.16 ticks it from {@code customServerAiStep}.
 * Saved under the entity's {@code Brain.memories} tag as 1.16.5 saves it.
 */
public class Brain<E extends EntityLivingBase> {

    private final Map<MemoryModuleType<?>, Memory<?>> memories = new HashMap<>();
    private final Map<SensorType<?>, Sensor<? super E>> sensors = new LinkedHashMap<>();
    private final Map<Integer, Map<Activity, Set<Task<? super E>>>> taskPriorityMap = new TreeMap<>();
    private Schedule schedule = Schedule.EMPTY;
    private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryModuleStatus>>> requiredMemoryStates = new HashMap<>();
    private final Map<Activity, Set<MemoryModuleType<?>>> memoryMap = new HashMap<>();
    private Set<Activity> persistentActivities = new HashSet<>();
    private final Set<Activity> activities = new HashSet<>();
    private Activity fallbackActivity = Activity.IDLE;
    private long lastGameTime = -9999L;

    @SuppressWarnings("unchecked")
    public Brain(Collection<? extends MemoryModuleType<?>> memoryTypes, Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes) {
        for (MemoryModuleType<?> type : memoryTypes) this.memories.put(type, null);
        for (SensorType<? extends Sensor<? super E>> type : sensorTypes) this.sensors.put(type, type.create());
        for (Sensor<? super E> sensor : this.sensors.values()) {
            for (MemoryModuleType<?> type : sensor.getUsedMemories()) this.memories.put(type, null);
        }
    }

    // ---- Saving ----

    /** The {@code memories} compound: each saved memory as {@code {value, ttl?}}. */
    public NBTTagCompound write() {
        NBTTagCompound memoriesTag = new NBTTagCompound();
        for (Map.Entry<MemoryModuleType<?>, Memory<?>> entry : this.memories.entrySet()) {
            NBTBase tag = writeMemory(entry.getKey(), entry.getValue());
            if (tag != null) memoriesTag.setTag(entry.getKey().getName(), tag);
        }
        NBTTagCompound brainTag = new NBTTagCompound();
        brainTag.setTag("memories", memoriesTag);
        return brainTag;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <U> NBTBase writeMemory(MemoryModuleType<U> type, @Nullable Memory<?> memory) {
        MemorySerializer<U> serializer = type.getSerializer();
        if (serializer == null || memory == null) return null;
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("value", serializer.write((U) memory.getValue()));
        if (memory.isForgettable()) tag.setLong("ttl", memory.getTimeToLive());
        return tag;
    }

    /** Loads the memories saved in a {@code Brain} tag, those this brain has. */
    public void read(NBTTagCompound brainTag) {
        NBTTagCompound memoriesTag = brainTag.getCompoundTag("memories");
        for (String name : memoriesTag.getKeySet()) {
            MemoryModuleType<?> type = MemoryModuleType.byName(name);
            if (type != null && type.getSerializer() != null) readMemory(type, memoriesTag.getCompoundTag(name));
        }
    }

    private <U> void readMemory(MemoryModuleType<U> type, NBTTagCompound tag) {
        U value = type.getSerializer().read(tag.getTag("value"));
        if (value == null) return;
        long ttl = tag.hasKey("ttl", 99) ? tag.getLong("ttl") : Long.MAX_VALUE;
        this.replaceMemory(type, Optional.of(Memory.create(value, ttl)));
    }

    // ---- Memories ----

    public boolean hasMemory(MemoryModuleType<?> type) {
        return this.hasMemory(type, MemoryModuleStatus.VALUE_PRESENT);
    }

    public <U> void removeMemory(MemoryModuleType<U> type) {
        this.setMemory(type, Optional.empty());
    }

    public <U> void setMemory(MemoryModuleType<U> type, @Nullable U value) {
        this.setMemory(type, Optional.ofNullable(value));
    }

    public <U> void replaceMemory(MemoryModuleType<U> type, U value, long timeToLive) {
        this.replaceMemory(type, Optional.of(Memory.create(value, timeToLive)));
    }

    public <U> void setMemory(MemoryModuleType<U> type, Optional<? extends U> value) {
        this.replaceMemory(type, value.map(Memory::create));
    }

    private <U> void replaceMemory(MemoryModuleType<U> type, Optional<? extends Memory<?>> memory) {
        if (!this.memories.containsKey(type)) return;
        if (memory.isPresent() && isEmptyCollection(memory.get().getValue())) {
            this.memories.put(type, null);
        } else {
            this.memories.put(type, memory.orElse(null));
        }
    }

    @SuppressWarnings("unchecked")
    public <U> Optional<U> getMemory(MemoryModuleType<U> type) {
        Memory<?> memory = this.memories.get(type);
        return memory == null ? Optional.empty() : Optional.ofNullable((U) memory.getValue());
    }

    public <U> boolean hasMemory(MemoryModuleType<U> type, U value) {
        return this.hasMemory(type) && this.getMemory(type).filter(present -> present.equals(value)).isPresent();
    }

    public boolean hasMemory(MemoryModuleType<?> type, MemoryModuleStatus status) {
        if (!this.memories.containsKey(type)) return false;
        boolean present = this.memories.get(type) != null;
        return status == MemoryModuleStatus.REGISTERED || status == MemoryModuleStatus.VALUE_PRESENT && present
                || status == MemoryModuleStatus.VALUE_ABSENT && !present;
    }

    private static boolean isEmptyCollection(Object value) {
        return value instanceof Collection && ((Collection<?>) value).isEmpty();
    }

    // ---- Activities ----

    public Schedule getSchedule() {
        return this.schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public void setPersistentActivities(Set<Activity> activities) {
        this.persistentActivities = activities;
    }

    public void setFallbackActivity(Activity activity) {
        this.fallbackActivity = activity;
    }

    public List<Task<? super E>> getRunningTasks() {
        List<Task<? super E>> running = new ArrayList<>();
        for (Map<Activity, Set<Task<? super E>>> byActivity : this.taskPriorityMap.values()) {
            for (Set<Task<? super E>> tasks : byActivity.values()) {
                for (Task<? super E> task : tasks) {
                    if (task.getStatus() == Task.Status.RUNNING) running.add(task);
                }
            }
        }
        return running;
    }

    public void switchToFallbackActivity() {
        this.switchActivity(this.fallbackActivity);
    }

    /** The current activity that isn't one of the always-on ones. */
    public Optional<Activity> getTemporaryActivity() {
        for (Activity activity : this.activities) {
            if (!this.persistentActivities.contains(activity)) return Optional.of(activity);
        }
        return Optional.empty();
    }

    public void switchTo(Activity activity) {
        if (this.hasRequiredMemories(activity)) {
            this.switchActivity(activity);
        } else {
            this.switchToFallbackActivity();
        }
    }

    private void switchActivity(Activity activity) {
        if (this.hasActivity(activity)) return;
        this.removeUnassociatedMemories(activity);
        this.activities.clear();
        this.activities.addAll(this.persistentActivities);
        this.activities.add(activity);
    }

    private void removeUnassociatedMemories(Activity next) {
        for (Activity activity : this.activities) {
            if (activity == next) continue;
            Set<MemoryModuleType<?>> types = this.memoryMap.get(activity);
            if (types != null) types.forEach(this::removeMemory);
        }
    }

    /** Follows the schedule, checked once a second. */
    public void updateActivity(long dayTime, long gameTime) {
        if (gameTime - this.lastGameTime > 20L) {
            this.lastGameTime = gameTime;
            Activity scheduled = this.schedule.getScheduledActivity((int) (dayTime % 24000L));
            if (!this.activities.contains(scheduled)) this.switchTo(scheduled);
        }
    }

    /** Switches to the first of these whose required memories are met. */
    public void switchActivities(List<Activity> candidates) {
        for (Activity activity : candidates) {
            if (this.hasRequiredMemories(activity)) {
                this.switchActivity(activity);
                break;
            }
        }
    }

    public void registerActivity(Activity activity, int firstPriority, ImmutableList<? extends Task<? super E>> tasks) {
        this.registerActivity(activity, priorities(firstPriority, tasks));
    }

    /** An activity that needs {@code memory} present, and that forgets it when left. */
    public void registerActivity(Activity activity, int firstPriority, ImmutableList<? extends Task<? super E>> tasks, MemoryModuleType<?> memory) {
        Set<Pair<MemoryModuleType<?>, MemoryModuleStatus>> required = ImmutableSet.of(Pair.of(memory, MemoryModuleStatus.VALUE_PRESENT));
        Set<MemoryModuleType<?>> forget = ImmutableSet.of(memory);
        this.registerActivity(activity, priorities(firstPriority, tasks), required, forget);
    }

    public void registerActivity(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> tasks) {
        this.registerActivity(activity, tasks, ImmutableSet.of(), new HashSet<>());
    }

    public void registerActivity(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> tasks,
                                 Set<Pair<MemoryModuleType<?>, MemoryModuleStatus>> required) {
        this.registerActivity(activity, tasks, required, new HashSet<>());
    }

    private void registerActivity(Activity activity, ImmutableList<? extends Pair<Integer, ? extends Task<? super E>>> tasks,
                                  Set<Pair<MemoryModuleType<?>, MemoryModuleStatus>> required, Set<MemoryModuleType<?>> forget) {
        this.requiredMemoryStates.put(activity, required);
        if (!forget.isEmpty()) this.memoryMap.put(activity, forget);
        for (Pair<Integer, ? extends Task<? super E>> entry : tasks) {
            this.taskPriorityMap.computeIfAbsent(entry.getLeft(), key -> new HashMap<>())
                    .computeIfAbsent(activity, key -> new LinkedHashSet<>()).add(entry.getRight());
        }
    }

    public boolean hasActivity(Activity activity) {
        return this.activities.contains(activity);
    }

    private boolean hasRequiredMemories(Activity activity) {
        Set<Pair<MemoryModuleType<?>, MemoryModuleStatus>> required = this.requiredMemoryStates.get(activity);
        if (required == null) return false;
        for (Pair<MemoryModuleType<?>, MemoryModuleStatus> pair : required) {
            if (!this.hasMemory(pair.getLeft(), pair.getRight())) return false;
        }
        return true;
    }

    private ImmutableList<Pair<Integer, ? extends Task<? super E>>> priorities(int first, ImmutableList<? extends Task<? super E>> tasks) {
        ImmutableList.Builder<Pair<Integer, ? extends Task<? super E>>> builder = ImmutableList.builder();
        int priority = first;
        for (Task<? super E> task : tasks) builder.add(Pair.of(priority++, task));
        return builder.build();
    }

    // ---- Ticking ----

    public void tick(WorldServer world, E entity) {
        this.tickMemories();
        this.tickSensors(world, entity);
        this.startTasks(world, entity);
        this.tickTasks(world, entity);
    }

    private void tickSensors(WorldServer world, E entity) {
        for (Sensor<? super E> sensor : this.sensors.values()) sensor.tick(world, entity);
    }

    private void tickMemories() {
        for (Iterator<Map.Entry<MemoryModuleType<?>, Memory<?>>> it = this.memories.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<MemoryModuleType<?>, Memory<?>> entry = it.next();
            Memory<?> memory = entry.getValue();
            if (memory == null) continue;
            memory.tick();
            if (memory.isForgotten()) entry.setValue(null);
        }
    }

    public void stopAllTasks(WorldServer world, E entity) {
        long time = world.getTotalWorldTime();
        for (Task<? super E> task : this.getRunningTasks()) task.stop(world, entity, time);
    }

    private void startTasks(WorldServer world, E entity) {
        long time = world.getTotalWorldTime();
        for (Map<Activity, Set<Task<? super E>>> byActivity : this.taskPriorityMap.values()) {
            for (Map.Entry<Activity, Set<Task<? super E>>> entry : byActivity.entrySet()) {
                if (!this.activities.contains(entry.getKey())) continue;
                for (Task<? super E> task : entry.getValue()) {
                    if (task.getStatus() == Task.Status.STOPPED) task.start(world, entity, time);
                }
            }
        }
    }

    private void tickTasks(WorldServer world, E entity) {
        long time = world.getTotalWorldTime();
        for (Task<? super E> task : this.getRunningTasks()) task.tick(world, entity, time);
    }

    /** For debugging: the memories that hold a value. */
    public Map<MemoryModuleType<?>, Memory<?>> presentMemories() {
        Map<MemoryModuleType<?>, Memory<?>> present = new LinkedHashMap<>();
        this.memories.forEach((type, memory) -> {
            if (memory != null) present.put(type, memory);
        });
        return present;
    }

    public Set<Activity> getActivities() {
        return this.activities;
    }
}
