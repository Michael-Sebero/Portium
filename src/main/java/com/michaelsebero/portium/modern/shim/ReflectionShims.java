package com.michaelsebero.portium.modern.shim;

import com.michaelsebero.portium.modern.ModernHooks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Forge 1.13's {@code ObfuscationReflectionHelper}, which finds members by their SRG names. Minecraft runs with SRG
 * names on 1.12 too, except in a development environment, where the names are translated as 1.13 code's are.
 */
public final class ReflectionShims {

    public static Method findMethod(Class<?> owner, String name, Class<?>... parameters) {
        try {
            Method method = owner.getDeclaredMethod(ModernHooks.memberName(owner, name), parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No method " + name + " in " + owner.getName(), e);
        }
    }

    public static Field findField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(ModernHooks.memberName(owner, name));
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

    private ReflectionShims() {}
}
