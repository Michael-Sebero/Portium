package com.michaelsebero.portium.modern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minecraft 1.13.2's own code for methods and static fields 1.13 added to classes 1.12 has (see {@link ModernNatives}):
 * a call to one becomes a call to a static method holding 1.13's body, the receiver its first argument, and a read of
 * one reads a field set the way 1.13's static initialiser sets it. Each lives in a class of its own, made when first
 * needed and translated like any 1.13 class, so what it calls that 1.12 lacks is bridged in turn.
 */
final class ModernBridges {

    static final String PREFIX = "com/michaelsebero/portium/modern/bridge/Bridge$";
    static final String FIELD = "value";

    /** What a bridge class holds: a method's body or a field's initialiser, taken from a 1.13.2 class. */
    private static final class Target {

        final String declarer;
        final String name;
        final String desc;
        final boolean field;
        /** Portium's adapter of the class called through, which has the method itself; null if none. */
        @Nullable
        final String adapter;

        Target(String declarer, String name, String desc, boolean field, @Nullable String adapter) {
            this.declarer = declarer;
            this.name = name;
            this.desc = desc;
            this.field = field;
            this.adapter = adapter;
        }
    }

    /** A bridge for a call: the static method to call instead. */
    static final class Bridge {

        final String owner;
        final String name;
        final String desc;

        Bridge(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }
    }

    private final ModernNatives natives;
    private final Map<String, Bridge> byMember = new ConcurrentHashMap<>();
    private final Map<String, Target> targets = new ConcurrentHashMap<>();
    private final Map<String, ClassNode> nodes = new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    ModernBridges(ModernNatives natives) {
        this.natives = natives;
    }

    /**
     * The static method holding 1.13.2's body of {@code owner.name desc} (1.13 names, as called), or null if 1.13.2
     * has no body for it to give: no such class or method, or an abstract one.
     */
    @Nullable
    Bridge method(String owner, String name, String desc, boolean isStatic) {
        return method(owner, name, desc, isStatic, null);
    }

    /**
     * As {@link #method(String, String, String, boolean)}; with an adapter, receivers that are that adapter (Portium's
     * 1.13 version of the class) get its method instead of 1.13.2's body.
     */
    @Nullable
    Bridge method(String owner, String name, String desc, boolean isStatic, @Nullable String adapter) {
        String key = owner + "." + name + desc;
        Bridge known = this.byMember.get(key);
        if (known != null) return known;
        String declarer = declarer(owner, name, desc, isStatic);
        if (declarer == null) return null;
        String bridgeDesc = isStatic ? desc : "(L" + declarer + ";" + desc.substring(1);
        String className = PREFIX + this.counter.incrementAndGet();
        this.targets.put(className, new Target(declarer, name, desc, false, isStatic ? null : adapter));
        Bridge bridge = new Bridge(className, name, bridgeDesc);
        Bridge raced = this.byMember.putIfAbsent(key, bridge);
        return raced != null ? raced : bridge;
    }

    /**
     * The class whose static {@link #FIELD} holds 1.13.2's value of the static field {@code owner.name}, or null if
     * 1.13.2's static initialiser doesn't set it in a way that can be lifted out: a straight run of instructions.
     */
    @Nullable
    String field(String owner, String name, String desc) {
        String key = owner + "." + name + ":" + desc;
        Bridge known = this.byMember.get(key);
        if (known != null) return known.owner;
        // The class that declares it: a static field reached through a subclass (BlockPane.NORTH is BlockFourWay's).
        String declarer = owner;
        ClassNode node = node(declarer);
        java.util.Set<String> seen = new java.util.HashSet<>();
        while (node != null && seen.add(declarer) && !declaresField(node, name, desc)) {
            declarer = node.superName;
            node = declarer == null ? null : node(declarer);
        }
        if (node == null || initializer(node, name, desc) == null) return null;
        String className = PREFIX + this.counter.incrementAndGet();
        this.targets.put(className, new Target(declarer, name, desc, true, null));
        Bridge raced = this.byMember.putIfAbsent(key, new Bridge(className, FIELD, desc));
        return raced != null ? raced.owner : className;
    }

