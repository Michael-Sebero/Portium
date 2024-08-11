package com.michaelsebero.portium.datapack;

import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** A named group of values from {@code data/<namespace>/tags/<type>/}, with nested tags already flattened in. */
public final class Tag<T> {

    private final ResourceLocation id;
    private final List<T> values;

    public Tag(ResourceLocation id, List<T> values) {
        this.id = id;
        this.values = Collections.unmodifiableList(values);
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public List<T> getValues() {
        return this.values;
    }

    /** Whether any value in the tag satisfies the test (values are matchers for blocks and items). */
    public boolean anyMatch(Predicate<T> test) {
        for (T value : this.values) {
            if (test.test(value)) return true;
        }
        return false;
    }
}
