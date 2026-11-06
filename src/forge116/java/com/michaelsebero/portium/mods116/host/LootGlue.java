package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.loot.ModernLootTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.ILootGenerator;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.IRandomRange;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootEntry;
import net.minecraft.loot.LootFunction;
import net.minecraft.loot.LootFunctionType;
import net.minecraft.loot.LootParameter;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.RandomValueRange;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.loot.functions.EnchantWithLevels;
import net.minecraft.loot.functions.ILootFunction;
import net.minecraft.loot.functions.LootingEnchantBonus;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * 1.16's loot, for mods: the loot types a mod registers, handed to 1.12's loot managers ({@link ModernLootTypes}),
 * and 1.16's loot builders (a mod adding a pool in {@code LootTableLoadEvent}). 1.16's entries, conditions and
 * functions are 1.16's own; a pool built from them becomes a 1.12 pool whose one entry makes 1.16's pick.
 */
public final class LootGlue {

    static void registered(ResourceLocation id, Object type) {
        if (type instanceof LootFunctionType) {
            ILootSerializer<?> serializer = ((LootFunctionType) type).func_237408_a_();
            ModernLootTypes.function(id, serializer, serializer::func_230423_a_);
        } else if (type instanceof LootConditionType) {
            ILootSerializer<?> serializer = ((LootConditionType) type).func_237408_a_();
            ModernLootTypes.condition(id, serializer, serializer::func_230423_a_);
        }
    }

    // Forge's additions to the pool builder (Minecraft 1.16.5's own lacks them) and the build.

    private static final Map<LootPool.Builder, String> NAMES = Collections.synchronizedMap(new WeakHashMap<>());

    public static LootPool.Builder poolName(LootPool.Builder builder, String name) {
        NAMES.put(builder, name);
        return builder;
    }

    public static LootPool.Builder bonusRolls(LootPool.Builder builder, float min, float max) {
        set(builder, "field_216051_e", new RandomValueRange(min, max));
        return builder;
    }

    @SuppressWarnings("unchecked")
    public static LootPool buildPool(LootPool.Builder builder) {
        List<LootEntry> entries = new ArrayList<>((List<LootEntry>) get(builder, "field_216047_a"));
        List<ILootCondition> conditions = new ArrayList<>((List<ILootCondition>) get(builder, "field_216048_b"));
        List<ILootFunction> functions = new ArrayList<>((List<ILootFunction>) get(builder, "field_216049_c"));
        Object rolls = get(builder, "field_216050_d");
        Object bonusRolls = get(builder, "field_216051_e");
        return (LootPool) ModernLootTypes.pool((out, context) -> roll(entries, functions, out, (LootContext) context), context -> {
            for (ILootCondition condition : conditions) if (!condition.test((LootContext) context)) return false;
            return true;
        }, rolls, bonusRolls, NAMES.get(builder));
    }

    /** 1.16's {@code LootPool.createItemStack}: one weighted pick among what the entries offer. */
    private static void roll(List<LootEntry> entries, List<ILootFunction> functions, Consumer<ItemStack> out, LootContext context) {
        float luck = ModernLootTypes.luck(context);
        List<ILootGenerator> offered = new ArrayList<>();
        int[] total = {0};
        for (LootEntry entry : entries) {
            entry.expand(context, generator -> {
                int weight = generator.func_186361_a(luck);
                if (weight > 0) {
                    offered.add(generator);
                    total[0] += weight;
                }
            });
        }
        if (offered.isEmpty() || total[0] == 0) return;
        Consumer<ItemStack> sink = functions.isEmpty() ? out : stack -> {
            ItemStack result = stack;
            for (ILootFunction function : functions) result = function.apply(result, context);
            out.accept(result);
        };
        if (offered.size() == 1) {
            offered.get(0).func_216188_a(sink, context);
            return;
        }
        int pick = ModernLootTypes.random().nextInt(total[0]);
        for (ILootGenerator generator : offered) {
            pick -= generator.func_186361_a(luck);
            if (pick < 0) {
                generator.func_216188_a(sink, context);
                return;
            }
        }
    }

    private static final java.util.function.BiFunction<ItemStack, LootContext, ItemStack> IDENTITY = (stack, context) -> stack;

    public static java.util.function.BiFunction<ItemStack, LootContext, ItemStack> identity() {
        return IDENTITY;
    }