    /**
     * The registry names 1.13.2's {@code Blocks} or {@code Items} gives its fields, read from the static initialiser
     * (each field is set from a lookup by name), by field name; empty if 1.13.2 has no such class.
     */
    Map<String, String> registeredNames(String owner) {
        Map<String, String> names = new java.util.LinkedHashMap<>();
        ClassNode node = node(owner);
        MethodNode clinit = node == null ? null : find(node, "<clinit>", "()V");
        if (clinit == null) return names;
        String last = null;
        for (AbstractInsnNode insn = clinit.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof org.objectweb.asm.tree.LdcInsnNode && ((org.objectweb.asm.tree.LdcInsnNode) insn).cst instanceof String) {
                last = (String) ((org.objectweb.asm.tree.LdcInsnNode) insn).cst;
            } else if (insn.getOpcode() == Opcodes.PUTSTATIC) {
                FieldInsnNode put = (FieldInsnNode) insn;
                if (last != null && put.owner.equals(owner)) names.put(put.name, last);
                last = null;
            }
        }
        return names;
    }

    /**
     * The first int constant in the static initialiser's code for a field, as in {@code new MaterialColor(36, ...)}:
     * the colour's index; null if there's none.
     */
    @Nullable
    Integer constructorIndex(String owner, String field, String desc) {
        ClassNode node = node(owner);
        InsnList code = node == null ? null : initializer(node, field, desc);
        if (code == null) return null;
        for (AbstractInsnNode insn = code.getFirst(); insn != null; insn = insn.getNext()) {
            int opcode = insn.getOpcode();
            if (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5) return opcode - Opcodes.ICONST_0;
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) return ((org.objectweb.asm.tree.IntInsnNode) insn).operand;
        }
        return null;
    }

    /** For an enum constant: the name and ordinal 1.13.2's static initialiser makes it with, or null. */
    @Nullable
    Object[] enumConstant(String owner, String field, String desc) {
        ClassNode node = node(owner);
        InsnList code = node == null || (node.access & Opcodes.ACC_ENUM) == 0 ? null : initializer(node, field, desc);
        if (code == null) return null;
        String name = null;
        for (AbstractInsnNode insn = code.getFirst(); insn != null; insn = insn.getNext()) {
            if (name == null) {
                if (insn instanceof org.objectweb.asm.tree.LdcInsnNode && ((org.objectweb.asm.tree.LdcInsnNode) insn).cst instanceof String) name = (String) ((org.objectweb.asm.tree.LdcInsnNode) insn).cst;
                continue;
            }
            int opcode = insn.getOpcode();
            if (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5) return new Object[] {name, opcode - Opcodes.ICONST_0};
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) return new Object[] {name, ((org.objectweb.asm.tree.IntInsnNode) insn).operand};
        }
        return null;
    }

    /** The bridge classes made so far, by internal name. */
    java.util.List<String> names() {
        return new java.util.ArrayList<>(this.targets.keySet());
    }

    /** What a bridge class holds, as {@code Declarer.member}, for messages. */
    @Nullable
    String describe(String className) {
        Target target = this.targets.get(className);
        return target == null ? null : target.declarer.substring(target.declarer.lastIndexOf('/') + 1) + "." + target.name;
    }

    private static boolean declaresField(ClassNode node, String name, String desc) {
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) return true;
        }
        return false;
    }

    boolean isBridge(String internalName) {
        return this.targets.containsKey(internalName);
    }

    /** A bridge class's bytes, by 1.13 names (they're translated as they're defined), or null if it isn't one. */
    @Nullable
    byte[] generate(String className) {
        Target target = this.targets.get(className);
        if (target == null) return null;
        ClassNode declarer = node(target.declarer);
        if (declarer == null) return null;
        ClassNode bridge = new ClassNode();
        bridge.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC, className, null, "java/lang/Object", null);
        bridge.sourceFile = "Minecraft 1.13.2 " + target.declarer.substring(target.declarer.lastIndexOf('/') + 1);
        if (target.field) {
            InsnList code = initializer(declarer, target.name, target.desc);
            if (code == null) return null;
            bridge.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, FIELD, target.desc, null, null));
            MethodNode clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(code);
            clinit.instructions.add(new FieldInsnNode(Opcodes.PUTSTATIC, className, FIELD, target.desc));
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            bridge.methods.add(clinit);
        } else {
            MethodNode original = find(declarer, target.name, target.desc);
            if (original == null) return null;
            boolean isStatic = (original.access & Opcodes.ACC_STATIC) != 0;
            String bridgeDesc = isStatic ? target.desc : "(L" + target.declarer + ";" + target.desc.substring(1);
            MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, target.name, bridgeDesc, null, null);
            // A copy: the same method may be bridged again through another class.
            original.accept(method);
            method.visibleAnnotations = null;
            method.invisibleAnnotations = null;
            method.visibleParameterAnnotations = null;
            method.invisibleParameterAnnotations = null;
            method.parameters = null;
            if (target.adapter != null) toAdapter(method, target, bridgeDesc);
            // super.m() and private calls on the receiver: the receiver is now an argument.
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.INVOKESPECIAL && !"<init>".equals(((MethodInsnNode) insn).name)) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    call.setOpcode(call.itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL);
                }
            }
            bridge.methods.add(method);
        }
        // No frames to compute: maxima only, which loads no classes.
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        bridge.accept(writer);
        return writer.toByteArray();
    }

    /** Starts a bridge with a call to the adapter's own method when the receiver is the adapter. */
    private static void toAdapter(MethodNode method, Target target, String bridgeDesc) {
        org.objectweb.asm.Type[] arguments = org.objectweb.asm.Type.getArgumentTypes(bridgeDesc);
        org.objectweb.asm.Type returned = org.objectweb.asm.Type.getReturnType(bridgeDesc);
        InsnList prefix = new InsnList();
        LabelNode own = new LabelNode();
        prefix.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
        prefix.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.INSTANCEOF, target.adapter));
        prefix.add(new JumpInsnNode(Opcodes.IFEQ, own));
        prefix.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
        prefix.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.CHECKCAST, target.adapter));
        int slot = 1;
        for (int i = 1; i < arguments.length; i++) {
            prefix.add(new org.objectweb.asm.tree.VarInsnNode(arguments[i].getOpcode(Opcodes.ILOAD), slot));
            slot += arguments[i].getSize();
        }
        prefix.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, target.adapter, target.name, target.desc, false));
        prefix.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
        prefix.add(own);
        // The original code's start is now a jump target, which needs a frame, unless it already has one there.
        AbstractInsnNode first = method.instructions.getFirst();
        while (first instanceof LabelNode || first instanceof LineNumberNode) first = first.getNext();
        if (!(first instanceof FrameNode)) {
            Object[] locals = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) locals[i] = frameType(arguments[i]);
            prefix.add(new FrameNode(Opcodes.F_NEW, locals.length, locals, 0, new Object[0]));
        }
        method.instructions.insert(prefix);
        method.maxStack = Math.max(method.maxStack, slot + 1);
    }

    private static Object frameType(org.objectweb.asm.Type type) {
        switch (type.getSort()) {
            case org.objectweb.asm.Type.BOOLEAN:
            case org.objectweb.asm.Type.BYTE:
            case org.objectweb.asm.Type.CHAR:
            case org.objectweb.asm.Type.SHORT:
            case org.objectweb.asm.Type.INT:
                return Opcodes.INTEGER;
            case org.objectweb.asm.Type.FLOAT:
                return Opcodes.FLOAT;
            case org.objectweb.asm.Type.LONG:
                return Opcodes.LONG;
            case org.objectweb.asm.Type.DOUBLE:
                return Opcodes.DOUBLE;
            case org.objectweb.asm.Type.ARRAY:
                return type.getDescriptor();
            default:
                return type.getInternalName();
        }
    }

    /** Where 1.13.2 declares a method reached through {@code owner}, if it has a body there. */
    @Nullable
    private String declarer(String owner, String name, String desc, boolean isStatic) {
        for (String type = owner; type != null; ) {
            ClassNode node = node(type);
            if (node == null) return null;
            MethodNode method = find(node, name, desc);
            if (method != null) {
                boolean methodStatic = (method.access & Opcodes.ACC_STATIC) != 0;
                if (methodStatic != isStatic || (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) return null;
                return type;
            }
            // Default methods of interfaces.
            for (String itf : node.interfaces) {
                String found = declarer(itf, name, desc, isStatic);
                if (found != null) return found;
            }
            type = node.superName;
        }
        return null;
    }

    @Nullable
    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return method;
        }
        return null;
    }

    /**
     * The instructions of 1.13.2's static initialiser that compute a static field's value: those since the previous
     * field was set, if they neither branch nor touch locals. Null if there's no such run.
     */
    @Nullable
    private static InsnList initializer(ClassNode node, String name, String desc) {
        MethodNode clinit = find(node, "<clinit>", "()V");
        if (clinit == null) return null;
        AbstractInsnNode start = clinit.instructions.getFirst();
        for (AbstractInsnNode insn = start; insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.PUTSTATIC) continue;
            FieldInsnNode put = (FieldInsnNode) insn;
            if (put.owner.equals(node.name) && put.name.equals(name) && put.desc.equals(desc)) {
                InsnList code = new InsnList();
                Map<LabelNode, LabelNode> labels = new HashMap<>();
                for (AbstractInsnNode each = start; each != insn; each = each.getNext()) {
                    if (each instanceof LabelNode) labels.put((LabelNode) each, new LabelNode());
                }
                for (AbstractInsnNode each = start; each != insn; each = each.getNext()) {
                    if (each instanceof JumpInsnNode || each instanceof TableSwitchInsnNode || each instanceof LookupSwitchInsnNode
                            || each.getType() == AbstractInsnNode.VAR_INSN || each.getType() == AbstractInsnNode.IINC_INSN) {
                        return null;
                    }
                    if (each instanceof FrameNode || each instanceof LineNumberNode || each instanceof LabelNode) continue;
                    code.add(each.clone(labels));
                }
                return code;
            }
            start = insn.getNext();
        }
        return null;
    }

    @Nullable
    private ClassNode node(String name) {
        ClassNode known = this.nodes.get(name);
        if (known != null) return known;
        try {
            byte[] bytes = this.natives.read(name);
            if (bytes == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
            this.nodes.put(name, node);
            return node;
        } catch (IOException e) {
            return null;
        }
    }
}
