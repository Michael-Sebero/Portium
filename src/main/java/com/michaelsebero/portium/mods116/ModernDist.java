package com.michaelsebero.portium.mods116;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.util.List;

/**
 * What Forge 1.13's runtime dist cleaner does to classes as they load: members marked {@code @OnlyIn} another side
 * than this one are removed, as are interfaces marked so through {@code _interface}, and a class marked so can't load.
 * 1.13 mods count on it as 1.12 mods count on {@code @SideOnly}: a client-only method referring to client classes,
 * say, never reaches a dedicated server.
 */
final class ModernDist {

    private static final String ONLY_IN = "Lnet/minecraftforge/api/distmarker/OnlyIn;";
    private static final String ONLY_INS = "Lnet/minecraftforge/api/distmarker/OnlyIns;";

    static void clean(ClassNode node, String dist) {
        String classDist = dist(node.visibleAnnotations);
        if (classDist == null) classDist = dist(node.invisibleAnnotations);
        if (classDist != null && !classDist.equals(dist)) {
            throw new RuntimeException("Attempted to load class " + node.name + " for invalid dist " + dist);
        }
        cleanInterfaces(node, node.visibleAnnotations, dist);
        cleanInterfaces(node, node.invisibleAnnotations, dist);
        node.fields.removeIf(field -> wrongDist(field.visibleAnnotations, field.invisibleAnnotations, dist));
        node.methods.removeIf(method -> wrongDist(method.visibleAnnotations, method.invisibleAnnotations, dist));
    }

    private static boolean wrongDist(@Nullable List<AnnotationNode> visible, @Nullable List<AnnotationNode> invisible, String dist) {
        String marked = dist(visible);
        if (marked == null) marked = dist(invisible);
        return marked != null && !marked.equals(dist);
    }

    /** The side an {@code @OnlyIn} among these marks for the member itself, or null if none does. */
    @Nullable
    private static String dist(@Nullable List<AnnotationNode> annotations) {
        if (annotations == null) return null;
        for (AnnotationNode annotation : annotations) {
            if (ONLY_IN.equals(annotation.desc) && interfaceOf(annotation) == null) return value(annotation);
        }
        return null;
    }

