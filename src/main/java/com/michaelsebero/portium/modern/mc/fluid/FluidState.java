package com.michaelsebero.portium.modern.mc.fluid;

import com.google.common.collect.ImmutableMap;
import com.michaelsebero.portium.modern.Srg;
import net.minecraft.block.properties.IProperty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** 1.13's {@code net.minecraft.fluid.FluidState}: a fluid and a value for each of its properties. */
public class FluidState implements IFluidState {

    private final Fluid fluid;
    private final ImmutableMap<IProperty<?>, Comparable<?>> values;
    /** The state with each property set to each value, filled by the fluid once all its states exist. */
    final Map<IProperty<?>, Map<Comparable<?>, FluidState>> neighbours = new HashMap<>();

    public FluidState(Fluid fluid, ImmutableMap<IProperty<?>, Comparable<?>> values) {
        this.fluid = fluid;
        this.values = values;
    }

    @Override
    @Srg("func_206886_c")
    public Fluid getFluid() {
        return this.fluid;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Comparable<T>> T get(IProperty<T> property) {
        Comparable<?> value = this.values.get(property);
        if (value == null) throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.fluid);
        return (T) value;
    }

    @Override
    public <T extends Comparable<T>, V extends T> Object with(IProperty<T> property, V value) {
        Comparable<?> current = this.values.get(property);
        if (current == null) throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.fluid);
        if (current == value) return this;
        FluidState other = this.neighbours.get(property).get(value);
        if (other == null) throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on " + this.fluid + ", it is not an allowed value");
        return other;
    }

    @Override
    public <T extends Comparable<T>> boolean has(IProperty<T> property) {
        return this.values.containsKey(property);
    }

    @Override
    public <T extends Comparable<T>> Object cycle(IProperty<T> property) {
        T current = get(property);
        Iterator<T> values = property.getAllowedValues().iterator();
        while (values.hasNext()) {
            if (values.next().equals(current)) return with(property, values.hasNext() ? values.next() : property.getAllowedValues().iterator().next());
        }
        return this;
    }

    @Override
    public ImmutableMap<IProperty<?>, Comparable<?>> getValues() {
        return this.values;
    }

    @Override
    public String toString() {
        return this.fluid + (this.values.isEmpty() ? "" : this.values.toString());
    }
}