    /** 1.16's {@code LootFunctionManager.combine(functions)}. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.function.BiFunction compose(java.util.function.BiFunction[] functions) {
        if (functions.length == 0) return IDENTITY;
        if (functions.length == 1) return functions[0];
        java.util.function.BiFunction[] all = functions.clone();
        return (stack, context) -> {
            Object result = stack;
            for (java.util.function.BiFunction function : all) result = function.apply(result, context);
            return result;
        };
    }

    // 1.16's loot context, answered from 1.12's.

    @Nullable
    public static Object param(LootContext context, LootParameter<?> parameter) {
        return ModernLootTypes.parameter(context, parameter.func_216247_a().func_110623_a());
    }

    public static boolean hasParam(LootContext context, LootParameter<?> parameter) {
        return param(context, parameter) != null;
    }

    public static Random random(LootContext context) {
        return ModernLootTypes.random();
    }

    public static ServerWorld world(LootContext context) {
        return (ServerWorld) ModernLootTypes.world(context);
    }

    @Nullable
    public static LootTable lootTable(LootContext context, ResourceLocation id) {
        return (LootTable) ModernLootTypes.lootTable(context, id);
    }

    /** 1.16's {@code lootTable.recursiveGenerate(context, consumer)} on a 1.12 table. */
    public static void generate(LootTable table, LootContext context, Consumer<ItemStack> out) {
        for (Object stack : ModernLootTypes.generate(table, context)) out.accept((ItemStack) stack);
    }

    public static List<ItemStack> generate(LootTable table, LootContext context) {
        List<ItemStack> stacks = new ArrayList<>();
        generate(table, context, stacks::add);
        return stacks;
    }

    // 1.16's builders of loot functions and conditions 1.12 has too (1.16 code knows those classes as 1.12's).

    private static final class Legacy extends LootFunction {

        private final Object legacy;

        Legacy(ILootCondition[] conditions, Object legacy) {
            super(conditions);
            this.legacy = legacy;
        }

        @Override
        protected ItemStack func_215859_a(ItemStack stack, LootContext context) {
            return (ItemStack) (Object) ModernLootTypes.applyLegacy(this.legacy, stack, context);
        }

        @Override
        @Nullable
        public LootFunctionType func_230425_b_() {
            return null;
        }

        static LootFunction.Builder<?> builder(Object legacy) {
            return func_215860_a(conditions -> new Legacy(conditions, legacy));
        }
    }

    public static LootFunction.Builder<?> setCount(IRandomRange range) {
        return Legacy.builder(ModernLootTypes.setCount(range));
    }

    public static LootFunction.Builder<?> setNbt(CompoundNBT tag) {
        return Legacy.builder(ModernLootTypes.setNbt(tag));
    }

    public static LootFunction.Builder<?> enchantRandomly() {
        return Legacy.builder(ModernLootTypes.enchantRandomly());
    }

    public static LootFunction.Builder<?> setDamage(RandomValueRange range) {
        return Legacy.builder(ModernLootTypes.setDamage(range));
    }

    public static LootFunction.Builder<?> smelt() {
        return Legacy.builder(ModernLootTypes.smelt());
    }

    public static LootingEnchantBonus.Builder lootingBonus(RandomValueRange range) {
        return new LootingEnchantBonus.Builder(range) {
            @Override
            public ILootFunction func_216052_b() {
                return new Legacy(func_216053_g(), ModernLootTypes.lootingBonus(range));
            }
        };
    }

    public static EnchantWithLevels.Builder enchantWithLevels(IRandomRange range) {
        return new EnchantWithLevels.Builder(range) {
            @Override
            public ILootFunction func_216052_b() {
                return new Legacy(func_216053_g(), ModernLootTypes.enchantWithLevels(range));
            }
        };
    }

    private static ILootCondition.IBuilder condition(Object legacy) {
        ILootCondition condition = new ILootCondition() {
            @Override
            @Nullable
            public LootConditionType func_230419_b_() {
                return null;
            }

            @Override
            public boolean test(LootContext context) {
                return ModernLootTypes.testLegacy(legacy, context);
            }
        };
        return () -> condition;
    }

    public static ILootCondition.IBuilder randomChance(float chance) {
        return condition(ModernLootTypes.randomChance(chance));
    }

    public static ILootCondition.IBuilder randomChanceWithLooting(float chance, float lootingMultiplier) {
        return condition(ModernLootTypes.randomChanceWithLooting(chance, lootingMultiplier));
    }

    private static final Map<String, Field> FIELDS = new java.util.concurrent.ConcurrentHashMap<>();

    private static Field field(Class<?> type, String name) {
        return FIELDS.computeIfAbsent(type.getName() + "." + name, key -> {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                try {
                    Field field = c.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    // the superclass's
                }
            }
            throw new IllegalStateException("No field " + name + " in " + type.getName());
        });
    }

    private static Object get(Object owner, String name) {
        try {
            return field(owner.getClass(), name).get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object owner, String name, Object value) {
        try {
            field(owner.getClass(), name).set(owner, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private LootGlue() {}
}
