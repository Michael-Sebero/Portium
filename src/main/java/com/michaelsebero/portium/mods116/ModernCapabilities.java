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
                // The class is set up first: its own initialiser sets the field (to null), and would undo the injection.
                Class.forName(owner.getName(), true, owner.getClassLoader());
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
            String name = ownName(capability).intern();
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

    /**
     * The capability name a 1.16 interface goes by: 1.16's fluid handlers are another API from 1.12's interfaces of the
     * same names (Portium keeps Forge 1.16's fluid package whole), so they get capabilities of their own, registered
     * here when first asked for.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String ownName(String capability) {
        if (capability.startsWith("forge116.")) capability = capability.substring("forge116.".length());
        if (!capability.startsWith("net.minecraftforge.fluids.")) return capability;
        String name = (capability + " (1.16)").intern();
        try {
            Map<String, Capability<?>> providers = (Map<String, Capability<?>>) PROVIDERS.get(CapabilityManager.INSTANCE);
            synchronized (providers) {
                if (!providers.containsKey(name)) {
                    java.lang.reflect.Constructor<Capability> make = Capability.class.getDeclaredConstructor(String.class, Capability.IStorage.class, java.util.concurrent.Callable.class);
                    make.setAccessible(true);
                    Capability.IStorage storage = new Capability.IStorage() {
                        @Override
                        public net.minecraft.nbt.NBTBase writeNBT(Capability capability, Object instance, EnumFacing side) {
                            return null;
                        }

                        @Override
                        public void readNBT(Capability capability, Object instance, EnumFacing side, net.minecraft.nbt.NBTBase nbt) {}
                    };
                    providers.put(name, make.newInstance(name, storage, (java.util.concurrent.Callable<Object>) () -> null));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't make the 1.16 capability " + capability, e);
        }
        return name;
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