    private static void cleanInterfaces(ClassNode node, @Nullable List<AnnotationNode> annotations, String dist) {
        if (annotations == null) return;
        for (AnnotationNode annotation : annotations) {
            if (ONLY_IN.equals(annotation.desc)) {
                removeInterface(node, annotation, dist);
            } else if (ONLY_INS.equals(annotation.desc) && annotation.values != null) {
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    if (!"value".equals(annotation.values.get(i))) continue;
                    for (Object each : (List<?>) annotation.values.get(i + 1)) removeInterface(node, (AnnotationNode) each, dist);
                }
            }
        }
    }

    private static void removeInterface(ClassNode node, AnnotationNode annotation, String dist) {
        Type type = interfaceOf(annotation);
        String marked = value(annotation);
        if (type != null && marked != null && !marked.equals(dist)) node.interfaces.remove(type.getInternalName());
    }

    @Nullable
    private static Type interfaceOf(AnnotationNode annotation) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if ("_interface".equals(annotation.values.get(i))) return (Type) annotation.values.get(i + 1);
        }
        return null;
    }

    @Nullable
    private static String value(AnnotationNode annotation) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if ("value".equals(annotation.values.get(i))) return ((String[]) annotation.values.get(i + 1))[1];
        }
        return null;
    }

    private ModernDist() {}

    /**
     * On a dedicated server, makes methods that use client classes and wouldn't pass the JVM's verifier there throw
     * instead. A real server has no client classes either, but 1.16 code often hands them on as interfaces the verifier
     * doesn't check (a client world as an {@code IWorld}); renamed to 1.12's classes (its {@code World}), the verifier
     * would load the client class to check it, and fail the whole class. Such a method only runs on a client anyway.
     */
    static void stubClientCode(ClassNode node, ModernRedirects.Hierarchy hierarchy) {
        for (MethodNode method : node.methods) {
            if (method.instructions.size() == 0 || !usesClientClasses(method)) continue;
            if (verifies(node, method, hierarchy)) continue;
            stub(method, node.name + "." + method.name + " is client code");
        }
    }

    private static boolean usesClientClasses(MethodNode method) {
        for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            String owner = null;
            String desc = null;
            if (insn instanceof org.objectweb.asm.tree.MethodInsnNode) {
                owner = ((org.objectweb.asm.tree.MethodInsnNode) insn).owner;
                desc = ((org.objectweb.asm.tree.MethodInsnNode) insn).desc;
            } else if (insn instanceof org.objectweb.asm.tree.FieldInsnNode) {
                owner = ((org.objectweb.asm.tree.FieldInsnNode) insn).owner;
                desc = ((org.objectweb.asm.tree.FieldInsnNode) insn).desc;
            } else if (insn instanceof org.objectweb.asm.tree.TypeInsnNode) {
                owner = ((org.objectweb.asm.tree.TypeInsnNode) insn).desc;
            }
            if (owner != null && owner.startsWith(CLIENT) || desc != null && desc.contains("L" + CLIENT)) return true;
        }
        return false;
    }

    private static final String CLIENT = "net/minecraft/client/";

    /** Whether the JVM's verifier would pass the method, answering type questions from class files, loading nothing. */
    private static boolean verifies(ClassNode node, MethodNode method, ModernRedirects.Hierarchy hierarchy) {
        java.util.List<Type> interfaces = new java.util.ArrayList<>();
        for (String type : node.interfaces) interfaces.add(Type.getObjectType(type));
        boolean isInterface = (node.access & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0;
        Type self = Type.getObjectType(node.name);
        Type superType = node.superName == null ? null : Type.getObjectType(node.superName);
        try {
            new org.objectweb.asm.tree.analysis.Analyzer<>(new HierarchyVerifier(self, superType, interfaces, isInterface, hierarchy)).analyze(node.name, method);
            return true;
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    /** ASM's verifier with the class loader's view of classes (their supertypes, read from their files). */
    private static final class HierarchyVerifier extends org.objectweb.asm.tree.analysis.SimpleVerifier {

        private static final Type OBJECT = Type.getObjectType("java/lang/Object");
        private final Type self;
        private final Type superType;
        private final boolean selfIsInterface;
        private final ModernRedirects.Hierarchy hierarchy;

        HierarchyVerifier(Type self, Type superType, java.util.List<Type> interfaces, boolean isInterface, ModernRedirects.Hierarchy hierarchy) {
            super(self, superType, interfaces, isInterface);
            this.self = self;
            this.superType = superType;
            this.selfIsInterface = isInterface;
            this.hierarchy = hierarchy;
        }

        @Override
        protected boolean isInterface(Type type) {
            if (type.equals(this.self)) return this.selfIsInterface;
            if (type.getSort() != Type.OBJECT) return false;
            Boolean isInterface = this.hierarchy.isInterface(type.getInternalName());
            if (isInterface == null) throw new IllegalStateException("Unknown class " + type.getInternalName());
            return isInterface;
        }

        @Override
        protected Type getSuperClass(Type type) {
            if (type.equals(this.self)) return this.superType;
            if (type.getSort() != Type.OBJECT) return OBJECT;
            String[] supertypes = this.hierarchy.supertypes(type.getInternalName());
            if (supertypes.length == 0) {
                if ("java/lang/Object".equals(type.getInternalName())) return null;
                throw new IllegalStateException("Unknown class " + type.getInternalName());
            }
            return supertypes[0] == null ? null : Type.getObjectType(supertypes[0]);
        }

        @Override
        protected boolean isAssignableFrom(Type to, Type from) {
            if (to.equals(from) || to.equals(OBJECT)) return true;
            if (to.getSort() == Type.ARRAY || from.getSort() == Type.ARRAY) {
                if (to.getSort() != Type.ARRAY || from.getSort() != Type.ARRAY) return false;
                Type toElement = Type.getType(to.getDescriptor().substring(1));
                Type fromElement = Type.getType(from.getDescriptor().substring(1));
                if (toElement.getSort() < Type.ARRAY || fromElement.getSort() < Type.ARRAY) return toElement.equals(fromElement);
                return isAssignableFrom(toElement, fromElement);
            }
            // The JVM takes any value for an interface.
            if (isInterface(to)) return true;
            for (Type current = from; current != null; current = getSuperClass(current)) {
                if (current.equals(to)) return true;
            }
            return false;
        }

        @Override
        protected Class<?> getClass(Type type) {
            throw new IllegalStateException("Verifying loads no classes");
        }
    }

    /** Replaces a method's code with throwing an {@code IllegalStateException}. */
    private static void stub(MethodNode method, String message) {
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) method.localVariables.clear();
        method.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(org.objectweb.asm.Opcodes.NEW, "java/lang/IllegalStateException"));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.DUP));
        method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(message));
        method.instructions.add(new org.objectweb.asm.tree.MethodInsnNode(org.objectweb.asm.Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException", "<init>",
                "(Ljava/lang/String;)V", false));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.ATHROW));
        method.maxStack = Math.max(method.maxStack, 3);
    }
}
