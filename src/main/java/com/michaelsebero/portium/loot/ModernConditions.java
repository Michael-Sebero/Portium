package com.michaelsebero.portium.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.michaelsebero.portium.compat.LegacyBiomes;
import com.michaelsebero.portium.mixin.accessor.LootContextAccessor;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;

import java.util.Random;

/**
 * Loot conditions 1.13 to 1.16 added that their vanilla tables use: {@code alternative}, {@code inverted},
 * {@code location_check} (where the loot is made: a container, a mob, a fishing bobber; see {@link LootOrigin}),
 * {@code damage_source_properties} (with 1.13's {@code is_lightning}), {@code weather_check} and
 * {@code survives_explosion} (1.12's loot has no explosions, so it always passes).
 */
public final class ModernConditions {

    public static void register() {
        LootConditionManager.registerCondition(new Serializer<Alternative>("alternative", Alternative.class) {
            @Override
            public Alternative deserialize(JsonObject json, JsonDeserializationContext context) {
                return new Alternative(JsonUtils.deserializeClass(json, "terms", context, LootCondition[].class));
            }
        });
        LootConditionManager.registerCondition(new Serializer<Inverted>("inverted", Inverted.class) {
            @Override
            public Inverted deserialize(JsonObject json, JsonDeserializationContext context) {
                return new Inverted(JsonUtils.deserializeClass(json, "term", context, LootCondition.class));
            }
        });
        LootConditionManager.registerCondition(new Serializer<LocationCheck>("location_check", LocationCheck.class) {
            @Override
            public LocationCheck deserialize(JsonObject json, JsonDeserializationContext context) {
                JsonObject predicate = copy(JsonUtils.getJsonObject(json, "predicate"));
                // 1.13+ biome names as 1.12's (modified_jungle is mutated_jungle).
                if (predicate.has("biome")) {
                    String biome = LegacyBiomes.toLegacy(JsonUtils.getString(predicate, "biome"));
                    // A biome this game hasn't got (one of Portium's switched off) is never where the loot is made.
                    if (!net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES.containsKey(new ResourceLocation(biome))) {
                        return new LocationCheck(null, 0, 0, 0);
                    }
                    predicate.addProperty("biome", biome);
                }
                return new LocationCheck(LocationPredicate.deserialize(predicate), JsonUtils.getInt(json, "offsetX", 0),
                        JsonUtils.getInt(json, "offsetY", 0), JsonUtils.getInt(json, "offsetZ", 0));
            }
        });
        LootConditionManager.registerCondition(new Serializer<DamageSourceCheck>("damage_source_properties", DamageSourceCheck.class) {
            @Override
            public DamageSourceCheck deserialize(JsonObject json, JsonDeserializationContext context) {
                JsonObject predicate = json.has("predicate") ? copy(JsonUtils.getJsonObject(json, "predicate")) : new JsonObject();
                Boolean lightning = predicate.has("is_lightning") ? JsonUtils.getBoolean(predicate, "is_lightning") : null;
                predicate.remove("is_lightning");
                return new DamageSourceCheck(DamageSourcePredicate.deserialize(predicate), lightning);
            }
        });
        LootConditionManager.registerCondition(new Serializer<WeatherCheck>("weather_check", WeatherCheck.class) {
            @Override
            public WeatherCheck deserialize(JsonObject json, JsonDeserializationContext context) {
                return new WeatherCheck(json.has("raining") ? JsonUtils.getBoolean(json, "raining") : null,
                        json.has("thundering") ? JsonUtils.getBoolean(json, "thundering") : null);
            }
        });
        LootConditionManager.registerCondition(new Serializer<SurvivesExplosion>("survives_explosion", SurvivesExplosion.class) {
            @Override
            public SurvivesExplosion deserialize(JsonObject json, JsonDeserializationContext context) {
                return new SurvivesExplosion();
            }
        });
    }

    private static JsonObject copy(JsonObject json) {
        return new com.google.gson.JsonParser().parse(json.toString()).getAsJsonObject();
    }

    /** Reading only: Portium never writes these conditions back out. */
    private abstract static class Serializer<T extends LootCondition> extends LootCondition.Serializer<T> {

        Serializer(String name, Class<T> type) {
            super(new ResourceLocation(name), type);
        }

        @Override
        public void serialize(JsonObject json, T value, JsonSerializationContext context) {}
    }

    static final class Alternative implements LootCondition {
        private final LootCondition[] terms;

        Alternative(LootCondition[] terms) {
            this.terms = terms;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            for (LootCondition term : this.terms) {
                if (term.testCondition(rand, context)) return true;
            }
            return false;
        }
    }

    static final class Inverted implements LootCondition {
        private final LootCondition term;

        Inverted(LootCondition term) {
            this.term = term;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            return !this.term.testCondition(rand, context);
        }
    }

    static final class LocationCheck implements LootCondition {
        private final LocationPredicate predicate;
        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;

        LocationCheck(LocationPredicate predicate, int offsetX, int offsetY, int offsetZ) {
            this.predicate = predicate;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            BlockPos origin = LootOrigin.of(context);
            if (origin == null || this.predicate == null) return false;
            return this.predicate.test(context.getWorld(), origin.getX() + this.offsetX + 0.5F, origin.getY() + this.offsetY + 0.5F,
                    origin.getZ() + this.offsetZ + 0.5F);
        }
    }

    static final class DamageSourceCheck implements LootCondition {
        private final DamageSourcePredicate predicate;
        private final Boolean lightning;

        DamageSourceCheck(DamageSourcePredicate predicate, Boolean lightning) {
            this.predicate = predicate;
            this.lightning = lightning;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            DamageSource source = ((LootContextAccessor) context).portium$getDamageSource();
            if (source == null) return false;
            if (this.lightning != null && this.lightning != DamageSource.LIGHTNING_BOLT.getDamageType().equals(source.getDamageType())) return false;
            if (this.predicate == DamageSourcePredicate.ANY) return true;
            return context.getKillerPlayer() instanceof EntityPlayerMP && this.predicate.test((EntityPlayerMP) context.getKillerPlayer(), source);
        }
    }

    static final class WeatherCheck implements LootCondition {
        private final Boolean raining;
        private final Boolean thundering;

        WeatherCheck(Boolean raining, Boolean thundering) {
            this.raining = raining;
            this.thundering = thundering;
        }

        @Override
        public boolean testCondition(Random rand, LootContext context) {
            if (this.raining != null && this.raining != context.getWorld().isRaining()) return false;
            return this.thundering == null || this.thundering == context.getWorld().isThundering();
        }
    }

    static final class SurvivesExplosion implements LootCondition {
        @Override
        public boolean testCondition(Random rand, LootContext context) {
            return true;
        }
    }

    private ModernConditions() {}
}
