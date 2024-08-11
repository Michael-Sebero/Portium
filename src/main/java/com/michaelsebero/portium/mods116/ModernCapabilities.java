package com.michaelsebero.portium.mods116;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import sun.misc.Unsafe;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Forge 1.12's capabilities for 1.13 mods: lookups for the glue's {@code CapabilityGlue}, which is compiled against
 * Forge 1.13's {@code getCapability} and can't name 1.12's, and capability injection.
 */
public final class ModernCapabilities {

    private static final Field PROVIDERS = managerField("providers");
    private static final Field CALLBACKS = managerField("callbacks");
    private static final Unsafe UNSAFE = unsafe();

    /** The capability from a provider, or null if it has none. */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Object get(Object provider, Object capability, @Nullable Object side) {
        ICapabilityProvider holder = (ICapabilityProvider) provider;
        Capability cap = (Capability) capability;
        EnumFacing facing = (EnumFacing) side;
        return cap != null && holder.hasCapability(cap, facing) ? holder.getCapability(cap, facing) : null;
    }

    /**
     * For 1.13 mods' {@code @CapabilityInject}: Forge 1.12 fills only its own mods' fields, from its own scan. Adds a
     * callback for when the capability is registered, or fills the field or calls the method now if it already is.
     *
     * @param member a static field's name, or a static method's name followed by its descriptor
     */
    @SuppressWarnings("unchecked")
    public static void inject(String capability, Class<?> owner, String member) {
        Function<Capability<?>, Object> callback = registered -> {
            try {
                int paren = member.indexOf('(');
                if (paren < 0) {
                    Field field = owner.getDeclaredField(member);
                    field.setAccessible(true);
                    UNSAFE.putObjectVolatile(UNSAFE.staticFieldBase(field), UNSAFE.staticFieldOffset(field), registered);
                } else {
                    Method method = owner.getDeclaredMethod(member.substring(0, paren), Capability.class);
                    method.setAccessible(true);
                    method.invoke(null, registered);
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Couldn't inject capability " + capability + " into " + owner.getName() + "." + member, e);
            }
            return null;
        };
        try {
            String name = capability.intern();
            Map<String, Capability<?>> providers = (Map<String, Capability<?>>) PROVIDERS.get(CapabilityManager.INSTANCE);
            Capability<?> existing = providers.get(name);
            if (existing != null) {
                callback.apply(existing);
                return;
            }
            Map<String, List<Function<Capability<?>, Object>>> callbacks = (Map<String, List<Function<Capability<?>, Object>>>) CALLBACKS.get(CapabilityManager.INSTANCE);
            callbacks.computeIfAbsent(name, key -> new ArrayList<>()).add(callback);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field managerField(String name) {
        try {
            Field field = CapabilityManager.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private ModernCapabilities() {}
}
