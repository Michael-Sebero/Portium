package com.michaelsebero.portium.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Newer mods' loot functions and conditions, as 1.12's loot tables use them: each type a mod registers (1.16's
 * {@code LootFunctionType} and {@code LootConditionType}) gets a 1.12 serializer of its name, which reads the JSON
 * with the mod's own serializer and wraps what it makes. 1.12 checks a function's conditions itself, so the mod's
 * serializer is given the JSON without them.
 */
public final class ModernLootTypes {

    private static final Logger LOGGER = LogManager.getLogger("Portium/Loot");

    private ModernLootTypes() {}

    private static JsonObject withoutConditions(JsonObject json) {
        if (!json.has("conditions")) return json;
        JsonObject copy = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) if (!"conditions".equals(entry.getKey())) copy.add(entry.getKey(), entry.getValue());
        return copy;
    }

    /**
     * A mod's loot function type.
     *
     * @param key    an object unique to the type (its serializer): 1.12 keeps one serializer per class
     * @param reader the mod's serializer reading a function from JSON; the function applies as a
     *               {@code BiFunction<ItemStack, LootContext, ItemStack>}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void function(ResourceLocation id, Object key, BiFunction<JsonObject, JsonDeserializationContext, Object> reader) {
        if ("minecraft".equals(id.getNamespace())) return;
        try {
            LootFunctionManager.registerFunction(new LootFunction.Serializer(id, key.getClass()) {
                @Override
                public void serialize(JsonObject json, LootFunction function, JsonSerializationContext context) {
                    // 1.12 only writes loot tables in its own tools; a newer mod's function has nothing to add.
                }

                @Override
                public LootFunction deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
                    return new Function(conditions, (BiFunction<ItemStack, LootContext, ItemStack>) reader.apply(withoutConditions(json), context));
                }
            });
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Loot function {} is already registered", id, e);
        }
    }

    /** A mod's loot condition type; the condition tests as a {@code Predicate<LootContext>}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void condition(ResourceLocation id, Object key, BiFunction<JsonObject, JsonDeserializationContext, Object> reader) {
        if ("minecraft".equals(id.getNamespace())) return;
        try {
            LootConditionManager.registerCondition(new LootCondition.Serializer(id, key.getClass()) {
                @Override
                public void serialize(JsonObject json, LootCondition condition, JsonSerializationContext context) {}

                @Override
                public LootCondition deserialize(JsonObject json, JsonDeserializationContext context) {
                    return new Condition((Predicate<LootContext>) reader.apply(json, context));
                }
            });
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Loot condition {} is already registered", id, e);
        }
    }

    /** A newer mod's loot function in a 1.12 loot table. */
    public static final class Function extends LootFunction {

        private final BiFunction<ItemStack, LootContext, ItemStack> modern;

        Function(LootCondition[] conditions, BiFunction<ItemStack, LootContext, ItemStack> modern) {
            super(conditions);
            this.modern = modern;
        }

        @Override
        public ItemStack apply(ItemStack stack, Random random, LootContext context) {
            Random outer = RANDOM.get();
            RANDOM.set(random);
            try {
                ItemStack result = this.modern.apply(stack, context);
                return result == null ? stack : result;
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("1.16.5 mod problem: loot function {} failed; the item is left as it was", this.modern.getClass().getName(), e);
                return stack;
            } finally {
                RANDOM.set(outer);
            }
        }
    }

    /** A newer mod's loot condition in a 1.12 loot table. */
    public static final class Condition implements LootCondition {

        private final Predicate<LootContext> modern;

        public Condition(Predicate<LootContext> modern) {
            this.modern = modern;
        }

        @Override
        public boolean testCondition(Random random, LootContext context) {
            Random outer = RANDOM.get();
            RANDOM.set(random);
            try {
                return this.modern.test(context);
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("1.16.5 mod problem: loot condition {} failed; it counts as not met", this.modern.getClass().getName(), e);
                return false;
            } finally {
                RANDOM.set(outer);
            }
        }
    }

    // 1.16's loot API for mods building loot in code (LootTableLoadEvent: LootPool.builder().addEntry(...)...build()).
    // 1.16's entries, conditions and functions stay 1.16's own; a 1.16 pool becomes a 1.12 pool of one entry that
    // makes 1.16's own pick among them (see the glue's LootGlue).

    /** The random of the 1.12 loot call under way (1.16's loot context has one; 1.12's passes it alongside). */
    private static final ThreadLocal<Random> RANDOM = new ThreadLocal<>();
    private static final java.util.concurrent.atomic.AtomicInteger POOLS = new java.util.concurrent.atomic.AtomicInteger();

    public static Random random() {
        Random random = RANDOM.get();
        return random != null ? random : new Random();
    }

    /**
     * A 1.16 pool: rolls of {@code roll} (1.16's choice among the pool's entries, functions applied), when all of the
     * pool's conditions hold.
     */
    @SuppressWarnings("unchecked")
    public static Object pool(java.util.function.BiConsumer<java.util.function.Consumer<ItemStack>, Object> roll, Predicate<Object> conditions,
            Object rolls, Object bonusRolls, @javax.annotation.Nullable String name) {
        String entryName = "portium_116_entries";
        net.minecraft.world.storage.loot.LootEntry entry = new net.minecraft.world.storage.loot.LootEntry(1, 0, new LootCondition[0], entryName) {
            @Override
            public void addLoot(java.util.Collection<ItemStack> stacks, Random random, LootContext context) {
                Random outer = RANDOM.get();
                RANDOM.set(random);
                try {
                    roll.accept(stacks::add, context);
                } catch (RuntimeException | LinkageError e) {
                    LOGGER.error("1.16.5 mod problem: a mod's loot pool failed; it adds nothing", e);
                } finally {
                    RANDOM.set(outer);
                }
            }

            @Override
            protected void serialize(JsonObject json, JsonSerializationContext context) {}
        };
        LootCondition condition = new Condition(context -> conditions.test(context));
        return new net.minecraft.world.storage.loot.LootPool(new net.minecraft.world.storage.loot.LootEntry[] {entry}, new LootCondition[] {condition},
                (net.minecraft.world.storage.loot.RandomValueRange) rolls, (net.minecraft.world.storage.loot.RandomValueRange) bonusRolls,
                name != null ? name : "portium_116_pool_" + POOLS.incrementAndGet());
    }

    public static float luck(Object context) {
        return ((LootContext) context).getLuck();
    }

    public static Object world(Object context) {
        return ((LootContext) context).getWorld();
    }

    @javax.annotation.Nullable
    private static java.lang.reflect.Field damageSource;

    /** 1.16's loot parameters, by name, from what 1.12's loot context knows (no block, tool or explosion: 1.12 drops blocks without loot tables). */
    @javax.annotation.Nullable
    public static Object parameter(Object context, String name) {
        LootContext loot = (LootContext) context;
        switch (name) {
            case "this_entity":
                return loot.getLootedEntity();
            case "last_damage_player":
                return loot.getKillerPlayer();
            case "killer_entity":
                return loot.getKiller();
            case "direct_killer_entity": {
                net.minecraft.util.DamageSource source = (net.minecraft.util.DamageSource) parameter(context, "damage_source");
                return source != null && source.getImmediateSource() != null ? source.getImmediateSource() : loot.getKiller();
            }
            case "damage_source":
                try {
                    if (damageSource == null) {
                        for (java.lang.reflect.Field field : LootContext.class.getDeclaredFields()) {
                            if (field.getType() == net.minecraft.util.DamageSource.class) {
                                field.setAccessible(true);
                                damageSource = field;
                            }
                        }
                    }
                    return damageSource == null ? null : damageSource.get(loot);
                } catch (IllegalAccessException e) {
                    return null;
                }
            case "origin": {
                net.minecraft.entity.Entity entity = loot.getLootedEntity();
                return entity == null ? null : entity.getPositionVector();
            }
            default:
                return null;
        }
    }

    // 1.12's own loot functions and conditions for 1.16's builders of them (SetCount.builder(range)...): 1.12 has
    // these classes, so 1.16 code knows them as 1.12's; they're applied from 1.16 wrappers (glue).

    public static ItemStack applyLegacy(Object function, Object stack, Object context) {
        return ((LootFunction) function).apply((ItemStack) stack, random(), (LootContext) context);
    }

    public static boolean testLegacy(Object condition, Object context) {
        return ((LootCondition) condition).testCondition(random(), (LootContext) context);
    }

    private static final LootCondition[] NONE = new LootCondition[0];

    public static Object setCount(Object range) {
        return new net.minecraft.world.storage.loot.functions.SetCount(NONE, (net.minecraft.world.storage.loot.RandomValueRange) range);
    }

    public static Object lootingBonus(Object range) {
        return new net.minecraft.world.storage.loot.functions.LootingEnchantBonus(NONE, (net.minecraft.world.storage.loot.RandomValueRange) range, 0);
    }

    public static Object setNbt(Object tag) {
        return new net.minecraft.world.storage.loot.functions.SetNBT(NONE, (net.minecraft.nbt.NBTTagCompound) tag);
    }

    public static Object enchantRandomly() {
        return new net.minecraft.world.storage.loot.functions.EnchantRandomly(NONE, null);
    }

    public static Object enchantWithLevels(Object range) {
        return new net.minecraft.world.storage.loot.functions.EnchantWithLevels(NONE, (net.minecraft.world.storage.loot.RandomValueRange) range, false);
    }

    public static Object setDamage(Object range) {
        return new net.minecraft.world.storage.loot.functions.SetDamage(NONE, (net.minecraft.world.storage.loot.RandomValueRange) range);
    }

    public static Object smelt() {
        return new net.minecraft.world.storage.loot.functions.Smelt(NONE);
    }

    public static Object randomChance(float chance) {
        return new net.minecraft.world.storage.loot.conditions.RandomChance(chance);
    }

    public static Object randomChanceWithLooting(float chance, float lootingMultiplier) {
        return new net.minecraft.world.storage.loot.conditions.RandomChanceWithLooting(chance, lootingMultiplier);
    }

    /** 1.16's {@code ConstantRange.of(n)}. */
    public static net.minecraft.world.storage.loot.RandomValueRange constant(int value) {
        return new net.minecraft.world.storage.loot.RandomValueRange(value);
    }

    /** 1.16's {@code BinomialRange.of(n, p)}: n tries of chance p. */
    public static net.minecraft.world.storage.loot.RandomValueRange binomial(int tries, float chance) {
        return new net.minecraft.world.storage.loot.RandomValueRange(0, tries) {
            @Override
            public int generateInt(Random random) {
                int hits = 0;
                for (int i = 0; i < tries; i++) if (random.nextFloat() < chance) hits++;
                return hits;
            }

            @Override
            public float generateFloat(Random random) {
                return generateInt(random);
            }
        };
    }

    @javax.annotation.Nullable
    public static Object lootTable(Object context, ResourceLocation id) {
        return ((LootContext) context).getLootTableManager().getLootTableFromLocation(id);
    }

    /** A 1.12 table's loot, for 1.16 code asking a table (a 1.16 table entry naming another table). */
    public static java.util.List<ItemStack> generate(Object table, Object context) {
        return ((net.minecraft.world.storage.loot.LootTable) table).generateLootForPools(random(), (LootContext) context);
    }
}
