package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.ModernHooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Forge 1.13's {@code ObfuscationReflectionHelper}, which finds members by their SRG names. Minecraft runs with SRG
 * names on 1.12 too, except in a development environment, where the names are translated as 1.13 code's are.
 */
public final class ReflectionShims {

    /** Forge 1.14+'s {@code remapName(domain, srgName)}: the name the member has at run time. */
    public static String remapName(Object domain, String name) {
        return runtimeName(Object.class, name);
    }

    public static Method findMethod(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(runtimeName(owner, name), parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No method " + name + " in " + owner.getName(), e);
        }
    }

    public static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(runtimeName(owner, name));
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("No field " + name + " in " + owner.getName(), e);
        }
    }

    public static Object getPrivateValue(Class<?> owner, Object instance, String name) {
        try {
            return findField(owner, name).get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setPrivateValue(Class<?> owner, Object instance, Object value, String name) {
        try {
            findField(owner, name).set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The name a member goes by at runtime, given its SRG name: by the 1.13 mods' translator, else the 1.16 mods'
     * (with only 1.16 mods loaded, the 1.13 one isn't made, and in a development environment Create's reflection on
     * {@code ParticleManager.field_178932_g} found nothing by its SRG name).
     */
    private static String runtimeName(Class<?> owner, String name) {
        String mapped = com.michaelsebero.portium.modern.ModernHooks.memberName(owner, name);
        return mapped.equals(name) ? com.michaelsebero.portium.mods116.ModernHooks.memberName(owner, name) : mapped;
    }

    private ReflectionShims() {}
}
