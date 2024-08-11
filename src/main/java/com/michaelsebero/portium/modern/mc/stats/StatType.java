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

    /** The statistic for an object, or null if 1.12 keeps none for it. */
    @Nullable
    @Srg("func_199076_b")
    public StatBase get(T object) {
        return this.stats.apply(object);
    }
}
