package com.michaelsebero.portium.modern;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Lets 1.13 code reach members 1.12 has but keeps from it: private or package-private there, public in 1.13 (or made
 * so by Forge 1.13), or inside another class loader's package. The translator turns such a field access or call into
 * an {@code invokedynamic} bootstrapped here, which links it once to a method handle with access to the member.
 */
public final class ModernAccess {

    public static final int GET_FIELD = 0;
    public static final int PUT_FIELD = 1;
    public static final int GET_STATIC = 2;
    public static final int PUT_STATIC = 3;
    public static final int INVOKE_VIRTUAL = 4;
    public static final int INVOKE_STATIC = 5;

    static final String BOOTSTRAP_DESC = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/invoke/CallSite;";

    /**
     * @param type   the call site's type: the receiver first for instance members, then the arguments or the value
     * @param owner  the class the 1.13 code names the member through, by binary name (it may not be public)
     * @param member the member's runtime name
     */
    public static CallSite bootstrap(MethodHandles.Lookup caller, String name, MethodType type, String ownerName, String member, int kind)
            throws ReflectiveOperationException {
        Class<?> owner = Class.forName(ownerName, false, caller.lookupClass().getClassLoader());
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle;
        switch (kind) {
            case GET_FIELD:
                handle = lookup.unreflectGetter(field(owner, member, type.returnType()));
                break;
            case GET_STATIC:
                handle = lookup.unreflectGetter(field(owner, member, type.returnType()));
                break;
            case PUT_FIELD:
                handle = lookup.unreflectSetter(field(owner, member, type.parameterType(1)));
                break;
            case PUT_STATIC:
                handle = lookup.unreflectSetter(field(owner, member, type.parameterType(0)));
                break;
            case INVOKE_VIRTUAL:
                handle = lookup.unreflect(method(owner, member, type.dropParameterTypes(0, 1)));
                break;
            case INVOKE_STATIC:
                handle = lookup.unreflect(method(owner, member, type));
                break;
            default:
                throw new IllegalArgumentException("Unknown access kind " + kind);
        }
        return new ConstantCallSite(handle.asType(type));
    }

    private static Field field(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        for (Class<?> c : lineage(owner)) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getName().equals(name) && field.getType() == type) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        throw new NoSuchFieldException(owner.getName() + "." + name);
    }

    private static Method method(Class<?> owner, String name, MethodType type) throws NoSuchMethodException {
        for (Class<?> c : lineage(owner)) {
            for (Method method : c.getDeclaredMethods()) {
                if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), type.parameterArray())
                        && method.getReturnType() == type.returnType()) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        throw new NoSuchMethodException(owner.getName() + "." + name + type);
    }

    private static Iterable<Class<?>> lineage(Class<?> owner) {
        Set<Class<?>> order = new LinkedHashSet<>();
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) order.add(c);
        Deque<Class<?>> queue = new ArrayDeque<>();
        for (Class<?> c : new HashSet<>(order)) Collections.addAll(queue, c.getInterfaces());
        while (!queue.isEmpty()) {
            Class<?> next = queue.poll();
            if (order.add(next)) Collections.addAll(queue, next.getInterfaces());
        }
        return order;
    }

    private ModernAccess() {}
}
