package com.michaelsebero.portium.mods116;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Gives event classes loaded for 1.13 mods what Forge 1.12's event bus expects of its events, as Forge 1.12's own
 * transformer does for classes the game loads: a listener list of their own, a bare constructor (the bus makes one
 * to find that list), and {@code isCancelable}/{@code hasResult} answering their annotations. Applies to Forge 1.13's
 * events that 1.12 lacks and to mods' own events, which extend 1.12's events once translated.
 */
final class ModernEventClasses {

    private static final String EVENT = "net/minecraftforge/fml/common/eventhandler/Event";
    private static final String LISTENER_LIST = "net/minecraftforge/fml/common/eventhandler/ListenerList";
    private static final String LIST_DESC = "L" + LISTENER_LIST + ";";
    private static final String CANCELABLE = "Lnet/minecraftforge/fml/common/eventhandler/Cancelable;";
    private static final String HAS_RESULT = "Lnet/minecraftforge/fml/common/eventhandler/Event$HasResult;";

    static boolean isEventClass(ClassNode node, ModernRedirects.Hierarchy hierarchy) {
        return !EVENT.equals(node.name) && node.superName != null && (node.access & Opcodes.ACC_INTERFACE) == 0
                && hierarchy.isTranslatedSubtype(node.superName, EVENT);
    }

    static void prepare(ClassNode node) {
        if (!has(node, "setup", "()V")) addListenerList(node);
        if (!has(node, "<init>", "()V")) addConstructor(node);
        if (annotated(node, CANCELABLE) && !has(node, "isCancelable", "()Z")) addTrue(node, "isCancelable");
        if (annotated(node, HAS_RESULT) && !has(node, "hasResult", "()Z")) addTrue(node, "hasResult");
    }

    private static void addListenerList(ClassNode node) {
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "LISTENER_LIST", LIST_DESC, null, null));

        // protected void setup() { super.setup(); if (LISTENER_LIST == null) LISTENER_LIST = new ListenerList(super.getListenerList()); }
        MethodNode setup = new MethodNode(Opcodes.ACC_PROTECTED, "setup", "()V", null, null);
        InsnList code = setup.instructions;
        LabelNode done = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "setup", "()V", false));
        code.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, "LISTENER_LIST", LIST_DESC));
        code.add(new JumpInsnNode(Opcodes.IFNONNULL, done));
        code.add(new TypeInsnNode(Opcodes.NEW, LISTENER_LIST));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "getListenerList", "()" + LIST_DESC, false));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, LISTENER_LIST, "<init>", "(" + LIST_DESC + ")V", false));
        code.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, "LISTENER_LIST", LIST_DESC));
        code.add(done);
        code.add(new FrameNode(Opcodes.F_NEW, 1, new Object[] {node.name}, 0, new Object[0]));
        code.add(new InsnNode(Opcodes.RETURN));
        setup.maxStack = 4;
        setup.maxLocals = 1;
        node.methods.add(setup);

        MethodNode get = new MethodNode(Opcodes.ACC_PUBLIC, "getListenerList", "()" + LIST_DESC, null, null);
        get.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, "LISTENER_LIST", LIST_DESC));
        get.instructions.add(new InsnNode(Opcodes.ARETURN));
        get.maxStack = 1;
        get.maxLocals = 1;
        node.methods.add(get);
    }

    private static void addConstructor(ClassNode node) {
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.maxStack = 1;
        constructor.maxLocals = 1;
        node.methods.add(constructor);
    }

    private static void addTrue(ClassNode node, String name) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, name, "()Z", null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
        node.methods.add(method);
    }

    private static boolean has(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return true;
        }
        return false;
    }

    private static boolean annotated(ClassNode node, String desc) {
        if (node.visibleAnnotations == null) return false;
        for (AnnotationNode annotation : node.visibleAnnotations) {
            if (annotation.desc.equals(desc)) return true;
        }
        return false;
    }

    private ModernEventClasses() {}
}
