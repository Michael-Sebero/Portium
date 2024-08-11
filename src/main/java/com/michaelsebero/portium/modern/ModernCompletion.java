package com.michaelsebero.portium.modern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives a translated class the methods 1.12 requires of it that it still lacks: abstract methods of the 1.12 classes and
 * interfaces it extends, which 1.13 changed (1.13's {@code IWorldEventListener} takes particle data where 1.12's takes
 * particle ids). Calling a missing one would end in an {@link AbstractMethodError}.
 * <ul>
 * <li>If the class has a method by the same name whose parameters take 1.12's arguments as they are (1.13 asks for an
 * {@code IBlockReader} where 1.12 gives a {@code World}), 1.12's method calls it.</li>
 * <li>Otherwise 1.12's method does nothing and returns its type's zero, false or null.</li>
 * </ul>
 * Classes whose superclass is a mod's (or one of Minecraft 1.13.2's) answer only for the 1.12 interfaces they add; the
 * first class below the parent's answers for the rest.
 */
final class ModernCompletion {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");

    private final ModernMembers members;
    private final ModernRedirects.Hierarchy hierarchy;

    ModernCompletion(ModernMembers members, ModernRedirects.Hierarchy hierarchy) {
        this.members = members;
        this.hierarchy = hierarchy;
    }

