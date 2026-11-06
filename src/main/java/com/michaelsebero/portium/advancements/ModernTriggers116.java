package com.michaelsebero.portium.advancements;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The advancement triggers 1.14 to 1.16 added, for 1.16.5's advancements: crossbow shots and kills, raids won, items
 * used on blocks, honey slides, bee nests broken, target hits, bastion loot, piglins picking up thrown gold, and
 * players interacting with mobs. Their conditions are read in 1.12's predicate forms (Portium's data pack loader
 * rewrites 1.16's composite entity conditions into those).
 */
public final class ModernTriggers116 {

    public static final Simple SHOT_CROSSBOW = CriteriaTriggers.register(new Simple("shot_crossbow", "item"));
    public static final Simple HERO_OF_THE_VILLAGE = CriteriaTriggers.register(new Simple("hero_of_the_village"));
    public static final Simple SLIDE_DOWN_BLOCK = CriteriaTriggers.register(new Simple("slide_down_block", "block"));
    public static final Simple ITEM_USED_ON_BLOCK = CriteriaTriggers.register(new Simple("item_used_on_block", "location", "item"));
    public static final Simple TARGET_HIT = CriteriaTriggers.register(new Simple("target_hit", "signal_strength", "projectile", "shooter"));
    public static final Simple BEE_NEST_DESTROYED = CriteriaTriggers.register(new Simple("bee_nest_destroyed", "block", "item", "num_bees_inside"));
    public static final Simple CONTAINER_LOOT = CriteriaTriggers.register(new Simple("player_generates_container_loot", "loot_table"));
    public static final Simple THROWN_ITEM_PICKED_UP = CriteriaTriggers.register(new Simple("thrown_item_picked_up_by_entity", "item", "entity", "player"));
    public static final Simple INTERACTED_WITH_ENTITY = CriteriaTriggers.register(new Simple("player_interacted_with_entity", "item", "entity", "player"));
    public static final KilledByCrossbow KILLED_BY_CROSSBOW = CriteriaTriggers.register(new KilledByCrossbow());

    /** Loads the class (registering the triggers). */
    public static void init() {}

    /** What a criterion asks of an event: any of these left out passes. */
    public static final class Conditions extends AbstractCriterionInstance {
        ItemPredicate item = ItemPredicate.ANY;
        EntityPredicate entity = EntityPredicate.ANY;
        EntityPredicate player = EntityPredicate.ANY;
        EntityPredicate projectile = EntityPredicate.ANY;
        EntityPredicate shooter = EntityPredicate.ANY;
        LocationPredicate location = LocationPredicate.ANY;
        MinMaxBounds signal = MinMaxBounds.UNBOUNDED;
        MinMaxBounds bees = MinMaxBounds.UNBOUNDED;
        @Nullable
        String block;
        @Nullable
        String lootTable;

        Conditions(ResourceLocation id) {
            super(id);
        }
    }

    /** A trigger that reads whichever of the usual condition fields it names, and is fired with what happened. */
    public static final class Simple extends PortiumTrigger<Conditions> {

        Simple(String name, String... fields) {
            super(new ResourceLocation(name));
        }

        @Override
        public Conditions deserializeInstance(JsonObject json, JsonDeserializationContext context) {
            Conditions conditions = new Conditions(this.getId());
            if (json.has("item")) conditions.item = ItemPredicate.deserialize(json.get("item"));
            if (json.has("entity")) conditions.entity = EntityPredicate.deserialize(json.get("entity"));
            if (json.has("player")) conditions.player = EntityPredicate.deserialize(json.get("player"));
            if (json.has("projectile")) conditions.projectile = EntityPredicate.deserialize(json.get("projectile"));
            if (json.has("shooter")) conditions.shooter = EntityPredicate.deserialize(json.get("shooter"));
            if (json.has("location")) conditions.location = LocationPredicate.deserialize(json.get("location"));
            if (json.has("signal_strength")) conditions.signal = MinMaxBounds.deserialize(json.get("signal_strength"));
            if (json.has("num_bees_inside")) conditions.bees = MinMaxBounds.deserialize(json.get("num_bees_inside"));
            if (json.has("block")) conditions.block = new ResourceLocation(JsonUtils.getString(json, "block")).toString();
            if (json.has("loot_table")) conditions.lootTable = new ResourceLocation(JsonUtils.getString(json, "loot_table")).toString();
            return conditions;
        }

        /** Fires with what the event involved; nulls stand for "nothing of that kind" and pass any condition on it. */
        public void trigger(EntityPlayerMP player, @Nullable ItemStack item, @Nullable Entity entity, @Nullable BlockPos pos, @Nullable Block block,
                            float number, @Nullable String lootTable) {
            WorldServer world = player.getServerWorld();
            this.trigger(player, conditions -> (item == null || conditions.item.test(item))
                    && (entity == null || conditions.entity.test(player, entity))
                    && conditions.player.test(player, player) && conditions.shooter.test(player, player)
                    && (entity == null || conditions.projectile.test(player, entity))
                    && (pos == null || conditions.location.test(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D))
                    && (block == null || conditions.block == null || conditions.block.equals(String.valueOf(block.getRegistryName())))
                    && conditions.signal.test(number) && conditions.bees.test(number)
                    && (conditions.lootTable == null || conditions.lootTable.equals(lootTable)));
        }

        public void trigger(EntityPlayerMP player) {
            this.trigger(player, null, null, null, null, 0.0F, null);
        }
    }

    /** {@code killed_by_crossbow}: what one crossbow shot has killed, by number of kinds and by each victim. */
    public static final class KilledByCrossbow extends PortiumTrigger<KilledByCrossbow.Instance> {

        KilledByCrossbow() {
            super(new ResourceLocation("killed_by_crossbow"));
        }

        @Override
        public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
            Instance instance = new Instance(this.getId());
            if (json.has("unique_entity_types")) instance.uniqueTypes = MinMaxBounds.deserialize(json.get("unique_entity_types"));
            if (json.has("victims")) {
                JsonElement victims = json.get("victims");
                if (victims.isJsonArray()) {
                    for (JsonElement victim : (JsonArray) victims) instance.victims.add(EntityPredicate.deserialize(victim));
                }
            }
            return instance;
        }

        public void trigger(EntityPlayerMP player, Collection<Entity> victims) {
            Set<String> types = new HashSet<>();
            for (Entity victim : victims) types.add(String.valueOf(net.minecraft.entity.EntityList.getKey(victim)));
            this.trigger(player, instance -> {
                if (!instance.uniqueTypes.test(types.size())) return false;
                List<Entity> left = new ArrayList<>(victims);
                for (EntityPredicate predicate : instance.victims) {
                    Entity match = null;
                    for (Entity victim : left) {
                        if (predicate.test(player, victim)) {
                            match = victim;
                            break;
                        }
                    }
                    if (match == null) return false;
                    left.remove(match);
                }
                return true;
            });
        }

        public static final class Instance extends AbstractCriterionInstance {
            MinMaxBounds uniqueTypes = MinMaxBounds.UNBOUNDED;
            final List<EntityPredicate> victims = new ArrayList<>();

            Instance(ResourceLocation id) {
                super(id);
            }
        }
    }

    private ModernTriggers116() {}
}
