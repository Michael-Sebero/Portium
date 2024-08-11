package com.michaelsebero.portium.mods116;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Forge 1.16's patches to Minecraft 1.16.5's classes that 1.16 mods rely on, made to the copies Portium runs (Mojang's
 * classes, unpatched), by SRG names as they're read. Only what's needed is redone, each from what Forge's patch does.
 */
final class ForgePatches116 {

    private static final String BUFFER_BUILDER = "net/minecraft/client/renderer/BufferBuilder";
    private static final String VERTEX_FORMAT = "Lnet/minecraft/client/renderer/vertex/VertexFormat;";

    static void apply(ClassNode node) {
        if (BUFFER_BUILDER.equals(node.name)) bufferBuilder(node);
        if (MATRIX_4F.equals(node.name)) matrix4f(node);
        if (REGISTRY_KEY.equals(node.name)) registryKey(node);
    }

    private static final String REGISTRY_KEY = "net/minecraft/util/RegistryKey";

    /** Forge's {@code getRegistryName()} of a registry key: the name of the registry it's a key of. */
    private static void registryKey(ClassNode node) {
        if (declares(node, "getRegistryName", "()Lnet/minecraft/util/ResourceLocation;")) return;
        MethodNode name = new MethodNode(Opcodes.ACC_PUBLIC, "getRegistryName", "()Lnet/minecraft/util/ResourceLocation;", null, null);
        name.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        name.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, REGISTRY_KEY, "field_240899_b_", "Lnet/minecraft/util/ResourceLocation;"));
        name.instructions.add(new InsnNode(Opcodes.ARETURN));
        name.maxStack = 1;
        name.maxLocals = 1;
        node.methods.add(name);
    }

    private static final String MATRIX_4F = "net/minecraft/util/math/vector/Matrix4f";
    /** Matrix4f's sixteen floats, m00 to m33 row by row, by SRG name. */
    private static final String[] MATRIX_4F_FIELDS = {"field_226575_a_", "field_226576_b_", "field_226577_c_", "field_226578_d_", "field_226579_e_",
            "field_226580_f_", "field_226581_g_", "field_226582_h_", "field_226583_i_", "field_226584_j_", "field_226585_k_", "field_226586_l_",
            "field_226587_m_", "field_226588_n_", "field_226589_o_", "field_226590_p_"};

    /**
     * Forge's additions to the 4x4 matrix, which render libraries use (Flywheel works out its view projection with
     * them): a constructor from sixteen floats row by row, {@code set} copying another matrix, {@code multiplyBackward}
     * (this becomes other times this) and {@code setTranslation}.
     */
    private static void matrix4f(ClassNode node) {
        String self = "L" + MATRIX_4F + ";";
        if (!declares(node, "<init>", "([F)V")) {
            MethodNode init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "([F)V", null, null);
            init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
            for (int i = 0; i < 16; i++) {
                init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                init.instructions.add(new org.objectweb.asm.tree.IntInsnNode(Opcodes.BIPUSH, i));
                init.instructions.add(new InsnNode(Opcodes.FALOAD));
                init.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, MATRIX_4F, MATRIX_4F_FIELDS[i], "F"));
            }
            init.instructions.add(new InsnNode(Opcodes.RETURN));
            init.maxStack = 3;
            init.maxLocals = 2;
            node.methods.add(init);
        }
        if (!declares(node, "set", "(" + self + ")V")) {
            MethodNode set = new MethodNode(Opcodes.ACC_PUBLIC, "set", "(" + self + ")V", null, null);
            for (String field : MATRIX_4F_FIELDS) {
                set.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                set.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                set.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, MATRIX_4F, field, "F"));
                set.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, MATRIX_4F, field, "F"));
            }
            set.instructions.add(new InsnNode(Opcodes.RETURN));
            set.maxStack = 2;
            set.maxLocals = 2;
            node.methods.add(set);
        }
        if (!declares(node, "multiplyBackward", "(" + self + ")V")) {
            // Matrix4f copy = other.copy(); copy.mul(this); this.set(copy);
            MethodNode backward = new MethodNode(Opcodes.ACC_PUBLIC, "multiplyBackward", "(" + self + ")V", null, null);
            backward.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            backward.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, MATRIX_4F, "func_226601_d_", "()" + self, false));
            backward.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));
            backward.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            backward.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            backward.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, MATRIX_4F, "func_226595_a_", "(" + self + ")V", false));
            backward.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            backward.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            backward.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, MATRIX_4F, "set", "(" + self + ")V", false));
            backward.instructions.add(new InsnNode(Opcodes.RETURN));
            backward.maxStack = 2;
            backward.maxLocals = 3;
            node.methods.add(backward);
        }
        if (!declares(node, "setTranslation", "(FFF)V")) {
            MethodNode translation = new MethodNode(Opcodes.ACC_PUBLIC, "setTranslation", "(FFF)V", null, null);
            int[][] slots = {{1, 3}, {2, 7}, {3, 11}};
            for (int[] slot : slots) {
                translation.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                translation.instructions.add(new VarInsnNode(Opcodes.FLOAD, slot[0]));
                translation.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, MATRIX_4F, MATRIX_4F_FIELDS[slot[1]], "F"));
            }
            translation.instructions.add(new InsnNode(Opcodes.RETURN));
            translation.maxStack = 2;
            translation.maxLocals = 4;
            node.methods.add(translation);
        }
    }

    private static boolean declares(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return true;
        }
        return false;
    }

    /**
     * The buffer builder's finished vertices come as a slice of its buffer, which Java makes big-endian whatever the
     * buffer's order; Forge keeps the buffer's order, as mods read the vertices back (Flywheel's reader). Forge also
     * adds {@code getVertexFormat()}.
     */
    private static void bufferBuilder(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!"func_227832_f_".equals(method.name)) continue;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (!"java/nio/ByteBuffer".equals(call.owner) || !"slice".equals(call.name)) continue;
                MethodInsnNode nativeOrder = new MethodInsnNode(Opcodes.INVOKESTATIC, "java/nio/ByteOrder", "nativeOrder", "()Ljava/nio/ByteOrder;", false);
                MethodInsnNode order = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/nio/ByteBuffer", "order", "(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;", false);
                method.instructions.insert(call, order);
                method.instructions.insert(call, nativeOrder);
                method.maxStack += 1;
                break;
            }
        }
        boolean hasFormatGetter = false;
        for (MethodNode method : node.methods) hasFormatGetter |= "getVertexFormat".equals(method.name);
        if (!hasFormatGetter) {
            MethodNode getter = new MethodNode(Opcodes.ACC_PUBLIC, "getVertexFormat", "()" + VERTEX_FORMAT, null, null);
            getter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            getter.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, "field_179011_q", VERTEX_FORMAT));
            getter.instructions.add(new InsnNode(Opcodes.ARETURN));
            getter.maxStack = 1;
            getter.maxLocals = 1;
            node.methods.add(getter);
        }
    }

    private ForgePatches116() {}
}
