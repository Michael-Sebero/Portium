package com.michaelsebero.portium.modern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A development aid, run with {@code -Dportium.modern.audit=true}: translates every class in the 1.13 mod jars and
 * logs the methods and fields they use that still don't exist on 1.12 once translated. Members of classes the mods
 * bring or Forge 1.13 provides aren't checked; those come along with their classes.
 */
final class ModernAudit {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");

    static void run(ModernClassLoader loader, Iterable<File> jars) {
        for (File jar : jars) {
            Set<String> missing = new TreeSet<>();
            try (JarFile zip = new JarFile(jar)) {
                Enumeration<JarEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) continue;
                    byte[] bytes;
                    try (InputStream in = zip.getInputStream(entry)) {
                        bytes = readAll(in);
                    }
                    ClassNode node = new ClassNode();
                    new ClassReader(loader.translator().translateForScan(bytes)).accept(node, ClassReader.SKIP_FRAMES);
                    for (MethodNode method : node.methods) {
                        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                            if (insn instanceof MethodInsnNode) {
                                MethodInsnNode call = (MethodInsnNode) insn;
                                if (!resolves(loader, call.owner, call.name, call.desc, true)) missing.add(describe(node, call.owner, call.name, call.desc));
                            } else if (insn instanceof FieldInsnNode) {
                                FieldInsnNode field = (FieldInsnNode) insn;
                                if (!resolves(loader, field.owner, field.name, field.desc, false)) missing.add(describe(node, field.owner, field.name, field.desc));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Couldn't audit {}", jar.getName(), e);
            }
            LOGGER.info("Audit of {}: {} missing", jar.getName(), missing.size());
            for (String line : missing) LOGGER.info("  missing {}", line);
        }
        auditBridges(loader);
    }

    /**
     * Bridges hold 1.13.2's own method bodies, made to run on 1.12: what those use must exist too. Bridges they need
     * in turn are made as they're translated, so this goes on until no new ones appear.
     */
    private static void auditBridges(ModernClassLoader loader) {
        ModernBridges bridges = loader.bridges();
        if (bridges == null) return;
        Set<String> done = new HashSet<>();
        Set<String> missing = new TreeSet<>();
        for (boolean more = true; more; ) {
            more = false;
            for (String name : bridges.names()) {
                if (!done.add(name)) continue;
                more = true;
                byte[] bytes = bridges.generate(name);
                if (bytes == null) continue;
                ClassNode node = new ClassNode();
                try {
                    new ClassReader(loader.translator().translateForScan(bytes)).accept(node, ClassReader.SKIP_FRAMES);
                } catch (RuntimeException e) {
                    missing.add("(couldn't translate the bridge for " + bridges.describe(name) + ": " + e + ")");
                    continue;
                }
                String user = "bridge for " + bridges.describe(name);
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                        if (insn instanceof MethodInsnNode) {
                            MethodInsnNode call = (MethodInsnNode) insn;
                            if (!resolves(loader, call.owner, call.name, call.desc, true)) missing.add(call.owner + "." + call.name + " " + call.desc + "  (" + user + ")");
                        } else if (insn instanceof FieldInsnNode) {
                            FieldInsnNode field = (FieldInsnNode) insn;
                            if (!resolves(loader, field.owner, field.name, field.desc, false)) missing.add(field.owner + "." + field.name + " " + field.desc + "  (" + user + ")");
                        }
                    }
                }
            }
        }
        LOGGER.info("Audit of {} bridges to 1.13.2's code: {} missing", done.size(), missing.size());
        for (String line : missing) LOGGER.info("  missing {}", line);
    }

    private static String describe(ClassNode user, String owner, String name, String desc) {
        return owner + "." + name + " " + desc + "  (" + user.name.substring(user.name.lastIndexOf('/') + 1) + ")";
    }

    /** Whether a member resolves from {@code owner}; members reached through the mods' own classes are looked for further up. */
    private static boolean resolves(ModernClassLoader loader, String owner, String name, String desc, boolean method) {
        // Bridges are made as they're loaded, holding exactly what's asked of them.
        if (owner.startsWith("[") || owner.startsWith("com/michaelsebero/portium/modern/bridge/")) return true;
        String type = owner;
        // Walk the mods' own classes up to the first class of the parent's.
        while (loader.ownsTranslated(type)) {
            if (loader.declares(type, name, desc)) return true;
            String parent = loader.superclassOf(type);
            if (parent == null) return false;
            type = parent;
        }
        Class<?> found;
        try {
            found = Class.forName(type.replace('/', '.'), false, loader.getParent());
        } catch (ClassNotFoundException | LinkageError | RuntimeException e) {
            return false;
        }
        try {
            return method ? hasMethod(found, name, desc) : hasField(found, name, desc);
        } catch (LinkageError | RuntimeException e) {
            return true;
        }
    }

    private static boolean hasMethod(Class<?> type, String name, String desc) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (declaresMethod(c, name, desc)) return true;
            for (Class<?> itf : allInterfaces(c)) {
                if (declaresMethod(itf, name, desc)) return true;
            }
        }
        return type.isInterface() && declaresMethod(Object.class, name, desc);
    }

    private static Set<Class<?>> allInterfaces(Class<?> type) {
        Set<Class<?>> all = new HashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>(Arrays.asList(type.getInterfaces()));
        while (!queue.isEmpty()) {
            Class<?> next = queue.poll();
            if (all.add(next)) queue.addAll(Arrays.asList(next.getInterfaces()));
        }
        return all;
    }

    private static boolean declaresMethod(Class<?> type, String name, String desc) {
        if (name.equals("<init>")) {
            for (Constructor<?> constructor : type.getDeclaredConstructors()) {
                if (Type.getConstructorDescriptor(constructor).equals(desc)) return true;
            }
            return false;
        }
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && Type.getMethodDescriptor(method).equals(desc)) return true;
        }
        return false;
    }

    private static boolean hasField(Class<?> type, String name, String desc) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getName().equals(name) && Type.getDescriptor(field.getType()).equals(desc)) return true;
            }
            for (Class<?> itf : allInterfaces(c)) {
                for (Field field : itf.getDeclaredFields()) {
                    if (field.getName().equals(name) && Type.getDescriptor(field.getType()).equals(desc)) return true;
                }
            }
        }
        return false;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private ModernAudit() {}
}
