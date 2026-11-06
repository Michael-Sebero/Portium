package com.michaelsebero.portium.world.gen.jigsaw;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads what a class's static initializer builds, without running it: a small symbolic interpreter over its
 * bytecode. Constants stay constants; {@code new X(args)} becomes a {@link Built} of X with its arguments; static
 * fields read are {@link FieldRef}s; static calls are given to a {@link Hooks} that may answer them (lists, pairs,
 * registrations) or not ({@link Opaque}). Portium reads 1.16.5's worldgen data this way from the official jar, which
 * keeps it in code (template pools, processor lists), so none of it is copied into Portium.
 */
public final class StaticInitReader {

    /** An object built with {@code new}: its class and constructor arguments. */
    public static final class Built {
        public final String type;
        public final List<Object> args;

        Built(String type, List<Object> args) {
            this.type = type;
            this.args = args;
        }

        @Override
        public String toString() {
            return "new " + this.type + this.args;
        }
    }

    /** {@code Pair.of(left, right)}. */
    public static final class PairValue {
        public final Object left;
        public final Object right;

        PairValue(Object left, Object right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public String toString() {
            return "(" + this.left + ", " + this.right + ")";
        }
    }

    /** A static field read. */
    public static final class FieldRef {
        public final String owner;
        public final String name;

        FieldRef(String owner, String name) {
            this.owner = owner;
            this.name = name;
        }

        @Override
        public String toString() {
            return this.owner + "." + this.name;
        }
    }

    /** A call nobody answered, with its receiver (or null) and arguments. */
    public static final class Opaque {
        public final String owner;
        public final String name;
        @Nullable
        public final Object receiver;
        public final List<Object> args;

        Opaque(String owner, String name, @Nullable Object receiver, List<Object> args) {
            this.owner = owner;
            this.name = name;
            this.receiver = receiver;
            this.args = args;
        }

        @Override
        public String toString() {
            return (this.receiver != null ? this.receiver + "." : this.owner + ".") + this.name + this.args;
        }
    }

    private static final class Uninitialized {
        final String type;

        Uninitialized(String type) {
            this.type = type;
        }
    }

    private static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    /** Answers calls; returns {@link #NO_ANSWER} to leave a call opaque. */
    public interface Hooks {
        Object call(MethodInsnNode call, @Nullable Object receiver, List<Object> args);

        default void putStatic(String owner, String name, Object value) {}
    }

    public static final Object NO_ANSWER = new Object();

    /** Runs a class's static initializer symbolically; returns the static fields it set. */
    public static Map<String, Object> read(byte[] classBytes, Hooks hooks) {
        ClassNode node = new ClassNode();
        new ClassReader(classBytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Map<String, Object> statics = new HashMap<>();
        for (MethodNode method : node.methods) {
            if (method.name.equals("<clinit>")) run(node.name, method, hooks, statics);
        }
        return statics;
    }

    private static void run(String owner, MethodNode method, Hooks hooks, Map<String, Object> statics) {
        Deque<Object> stack = new ArrayDeque<>();
        Object[] locals = new Object[Math.max(method.maxLocals, 1)];
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            int op = insn.getOpcode();
            if (op < 0) continue;
            try {
                step(owner, insn, op, stack, locals, hooks, statics);
            } catch (RuntimeException e) {
                // An instruction the reader doesn't follow (branches, maths): start over clean from the next statement.
                stack.clear();
            }
            if (op == Opcodes.RETURN) break;
        }
    }

