package com.michaelsebero.portium.entity.ai.brain.memory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1.14-1.16's memory types, by their 1.16.5 names. Those with a serializer are saved with the entity, as in 1.16.5.
 * Mob-specific memories (piglins' hoglins and so on) hold their 1.12 base types; the tasks using them cast.
 */
public final class MemoryModuleType<U> {

    private static final Map<String, MemoryModuleType<?>> REGISTRY = new LinkedHashMap<>();

    public static final MemoryModuleType<Void> DUMMY = register("dummy");
    public static final MemoryModuleType<GlobalPos> HOME = register("home", MemorySerializer.GLOBAL_POS);
    public static final MemoryModuleType<GlobalPos> JOB_SITE = register("job_site", MemorySerializer.GLOBAL_POS);
    public static final MemoryModuleType<GlobalPos> POTENTIAL_JOB_SITE = register("potential_job_site", MemorySerializer.GLOBAL_POS);
    public static final MemoryModuleType<GlobalPos> MEETING_POINT = register("meeting_point", MemorySerializer.GLOBAL_POS);
    public static final MemoryModuleType<List<GlobalPos>> SECONDARY_JOB_SITE = register("secondary_job_site");
    public static final MemoryModuleType<List<EntityLivingBase>> MOBS = register("mobs");
    public static final MemoryModuleType<List<EntityLivingBase>> VISIBLE_MOBS = register("visible_mobs");
    public static final MemoryModuleType<List<EntityLivingBase>> VISIBLE_VILLAGER_BABIES = register("visible_villager_babies");
    public static final MemoryModuleType<List<EntityPlayer>> NEAREST_PLAYERS = register("nearest_players");
    public static final MemoryModuleType<EntityPlayer> NEAREST_VISIBLE_PLAYER = register("nearest_visible_player");
    public static final MemoryModuleType<EntityPlayer> NEAREST_VISIBLE_TARGETABLE_PLAYER = register("nearest_visible_targetable_player");
    public static final MemoryModuleType<WalkTarget> WALK_TARGET = register("walk_target");
    public static final MemoryModuleType<PosWrapper> LOOK_TARGET = register("look_target");
    public static final MemoryModuleType<EntityLivingBase> ATTACK_TARGET = register("attack_target");
    public static final MemoryModuleType<Boolean> ATTACK_COOLING_DOWN = register("attack_cooling_down");
    public static final MemoryModuleType<EntityLivingBase> INTERACTION_TARGET = register("interaction_target");
    public static final MemoryModuleType<EntityAgeable> BREED_TARGET = register("breed_target");
    public static final MemoryModuleType<Entity> RIDE_TARGET = register("ride_target");
    public static final MemoryModuleType<Path> PATH = register("path");
    public static final MemoryModuleType<List<GlobalPos>> INTERACTABLE_DOORS = register("interactable_doors");
    public static final MemoryModuleType<Set<GlobalPos>> OPENED_DOORS = register("doors_to_close");
    public static final MemoryModuleType<BlockPos> NEAREST_BED = register("nearest_bed");
    public static final MemoryModuleType<DamageSource> HURT_BY = register("hurt_by");
    public static final MemoryModuleType<EntityLivingBase> HURT_BY_ENTITY = register("hurt_by_entity");
    public static final MemoryModuleType<EntityLivingBase> AVOID_TARGET = register("avoid_target");
    public static final MemoryModuleType<EntityLivingBase> NEAREST_HOSTILE = register("nearest_hostile");
    public static final MemoryModuleType<GlobalPos> HIDING_PLACE = register("hiding_place");
    public static final MemoryModuleType<Long> HEARD_BELL_TIME = register("heard_bell_time");
    public static final MemoryModuleType<Long> CANT_REACH_WALK_TARGET_SINCE = register("cant_reach_walk_target_since");
    public static final MemoryModuleType<Boolean> GOLEM_DETECTED_RECENTLY = register("golem_detected_recently", MemorySerializer.BOOL);
    public static final MemoryModuleType<Long> LAST_SLEPT = register("last_slept", MemorySerializer.LONG);
    public static final MemoryModuleType<Long> LAST_WOKEN = register("last_woken", MemorySerializer.LONG);
    public static final MemoryModuleType<Long> LAST_WORKED_AT_POI = register("last_worked_at_poi", MemorySerializer.LONG);
    public static final MemoryModuleType<EntityAgeable> NEAREST_VISIBLE_ADULT = register("nearest_visible_adult");
    public static final MemoryModuleType<EntityItem> NEAREST_VISIBLE_WANTED_ITEM = register("nearest_visible_wanted_item");
    public static final MemoryModuleType<EntityLiving> NEAREST_VISIBLE_NEMESIS = register("nearest_visible_nemesis");
    public static final MemoryModuleType<UUID> ANGRY_AT = register("angry_at", MemorySerializer.UUID_ARRAY);
    public static final MemoryModuleType<Boolean> UNIVERSAL_ANGER = register("universal_anger", MemorySerializer.BOOL);
    public static final MemoryModuleType<Boolean> ADMIRING_ITEM = register("admiring_item", MemorySerializer.BOOL);
    public static final MemoryModuleType<Integer> TIME_TRYING_TO_REACH_ADMIRE_ITEM = register("time_trying_to_reach_admire_item");
    public static final MemoryModuleType<Boolean> DISABLE_WALK_TO_ADMIRE_ITEM = register("disable_walk_to_admire_item");
    public static final MemoryModuleType<Boolean> ADMIRING_DISABLED = register("admiring_disabled", MemorySerializer.BOOL);
    public static final MemoryModuleType<Boolean> HUNTED_RECENTLY = register("hunted_recently", MemorySerializer.BOOL);
    public static final MemoryModuleType<BlockPos> CELEBRATE_LOCATION = register("celebrate_location");
    public static final MemoryModuleType<Boolean> DANCING = register("dancing");
    public static final MemoryModuleType<EntityLivingBase> NEAREST_VISIBLE_HUNTABLE_HOGLIN = register("nearest_visible_huntable_hoglin");
    public static final MemoryModuleType<EntityLivingBase> NEAREST_VISIBLE_BABY_HOGLIN = register("nearest_visible_baby_hoglin");
    public static final MemoryModuleType<EntityPlayer> NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD = register("nearest_targetable_player_not_wearing_gold");
    public static final MemoryModuleType<List<EntityLivingBase>> NEAREST_ADULT_PIGLINS = register("nearby_adult_piglins");
    public static final MemoryModuleType<List<EntityLivingBase>> NEAREST_VISIBLE_ADULT_PIGLINS = register("nearest_visible_adult_piglins");
    public static final MemoryModuleType<List<EntityLivingBase>> NEAREST_VISIBLE_ADULT_HOGLINS = register("nearest_visible_adult_hoglins");
    public static final MemoryModuleType<EntityLivingBase> NEAREST_VISIBLE_ADULT_PIGLIN = register("nearest_visible_adult_piglin");
    public static final MemoryModuleType<EntityLivingBase> NEAREST_VISIBLE_ZOMBIFIED = register("nearest_visible_zombified");
    public static final MemoryModuleType<Integer> VISIBLE_ADULT_PIGLIN_COUNT = register("visible_adult_piglin_count");
    public static final MemoryModuleType<Integer> VISIBLE_ADULT_HOGLIN_COUNT = register("visible_adult_hoglin_count");
    public static final MemoryModuleType<EntityPlayer> NEAREST_PLAYER_HOLDING_WANTED_ITEM = register("nearest_player_holding_wanted_item");
    public static final MemoryModuleType<Boolean> ATE_RECENTLY = register("ate_recently");
    public static final MemoryModuleType<BlockPos> NEAREST_REPELLENT = register("nearest_repellent");
    public static final MemoryModuleType<Boolean> PACIFIED = register("pacified");

    private final String name;
    @Nullable
    private final MemorySerializer<U> serializer;

    private MemoryModuleType(String name, @Nullable MemorySerializer<U> serializer) {
        this.name = name;
        this.serializer = serializer;
    }

    private static <U> MemoryModuleType<U> register(String name) {
        return register(name, null);
    }

    private static <U> MemoryModuleType<U> register(String name, @Nullable MemorySerializer<U> serializer) {
        MemoryModuleType<U> type = new MemoryModuleType<>("minecraft:" + name, serializer);
        REGISTRY.put(type.name, type);
        return type;
    }

    @Nullable
    public static MemoryModuleType<?> byName(String name) {
        return REGISTRY.get(name.indexOf(':') < 0 ? "minecraft:" + name : name);
    }

    public static Map<String, MemoryModuleType<?>> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public MemorySerializer<U> getSerializer() {
        return this.serializer;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
