package com.michaelsebero.portium.mods116.host;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.Property;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 1.16's state codec ({@code StateHolder.codec}): a state as its owner's name and its property values by name, as
 * 1.16 writes them. 1.16's own reads fields of its state classes, which Portium's states (1.12's block states and its
 * fluid states) don't have.
 */
public final class StateGlue {

    @SuppressWarnings("unchecked")
    public static <O, S> Codec<S> codec(Codec<O> owners, Function<O, S> defaultState) {
        Codec<Map<String, String>> values = Codec.unboundedMap(Codec.STRING, Codec.STRING);
        return owners.dispatch("Name", state -> (O) owner(state), owner -> {
            S state = defaultState.apply(owner);
            return values.optionalFieldOf("Properties", Collections.emptyMap()).xmap(names -> (S) with(state, names), StateGlue::names).codec();
        });
    }

    private static Object owner(Object state) {
        return state instanceof BlockState ? ((BlockState) state).func_177230_c() : ((FluidState) state).func_206886_c();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<Property<?>, Comparable<?>> values(Object state) {
        return state instanceof BlockState ? ((BlockState) state).func_206871_b() : ((FluidState) state).func_206871_b();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, String> names(Object state) {
        Map<String, String> names = new HashMap<>();
        for (Map.Entry<Property<?>, Comparable<?>> value : values(state).entrySet()) {
            Property property = value.getKey();
            names.put(property.func_177701_a(), property.func_177702_a(value.getValue()));
        }
        return names;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object with(Object state, Map<String, String> names) {
        Object result = state;
        for (Property property : values(state).keySet()) {
            String name = names.get(property.func_177701_a());
            if (name == null) continue;
            for (Object value : property.func_177700_c()) {
                if (!name.equals(property.func_177702_a((Comparable) value))) continue;
                result = result instanceof BlockState ? ((BlockState) result).func_206870_a(property, (Comparable) value)
                        : ((FluidState) result).func_206870_a(property, (Comparable) value);
                break;
            }
        }
        return result;
    }

    private StateGlue() {}
}