    private static void step(String owner, AbstractInsnNode insn, int op, Deque<Object> stack, Object[] locals, Hooks hooks, Map<String, Object> statics) {
        switch (op) {
            case Opcodes.ACONST_NULL:
                stack.push(NULL);
                return;
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                stack.push(op - Opcodes.ICONST_0);
                return;
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
                stack.push((long) (op - Opcodes.LCONST_0));
                return;
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
                stack.push((float) (op - Opcodes.FCONST_0));
                return;
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                stack.push((double) (op - Opcodes.DCONST_0));
                return;
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
                stack.push(((IntInsnNode) insn).operand);
                return;
            case Opcodes.LDC:
                Object constant = ((LdcInsnNode) insn).cst;
                stack.push(constant instanceof Type ? ((Type) constant).getInternalName() : constant);
                return;
            case Opcodes.ALOAD:
            case Opcodes.ILOAD:
            case Opcodes.FLOAD:
                Object local = locals[((VarInsnNode) insn).var];
                stack.push(local == null ? NULL : local);
                return;
            case Opcodes.ASTORE:
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
                locals[((VarInsnNode) insn).var] = stack.pop();
                return;
            case Opcodes.DUP:
                stack.push(stack.peek());
                return;
            case Opcodes.POP:
                stack.pop();
                return;
            case Opcodes.CHECKCAST:
                return;
            case Opcodes.NEW:
                stack.push(new Uninitialized(((TypeInsnNode) insn).desc));
                return;
            case Opcodes.ANEWARRAY: {
                int size = (Integer) stack.pop();
                stack.push(new Object[size]);
                return;
            }
            case Opcodes.AASTORE: {
                Object value = stack.pop();
                int index = (Integer) stack.pop();
                Object[] array = (Object[]) stack.pop();
                array[index] = value;
                return;
            }
            case Opcodes.GETSTATIC: {
                FieldInsnNode field = (FieldInsnNode) insn;
                if (field.owner.equals(owner) && statics.containsKey(field.name)) {
                    stack.push(statics.get(field.name));
                } else {
                    stack.push(new FieldRef(field.owner, field.name));
                }
                return;
            }
            case Opcodes.PUTSTATIC: {
                FieldInsnNode field = (FieldInsnNode) insn;
                Object value = stack.pop();
                if (field.owner.equals(owner)) statics.put(field.name, value);
                hooks.putStatic(field.owner, field.name, value);
                return;
            }
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKEINTERFACE:
            case Opcodes.INVOKESPECIAL: {
                MethodInsnNode call = (MethodInsnNode) insn;
                Type[] argTypes = Type.getArgumentTypes(call.desc);
                Object[] args = new Object[argTypes.length];
                for (int i = args.length - 1; i >= 0; i--) args[i] = stack.pop();
                List<Object> argList = new ArrayList<>(Arrays.asList(args));
                if (op == Opcodes.INVOKESPECIAL && call.name.equals("<init>")) {
                    Object target = stack.pop();
                    Built built = new Built(call.owner, argList);
                    // The duplicate left by DUP is the object itself once built.
                    if (!stack.isEmpty() && stack.peek() == target) {
                        stack.pop();
                        stack.push(built);
                    }
                    return;
                }
                Object receiver = op == Opcodes.INVOKESTATIC ? null : stack.pop();
                Object answer = hooks.call(call, receiver, argList);
                if (answer == NO_ANSWER) answer = standard(call, receiver, argList);
                if (Type.getReturnType(call.desc) != Type.VOID_TYPE) stack.push(answer == null ? NULL : answer);
                return;
            }
            case Opcodes.INVOKEDYNAMIC: {
                org.objectweb.asm.tree.InvokeDynamicInsnNode indy = (org.objectweb.asm.tree.InvokeDynamicInsnNode) insn;
                Type[] argTypes = Type.getArgumentTypes(indy.desc);
                for (int i = 0; i < argTypes.length; i++) stack.pop();
                stack.push(new Opaque("indy", indy.name, null, new ArrayList<>()));
                return;
            }
            default:
                // Anything else (maths, branches) isn't followed: throw so this statement is skipped.
                throw new IllegalStateException("unhandled opcode " + op);
        }
    }

    /** Boxing, lists and pairs as themselves; anything else opaque. */
    private static Object standard(MethodInsnNode call, @Nullable Object receiver, List<Object> args) {
        String owner = call.owner;
        if (call.name.equals("valueOf") && owner.startsWith("java/lang/") && args.size() == 1) return args.get(0);
        if (owner.equals("com/google/common/collect/ImmutableList") && call.name.equals("of")) {
            List<Object> list = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof Object[]) {
                    list.addAll(Arrays.asList((Object[]) arg));
                } else {
                    list.add(arg);
                }
            }
            return list;
        }
        if ((owner.equals("com/mojang/datafixers/util/Pair") && call.name.equals("of"))) return new PairValue(args.get(0), args.get(1));
        return new Opaque(owner, call.name, receiver, args);
    }

    public static boolean isNull(Object value) {
        return value == NULL;
    }

    private StaticInitReader() {}
}
