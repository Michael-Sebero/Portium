package com.michaelsebero.portium.modern;

import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The methods and fields 1.12 has: those of the parent class loader's classes (Minecraft, Forge 1.12, Portium),
 * found by reflection on the classes as they run, so with their runtime names and Forge's and Portium's changes.
 * Classes are looked at without being initialised.
 */
final class ModernMembers {

    /** A method or field of one of the parent's classes. */
    static final class Member {

        final Class<?> owner;
        final String name;
        final String desc;
        final int access;
        final boolean method;

        Member(Class<?> owner, String name, String desc, int access, boolean method) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
            this.access = access;
            this.method = method;
        }

        String ownerName() {
            return Type.getInternalName(this.owner);
        }

        boolean isStatic() {
            return Modifier.isStatic(this.access);
        }

        boolean isPrivate() {
            return Modifier.isPrivate(this.access);
        }

        @Override
        public String toString() {
            return ownerName() + "." + this.name + this.desc;
        }
    }

    private final ClassLoader parent;
    private final Map<String, Optional<Class<?>>> classes = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Member>> declared = new ConcurrentHashMap<>();

    ModernMembers(ClassLoader parent) {
        this.parent = parent;
    }

    /** One of the parent's classes by internal name, uninitialised; null if the parent has no such class. */
    @Nullable
    Class<?> type(String name) {
        if (name.startsWith("[")) return null;
        return this.classes.computeIfAbsent(name, key -> {
            try {
                return Optional.of(Class.forName(key.replace('/', '.'), false, this.parent));
            } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    /** The method a call to {@code owner.name desc} reaches, resolved as the JVM resolves it, or null if none. */
    @Nullable
    Member method(String owner, String name, String desc) {
        Class<?> type = type(owner);
        if (type == null) return null;
        for (Class<?> c : "<init>".equals(name) ? Collections.<Class<?>>singletonList(type) : lineage(type)) {
            for (Member member : declared(c)) {
                if (member.method && member.name.equals(name) && member.desc.equals(desc)) return member;
            }
        }
        return null;
    }

    /** The field {@code owner.name desc} reaches, or null if none. */
    @Nullable
    Member field(String owner, String name, String desc) {
        Class<?> type = type(owner);
        if (type == null) return null;
        for (Class<?> c : lineage(type)) {
            for (Member member : declared(c)) {
                if (!member.method && member.name.equals(name) && member.desc.equals(desc)) return member;
            }
        }
        return null;
    }

    /** The field called {@code name} that {@code owner} reaches, whatever its type; null if none. */
    @Nullable
    Member fieldNamed(String owner, String name) {
        Class<?> type = type(owner);
        if (type == null) return null;
        for (Class<?> c : lineage(type)) {
            for (Member member : declared(c)) {
                if (!member.method && member.name.equals(name)) return member;
            }
        }
        return null;
    }

    /** Every method called {@code name} that {@code owner} declares or inherits, nearest first. */
    List<Member> methodsNamed(String owner, String name) {
        Class<?> type = type(owner);
        if (type == null) return Collections.emptyList();
        List<Member> found = new ArrayList<>();
        for (Class<?> c : lineage(type)) {
            for (Member member : declared(c)) {
                if (member.method && member.name.equals(name)) found.add(member);
            }
        }
        return found;
    }

    /** A class, its superclasses, then all its interfaces: the order the JVM looks for members in. */
    private static List<Class<?>> lineage(Class<?> type) {
        List<Class<?>> order = new ArrayList<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) order.add(c);
        Set<Class<?>> interfaces = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>();
        for (Class<?> c : new ArrayList<>(order)) Collections.addAll(queue, c.getInterfaces());
        while (!queue.isEmpty()) {
            Class<?> next = queue.poll();
            if (interfaces.add(next)) {
                order.add(next);
                Collections.addAll(queue, next.getInterfaces());
            }
        }
        if (type.isInterface()) order.add(Object.class);
        return order;
    }

    private List<Member> declared(Class<?> type) {
        List<Member> known = this.declared.get(type);
        if (known != null) return known;
        List<Member> found = new ArrayList<>();
        // Each kind apart: reflection fails for all of a kind if one signature names a class this side lacks (1.12's
        // Vec3d has a method taking the client's Vec2f, so on a server none of its methods reflect).
        try {
            for (Method method : type.getDeclaredMethods()) {
                found.add(new Member(type, method.getName(), Type.getMethodDescriptor(method), method.getModifiers(), true));
            }
        } catch (LinkageError | RuntimeException e) {
            found.addAll(methodsFromBytes(type));
        }
        try {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                found.add(new Member(type, "<init>", Type.getConstructorDescriptor(constructor), constructor.getModifiers(), true));
            }
        } catch (LinkageError | RuntimeException e) {
            // A constructor names a class this side lacks.
        }
        try {
            for (Field field : type.getDeclaredFields()) {
                found.add(new Member(type, field.getName(), Type.getDescriptor(field.getType()), field.getModifiers(), false));
            }
        } catch (LinkageError | RuntimeException e) {
            // A field's type is a class this side lacks.
        }
        this.declared.put(type, found);
        return found;
    }

    /**
     * A class's methods read from its bytes, for when reflection can't list them: as the game runs them, with Forge's
     * patches and, outside development, SRG names.
     */
    private List<Member> methodsFromBytes(Class<?> type) {
        List<Member> found = new ArrayList<>();
        byte[] bytes = classBytes(type);
        if (bytes == null) return found;
        org.objectweb.asm.tree.ClassNode node = new org.objectweb.asm.tree.ClassNode();
        try {
            new org.objectweb.asm.ClassReader(bytes).accept(node, org.objectweb.asm.ClassReader.SKIP_CODE);
        } catch (RuntimeException e) {
            return found;
        }
        for (org.objectweb.asm.tree.MethodNode method : node.methods) {
            if (method.name.equals("<init>") || method.name.equals("<clinit>")) continue;
            found.add(new Member(type, method.name, method.desc, method.access & 0xFFFF, true));
        }
        return found;
    }

    @Nullable
    private byte[] classBytes(Class<?> type) {
        String name = type.getName();
        try {
            if (this.parent instanceof net.minecraft.launchwrapper.LaunchClassLoader) {
                net.minecraft.launchwrapper.LaunchClassLoader launch = (net.minecraft.launchwrapper.LaunchClassLoader) this.parent;
                boolean deobfuscated = Boolean.TRUE.equals(net.minecraft.launchwrapper.Launch.blackboard.get("fml.deobfuscatedEnvironment"));
                String untransformed = deobfuscated ? name
                        : net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(name.replace('.', '/')).replace('/', '.');
                byte[] bytes = launch.getClassBytes(untransformed);
                if (bytes == null || deobfuscated) return bytes;
                bytes = net.minecraftforge.fml.common.patcher.ClassPatchManager.INSTANCE.applyPatch(untransformed, name, bytes);
                return new net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer().transform(untransformed, name, bytes);
            }
            try (java.io.InputStream in = this.parent.getResourceAsStream(name.replace('.', '/') + ".class")) {
                return in == null ? null : org.apache.commons.io.IOUtils.toByteArray(in);
            }
        } catch (java.io.IOException | RuntimeException | LinkageError e) {
            return null;
        }
    }
}
