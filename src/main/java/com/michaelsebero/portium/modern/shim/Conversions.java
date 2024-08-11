package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.adapter.ModernBlocks;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShape;
import com.michaelsebero.portium.modern.mc.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * Values 1.13 passes where 1.12 has another type for the same thing, converted. The translator inserts these where
 * a call or override reaches 1.12's version of a 1.13 method (see {@code ModernLinker}).
 */
public final class Conversions {

    /** 1.13 takes Java's functional interfaces where 1.12 took Guava's, which extend them. */
    public static com.google.common.base.Predicate<Object> guavaPredicate(java.util.function.Predicate<Object> predicate) {
        return predicate == null ? null : predicate::test;
    }

    public static com.google.common.base.Function<Object, Object> guavaFunction(java.util.function.Function<Object, Object> function) {
        return function == null ? null : function::apply;
    }

    public static com.google.common.base.Supplier<Object> guavaSupplier(java.util.function.Supplier<Object> supplier) {
        return supplier == null ? null : supplier::get;
    }

    /** A 1.13 enum constant 1.12 has under another name (light gray, once silver): by name, else by ordinal. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Enum<?> enumConstant(Class<?> type, String name, int ordinal) {
        Enum<?>[] values = ((Class<? extends Enum>) type).getEnumConstants();
        for (Enum<?> value : values) {
            if (value.name().equals(name)) return value;
        }
        return ordinal < values.length ? values[ordinal] : values[0];
    }

    /** 1.13 names things with text components where 1.12 took their text. */
    public static String text(net.minecraft.util.text.ITextComponent text) {
        return text == null ? null : text.getUnformattedText();
    }

    /** Always true: 1.12's "has a custom name" for a name 1.13 code gave. */
    public static boolean named(Object text) {
        return true;
    }

    /** 1.12's simple registries, where 1.13 has maps (the baked models of {@code ModelBakeEvent}). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static java.util.Map<Object, Object> registryMap(net.minecraft.util.registry.IRegistry registry) {
        return registry == null ? null : new RegistryMap<>(registry);
    }

    public static AxisAlignedBB bounds(VoxelShape shape) {
        return ModernBlocks.bounds(shape);
    }

    public static VoxelShape shape(AxisAlignedBB box) {
        return box == null ? VoxelShapes.empty() : VoxelShapes.create(box);
    }

    private static final ThreadLocal<java.util.Random> RANDOM = ThreadLocal.withInitial(java.util.Random::new);

    /** A random seeded as 1.12 seeds what 1.16 hands a random for (a baked model's quads); one per thread, reseeded. */
    public static java.util.Random random(long seed) {
        java.util.Random random = RANDOM.get();
        random.setSeed(seed);
        return random;
    }

    private Conversions() {}
}
