package com.michaelsebero.portium.mods116;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A development aid, run with {@code -Dportium.forge116.verify=true} once the mods are constructed: translates every
 * class in the 1.16 mod jars and checks each method's types as the JVM's verifier would, logging every class that
 * would fail to load (a value of one class where 1.12 wants another), instead of finding them one load at a time.
 */
final class ModernVerify {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");

    static void run(ModernClassLoader loader, Iterable<File> jars) {
        for (File jar : jars) {
            Set<String> problems = new TreeSet<>();
            int classes = 0;
            try (JarFile zip = new JarFile(jar)) {
                Enumeration<JarEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class") || entry.getName().startsWith("META-INF/")) continue;
                    byte[] bytes;
                    try (InputStream in = zip.getInputStream(entry)) {
                        bytes = readAll(in);
                    }
                    classes++;
                    try {
                        verify(loader, loader.translator().translate(bytes), problems);
                    } catch (RuntimeException | LinkageError e) {
                        problems.add(entry.getName() + ": couldn't translate: " + e);
                    }
                }
            } catch (IOException e) {
                LOGGER.warn("Couldn't verify {}", jar.getName(), e);
            }
            LOGGER.info("Verified {} classes of {}: {} problems", classes, jar.getName(), problems.size());
            for (String problem : problems) LOGGER.info("  verify {}", problem);
        }
    }

    private static void verify(ModernClassLoader loader, byte[] bytes, Set<String> problems) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        Type self = Type.getObjectType(node.name);
        Type superType = node.superName == null ? null : Type.getObjectType(node.superName);
        List<Type> interfaces = new ArrayList<>();
        for (String name : node.interfaces) interfaces.add(Type.getObjectType(name));
        boolean isInterface = (node.access & Opcodes.ACC_INTERFACE) != 0;
        for (MethodNode method : node.methods) {
            if (method.instructions.size() == 0) continue;
            SimpleVerifier verifier = new SimpleVerifier(self, superType, interfaces, isInterface);
            verifier.setClassLoader(loader);
            try {
                new Analyzer<BasicValue>(verifier).analyze(node.name, method);
            } catch (Exception | LinkageError e) {
                String message = String.valueOf(e.getMessage());
                Throwable cause = e.getCause();
                if (cause != null && cause != e) message += " (" + cause + ")";
                problems.add(node.name + "." + method.name + method.desc + ": " + message.replace('\n', ' '));
            }
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int read; (read = in.read(buffer)) > 0; ) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    private ModernVerify() {}
}