    /** @param node a translated class, by runtime names */
    void complete(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) != 0 || node.superName == null) return;

        // The first of the parent's classes above, and this loader's classes on the way (a mod's, 1.13.2's copies).
        List<String> between = new ArrayList<>();
        Class<?> ancestor = null;
        Set<String> seen = new HashSet<>();
        for (String current = node.superName; current != null && seen.add(current); ) {
            ancestor = parent(current);
            if (ancestor != null) break;
            between.add(current);
            String[] supertypes = this.hierarchy.supertypes(current);
            current = supertypes.length > 0 ? supertypes[0] : null;
        }
        if (ancestor == null) return;

        // 1.12's interfaces, listed or reached through this loader's (Forge 1.13's IDynamicBakedModel is an
        // IBakedModel); this loader's interfaces' default methods count as implementations.
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        Set<String> modernDefaults = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(node.interfaces);
        Set<String> visited = new HashSet<>();
        while (!pending.isEmpty()) {
            String name = pending.poll();
            if (!visited.add(name)) continue;
            Class<?> type = parent(name);
            if (type != null) {
                if (type.isInterface()) interfaces.add(type);
                continue;
            }
            modernDefaults.addAll(this.hierarchy.translatedMethods(name));
            for (String supertype : this.hierarchy.supertypes(name)) {
                if (supertype != null && !"java/lang/Object".equals(supertype)) pending.add(supertype);
            }
        }
        if (!between.isEmpty()) {
            // The classes above answer for the interfaces they have, and for the parent class's own methods.
            interfaces.removeIf(type -> inheritsModern(node.superName, type));
            if (interfaces.isEmpty()) return;
        }

        // What's implemented already: by the parent class and those above it (bridges included: they implement
        // generic interface methods), their interfaces' default methods, and this loader's classes on the way.
        Set<String> concrete = new HashSet<>();
        Map<String, Method> required = new LinkedHashMap<>();
        Set<Class<?>> inherited = new LinkedHashSet<>();
        // A class or interface whose methods reflection can't list here (one names a class only the client has) leaves
        // what's implemented unknown: nothing is added rather than something that hides a real method.
        for (Class<?> c = ancestor; c != null; c = c.getSuperclass()) {
            Method[] own = declared(c);
            if (own == null) return;
            for (Method method : own) {
                if (Modifier.isStatic(method.getModifiers())) continue;
                String key = key(method);
                if (!Modifier.isAbstract(method.getModifiers())) {
                    concrete.add(key);
                } else if (between.isEmpty() && !concrete.contains(key)) {
                    required.putIfAbsent(key, method);
                }
            }
            try {
                for (Class<?> type : c.getInterfaces()) inherited.add(type);
            } catch (LinkageError e) {
                // An interface this side lacks.
            }
        }
        for (Class<?> type : inherited) {
            Method[] all = methods(type);
            if (all == null) return;
            for (Method method : all) {
                if (method.isDefault()) concrete.add(key(method));
            }
        }
        if (between.isEmpty()) interfaces.addAll(inherited);
        for (String type : between) concrete.addAll(this.hierarchy.translatedMethods(type));
        concrete.addAll(modernDefaults);
        for (Class<?> type : interfaces) {
            Method[] all = methods(type);
            if (all == null) return;
            for (Method method : all) {
                if (method.isDefault()) concrete.add(key(method));
            }
        }
        for (Class<?> type : interfaces) {
            for (Method method : methods(type)) {
                if (!Modifier.isAbstract(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) continue;
                String key = key(method);
                if (!concrete.contains(key)) required.putIfAbsent(key, method);
            }
        }
        for (MethodNode method : node.methods) required.remove(method.name + method.desc);
        if (required.isEmpty()) return;

        List<String> defaulted = new ArrayList<>();
        for (Method method : required.values()) {
            MethodNode converted = converting(node, method, concrete);
            if (converted != null) {
                node.methods.add(converted);
                continue;
            }
            MethodNode own = taking(node, method);
            node.methods.add(own != null ? bridge(node, method, own) : defaultMethod(method));
            if (own == null) defaulted.add(method.getDeclaringClass().getSimpleName() + "." + method.getName());
        }
        if (!defaulted.isEmpty() && this.reported.add(node.name)) {
            LOGGER.info("{} lacks 1.12's {}, which now do nothing", node.name.replace('/', '.'), String.join(", ", defaulted));
        }
    }

    private final Set<String> reported = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    /** A type's public methods; null if reflection can't list them all on this side. */
    @Nullable
    private static Method[] methods(Class<?> type) {
        try {
            return type.getMethods();
        } catch (LinkageError | RuntimeException e) {
            return null;
        }
    }

    private static final String QUADS_WITH_DATA = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;Ljava/util/Random;"
            + "Lforge113/net/minecraftforge/client/model/data/IModelData;)Ljava/util/List;";
    private static final String QUADS_1_13 = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;Ljava/util/Random;)Ljava/util/List;";

    /**
     * 1.12 methods whose 1.13 versions take something else in an argument's place: a baked model's quads come from a
     * random seed in 1.12, a {@code Random} in 1.13.
     */
    private static final String NAME_1_13 = "()Lnet/minecraft/util/text/ITextComponent;";

    @Nullable
    private static MethodNode converting(ClassNode node, Method required, Set<String> inherited) {
        // 1.12's name of a nameable thing is a string; 1.13's a text component.
        if ("net.minecraft.world.IWorldNameable".equals(required.getDeclaringClass().getName()) && required.getParameterTypes().length == 0
                && required.getReturnType() == String.class) {
            String own = null;
            for (MethodNode method : node.methods) {
                if (method.name.equals("func_200200_C_") && NAME_1_13.equals(method.desc)) own = method.name;
            }
            if (own == null && inherited.contains("func_200200_C_" + NAME_1_13)) own = "func_200200_C_";
            if (own == null) return null;
            MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, required.getName(), "()Ljava/lang/String;", null, null);
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, own, NAME_1_13, false));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/michaelsebero/portium/modern/shim/Conversions", "text",
                    "(Lnet/minecraft/util/text/ITextComponent;)Ljava/lang/String;", false));
            bridge.instructions.add(new InsnNode(Opcodes.ARETURN));
            bridge.maxLocals = 1;
            bridge.maxStack = 1;
            return bridge;
        }
        if (!"net.minecraft.client.renderer.block.model.IBakedModel".equals(required.getDeclaringClass().getName())) return null;
        Class<?>[] parameters = required.getParameterTypes();
        if (parameters.length != 3 || parameters[2] != long.class) return null;
        // A Forge 1.13 model with model data: asked as Forge 1.13 asks it, data first.
        boolean withData = false;
        for (MethodNode method : node.methods) {
            if (QUADS_WITH_DATA.equals(method.desc) && (method.access & Opcodes.ACC_STATIC) == 0) withData = true;
        }
        for (String key : inherited) {
            if (key.endsWith(QUADS_WITH_DATA)) withData = true;
        }
        if (withData) {
            MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, required.getName(), Type.getMethodDescriptor(required), null, null);
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            bridge.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/michaelsebero/portium/modern/host/ModelGlue", "quads",
                    "(Ljava/lang/Object;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/EnumFacing;J)Ljava/util/List;", false));
            bridge.instructions.add(new InsnNode(Opcodes.ARETURN));
            bridge.maxLocals = 5;
            bridge.maxStack = 5;
            return bridge;
        }
        String ownName = null;
        for (MethodNode method : node.methods) {
            if (QUADS_1_13.equals(method.desc) && (method.access & Opcodes.ACC_STATIC) == 0) ownName = method.name;
        }
        for (String key : inherited) {
            if (ownName == null && key.endsWith(QUADS_1_13)) ownName = key.substring(0, key.length() - QUADS_1_13.length());
        }
        if (ownName == null) return null;
        String desc = Type.getMethodDescriptor(required);
        MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, required.getName(), desc, null, null);
        bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        bridge.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.NEW, "java/util/Random"));
        bridge.instructions.add(new InsnNode(Opcodes.DUP));
        bridge.instructions.add(new VarInsnNode(Opcodes.LLOAD, 3));
        bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/Random", "<init>", "(J)V", false));
        bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, ownName, QUADS_1_13, false));
        bridge.instructions.add(new InsnNode(Opcodes.ARETURN));
        bridge.maxLocals = 5;
        bridge.maxStack = 7;
        return bridge;
    }

    /** The class's own method by 1.12's method's name that takes its arguments as they are and returns what it may. */
    @Nullable
    private MethodNode taking(ClassNode node, Method required) {
        Class<?>[] parameters = required.getParameterTypes();
        for (MethodNode method : node.methods) {
            if (!method.name.equals(required.getName()) || (method.access & Opcodes.ACC_STATIC) != 0) continue;
            Type[] own = Type.getArgumentTypes(method.desc);
            if (own.length != parameters.length) continue;
            boolean fits = returns(Type.getReturnType(method.desc), required.getReturnType());
            for (int i = 0; fits && i < own.length; i++) fits = accepts(own[i], parameters[i]);
            if (fits) return method;
        }
        return null;
    }

    /** Whether a parameter of this type takes a value of 1.12's type. */
    private boolean accepts(Type parameter, Class<?> given) {
        if (parameter.getSort() != Type.OBJECT) return parameter.getDescriptor().equals(Type.getDescriptor(given));
        if (given.isPrimitive()) return false;
        Class<?> type = parent(parameter.getInternalName());
        if (type == null && parameter.getInternalName().startsWith("java/")) type = javaType(parameter.getClassName());
        return type != null && type.isAssignableFrom(given);
    }

    /** Whether a method returning this type can stand for one returning 1.12's. */
    private boolean returns(Type returned, Class<?> expected) {
        if (expected == void.class) return returned.getSort() == Type.VOID;
        if (returned.getSort() != Type.OBJECT) return returned.getDescriptor().equals(Type.getDescriptor(expected));
        if (expected.isPrimitive()) return false;
        Class<?> type = parent(returned.getInternalName());
        if (type == null && returned.getInternalName().startsWith("java/")) type = javaType(returned.getClassName());
        return type != null && expected.isAssignableFrom(type);
    }

    private static MethodNode bridge(ClassNode node, Method required, MethodNode own) {
        String desc = Type.getMethodDescriptor(required);
        MethodNode bridge = new MethodNode(access(required) | Opcodes.ACC_SYNTHETIC, required.getName(), desc, null, null);
        bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (Type argument : Type.getArgumentTypes(desc)) {
            bridge.instructions.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), slot));
            slot += argument.getSize();
        }
        bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, own.name, own.desc, false));
        bridge.instructions.add(new InsnNode(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN)));
        int returned = Type.getReturnType(desc).getSize();
        bridge.maxLocals = slot;
        bridge.maxStack = Math.max(slot, returned);
        return bridge;
    }

    private static MethodNode defaultMethod(Method required) {
        String desc = Type.getMethodDescriptor(required);
        MethodNode method = new MethodNode(access(required) | Opcodes.ACC_SYNTHETIC, required.getName(), desc, null, null);
        Type returned = Type.getReturnType(desc);
        switch (returned.getSort()) {
            case Type.VOID:
                break;
            case Type.LONG:
                method.instructions.add(new InsnNode(Opcodes.LCONST_0));
                break;
            case Type.FLOAT:
                method.instructions.add(new InsnNode(Opcodes.FCONST_0));
                break;
            case Type.DOUBLE:
                method.instructions.add(new InsnNode(Opcodes.DCONST_0));
                break;
            case Type.OBJECT:
                // Names and collections are better empty than missing.
                String empty = EMPTY.get(returned.getInternalName());
                if ("java/lang/String".equals(returned.getInternalName())) {
                    method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(""));
                } else if (empty != null) {
                    int dot = empty.indexOf('.');
                    int paren = empty.indexOf('(');
                    method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, empty.substring(0, dot), empty.substring(dot + 1, paren),
                            empty.substring(paren), false));
                } else {
                    method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                }
                break;
            case Type.ARRAY:
                method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                break;
            default:
                method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        }
        method.instructions.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
        int slots = 1;
        for (Type argument : Type.getArgumentTypes(desc)) slots += argument.getSize();
        method.maxLocals = slots;
        method.maxStack = Math.max(1, returned.getSize());
        return method;
    }

    private static final Map<String, String> EMPTY = new java.util.HashMap<>();

    static {
        EMPTY.put("java/util/List", "java/util/Collections.emptyList()Ljava/util/List;");
        EMPTY.put("java/util/Collection", "java/util/Collections.emptyList()Ljava/util/List;");
        EMPTY.put("java/util/Set", "java/util/Collections.emptySet()Ljava/util/Set;");
        EMPTY.put("java/util/Map", "java/util/Collections.emptyMap()Ljava/util/Map;");
        EMPTY.put("java/util/Optional", "java/util/Optional.empty()Ljava/util/Optional;");
    }

    private static int access(Method method) {
        return Modifier.isProtected(method.getModifiers()) ? Opcodes.ACC_PROTECTED : Opcodes.ACC_PUBLIC;
    }

    /** Whether a class of this loader's, by translated name, already has the interface from above. */
    private boolean inheritsModern(String type, Class<?> wanted) {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!seen.add(current)) continue;
            Class<?> known = parent(current);
            if (known != null) {
                if (wanted.isAssignableFrom(known)) return true;
                continue;
            }
            for (String supertype : this.hierarchy.supertypes(current)) {
                if (supertype != null) queue.add(supertype);
            }
        }
        return false;
    }

    @Nullable
    private Class<?> parent(String type) {
        if (type.startsWith("[")) return null;
        if (type.startsWith("java/")) return javaType(type.replace('/', '.'));
        return this.members.type(type);
    }

    @Nullable
    private static Class<?> javaType(String name) {
        try {
            return Class.forName(name, false, ModernCompletion.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /** A type's own methods; null if reflection can't list them all on this side. */
    @Nullable
    private static Method[] declared(Class<?> type) {
        try {
            return type.getDeclaredMethods();
        } catch (LinkageError | RuntimeException e) {
            return null;
        }
    }

    private static String key(Method method) {
        return method.getName() + Type.getMethodDescriptor(method);
    }
}
