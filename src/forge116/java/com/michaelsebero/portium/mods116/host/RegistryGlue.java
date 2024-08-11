package com.michaelsebero.portium.mods116.host;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Stream;

/**
 * 1.16's registries are codecs of what they hold, by name. Portium's registries (its stand-in, which 1.16's registry
 * classes become) can't be: DataFixerUpper 4 is the mods' library, not 1.12's. The translator turns a codec method
 * called on a registry into an {@code invokedynamic} bootstrapped here, which calls it on a codec made for that
 * registry as 1.16 makes its own.
 */
public final class RegistryGlue {

    private static final Map<Object, Codec<Object>> CODECS = Collections.synchronizedMap(new WeakHashMap<>());

    /** Bootstraps {@code registry.name(arguments)}, a method of {@code Codec} (or {@code Keyable}), on the registry's codec. */
    public static CallSite codecCall(MethodHandles.Lookup lookup, String name, MethodType type) throws ReflectiveOperationException {
        MethodType call = type.dropParameterTypes(0, 1);
        MethodHandle target;
        MethodHandle convert;
        try {
            target = MethodHandles.publicLookup().findVirtual(Codec.class, name, call);
            convert = MethodHandles.lookup().findStatic(RegistryGlue.class, "codec", MethodType.methodType(Codec.class, Object.class));
        } catch (NoSuchMethodException e) {
            target = MethodHandles.publicLookup().findVirtual(Keyable.class, name, call);
            convert = MethodHandles.lookup().findStatic(RegistryGlue.class, "keyable", MethodType.methodType(Keyable.class, Object.class));
        }
        return new ConstantCallSite(MethodHandles.filterArguments(target, 0, convert).asType(type));
    }

    /** The registry's codec: its values by registry name. */
    @SuppressWarnings("unchecked")
    public static Codec<Object> codec(Object registry) {
        return CODECS.computeIfAbsent(registry, key -> make((Registry<Object>) key));
    }

    private static Codec<Object> make(Registry<Object> registry) {
        Codec<ResourceLocation> names = Codec.STRING.comapFlatMap(RegistryGlue::location, ResourceLocation::toString);
        return names.flatXmap(name -> {
            Optional<Object> value = registry.func_241873_b(name);
            return value.map(DataResult::success).orElseGet(() -> DataResult.error("Unknown registry key: " + name));
        }, value -> {
            ResourceLocation key = registry.func_177774_c(value);
            return key == null ? DataResult.error("Unknown registry element " + value) : DataResult.success(key);
        });
    }

    private static DataResult<ResourceLocation> location(String text) {
        try {
            return DataResult.success(new ResourceLocation(text));
        } catch (RuntimeException e) {
            return DataResult.error("Not a valid resource location: " + text + " " + e.getMessage());
        }
    }

    /** The registry's names, as {@code Keyable}. */
    @SuppressWarnings("unchecked")
    public static Keyable keyable(Object registry) {
        Registry<Object> values = (Registry<Object>) registry;
        return new Keyable() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return values.func_148742_b().stream().map(name -> ops.createString(name.toString()));
            }
        };
    }

    private RegistryGlue() {}
}
