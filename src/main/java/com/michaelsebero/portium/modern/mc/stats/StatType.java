package com.michaelsebero.portium.modern.mc.stats;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.stats.StatBase;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * 1.13's {@code net.minecraft.stats.StatType}: a kind of statistic kept per block, item or entity. Its statistics are
 * 1.12's (1.13's {@code Stat} is 1.12's {@code StatBase}).
 */
public class StatType<T> extends com.michaelsebero.portium.modern.RegistryEntryStandIn<StatType<?>> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public final Class<StatType<?>> getRegistryType() {
        return (Class) StatType.class;
    }


    private final Function<T, StatBase> stats;

    public StatType(Function<T, StatBase> stats) {
        this.stats = stats;
    }

    /**
     * 1.16's: a statistic for each entry of a registry (its custom statistics are names in one). Made when first
     * asked for, as a plain 1.12 statistic named after the entry.
     */
    public StatType(com.michaelsebero.portium.modern.mc.util.registry.IRegistry registry) {
        java.util.Map<Object, StatBase> made = new java.util.concurrent.ConcurrentHashMap<>();
        this.stats = object -> object == null ? null : made.computeIfAbsent(object, key -> {
            String name = String.valueOf(key).replace(':', '.');
            StatBase stat = new net.minecraft.stats.StatBasic("stat." + name, new net.minecraft.util.text.TextComponentTranslation("stat." + name));
            try {
                stat.registerStat();
            } catch (RuntimeException e) {
                // Already one by that name: the statistic still counts for the object asked.
            }
            return stat;
        });
    }

    /** 1.16's {@code get(object, formatter)}: 1.12 formats statistics by their own kind. */
    public static StatBase withFormatter(StatType<Object> type, Object object, Object formatter) {
        return type.get(object);
    }

    /** The statistic for an object, or null if 1.12 keeps none for it. */
    @Nullable
    @Srg("func_199076_b")
    public StatBase get(T object) {
        return this.stats.apply(object);
    }
}
