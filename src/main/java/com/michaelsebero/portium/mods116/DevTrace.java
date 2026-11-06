package com.michaelsebero.portium.mods116;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A development aid: {@code -Dportium.dev.trace=Class#method,Class#*,...} logs each call of those methods of 1.16 mods'
 * classes as {@code Class#method(arguments) -> result}, each different line once. A class is named by its simple name
 * (an inner class as {@code Outer$Inner}) or its full name with dots or slashes.
 */
public final class DevTrace {

    private static final Logger LOGGER = LogManager.getLogger("Portium/Trace");
    private static final List<String[]> TARGETS = targets(System.getProperty("portium.dev.trace"));
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Deque<String[]>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final String SELF = "com/michaelsebero/portium/mods116/DevTrace";

    private static List<String[]> targets(String spec) {
        List<String[]> targets = new ArrayList<>();
        if (spec == null) return targets;
        for (String each : spec.split(",")) {
            int hash = each.indexOf('#');
            if (hash < 0) targets.add(new String[] {each.trim().replace('.', '/'), "*"});
            else targets.add(new String[] {each.substring(0, hash).trim().replace('.', '/'), each.substring(hash + 1).trim()});
        }
        return targets;
    }

    private static boolean classMatches(String pattern, String name) {
        return name.equals(pattern) || name.endsWith("/" + pattern);
    }

    static byte[] apply(byte[] bytes) {
        if (TARGETS.isEmpty()) return bytes;
        String name = new ClassReader(bytes).getClassName();
        boolean any = false;
        for (String[] target : TARGETS) any |= classMatches(target[0], name);
        if (!any) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        String simple = name.substring(name.lastIndexOf('/') + 1);
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if ((method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || method.instructions.size() == 0) continue;
            // Constructors can't be: `this` isn't usable before super() runs.
            if (method.name.startsWith("<")) continue;
            boolean wanted = false;
            for (String[] target : TARGETS) {
                if (classMatches(target[0], name) && (target[1].equals("*") ? !method.name.startsWith("<") && !method.name.startsWith("lambda$") : target[1].equals(method.name))) wanted = true;
            }
            if (!wanted) continue;
            instrument(simple + "#" + method.name, method);
            changed = true;
        }
        if (!changed) return bytes;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        LOGGER.info("Tracing calls in {}", name);
        return writer.toByteArray();
    }

    private static void instrument(String id, MethodNode method) {
        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        Type[] args = Type.getArgumentTypes(method.desc);
        InsnList entry = new InsnList();
        entry.add(new LdcInsnNode(id));
        entry.add(isStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0));
        entry.add(new IntInsnNode(Opcodes.SIPUSH, args.length));
        entry.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        int slot = isStatic ? 0 : 1;
        for (int i = 0; i < args.length; i++) {
            entry.add(new InsnNode(Opcodes.DUP));
            entry.add(new IntInsnNode(Opcodes.SIPUSH, i));
            entry.add(new VarInsnNode(args[i].getOpcode(Opcodes.ILOAD), slot));
            box(entry, args[i]);
            entry.add(new InsnNode(Opcodes.AASTORE));
            slot += args[i].getSize();
        }
        entry.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SELF, "enter", "(Ljava/lang/String;Ljava/lang/Object;[Ljava/lang/Object;)V", false));
        method.instructions.insert(entry);
        Type result = Type.getReturnType(method.desc);
        for (AbstractInsnNode insn : method.instructions.toArray()) {
            int op = insn.getOpcode();
            InsnList exit = new InsnList();
            if (op >= Opcodes.IRETURN && op <= Opcodes.ARETURN) {
                exit.add(new InsnNode(result.getSize() == 2 ? Opcodes.DUP2 : Opcodes.DUP));
                box(exit, result);
            } else if (op == Opcodes.RETURN) {
                exit.add(new LdcInsnNode("(void)"));
            } else if (op == Opcodes.ATHROW) {
                exit.add(new InsnNode(Opcodes.DUP));
                exit.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SELF, "thrown", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
            } else continue;
            exit.add(new LdcInsnNode(id));
            exit.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SELF, "exit", "(Ljava/lang/Object;Ljava/lang/String;)V", false));
            method.instructions.insertBefore(insn, exit);
        }
    }

    private static void box(InsnList list, Type type) {
        String owner;
        switch (type.getSort()) {
            case Type.BOOLEAN: owner = "java/lang/Boolean"; break;
            case Type.CHAR: owner = "java/lang/Character"; break;
            case Type.BYTE: owner = "java/lang/Byte"; break;
            case Type.SHORT: owner = "java/lang/Short"; break;
            case Type.INT: owner = "java/lang/Integer"; break;
            case Type.FLOAT: owner = "java/lang/Float"; break;
            case Type.LONG: owner = "java/lang/Long"; break;
            case Type.DOUBLE: owner = "java/lang/Double"; break;
            default: return;
        }
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "valueOf", "(" + type.getDescriptor() + ")L" + owner + ";", false));
    }

    private static String show(Object value) {
        if (value == null) return "null";
        String text;
        try {
            text = value instanceof Object[] ? java.util.Arrays.toString((Object[]) value) : String.valueOf(value);
        } catch (Throwable e) {
            text = value.getClass().getName() + "<" + e + ">";
        }
        if (text.startsWith(value.getClass().getName() + "@")) text = value.getClass().getSimpleName() + "@" + text.substring(text.indexOf('@') + 1);
        return text.length() > 160 ? text.substring(0, 160) + "..." : text;
    }

    public static void enter(String id, Object self, Object[] args) {
        StringBuilder line = new StringBuilder(id).append('(');
        for (int i = 0; i < args.length; i++) line.append(i == 0 ? "" : ", ").append(show(args[i]));
        CALLS.get().push(new String[] {id, line.append(')').toString(), self == null ? "" : " on " + show(self)});
    }

    public static Object thrown(Object error) {
        return "threw " + error;
    }

    public static void exit(Object result, String id) {
        Deque<String[]> calls = CALLS.get();
        String[] call = null;
        while (!calls.isEmpty()) {
            call = calls.pop();
            if (call[0].equals(id)) break;
        }
        if (call == null) return;
        String line = call[1] + " -> " + show(result);
        if (LOGGED.size() < 5000 && LOGGED.add(line + call[2])) LOGGER.info("{}{}", line, call[2]);
    }

    private DevTrace() {}
}
