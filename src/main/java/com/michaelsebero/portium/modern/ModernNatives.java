package com.michaelsebero.portium.modern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Minecraft 1.13.2's own classes, for the 1.13 classes neither 1.12 nor Portium has: read from Mojang's client jar
 * and given their SRG names from MCPConfig, as mods are compiled against, before {@link ModernTranslator} makes them
 * link against 1.12 like any mod class. So a mod's {@code BlockFlowingFluid} is 1.13's, extending Portium's 1.13
 * {@code Block}, and the many small classes 1.13 added (property values, shapes, helpers) behave as they did.
 */
final class ModernNatives {

    private final File file;
    private final JarFile jar;
    /** Obfuscated to SRG class names, and back. */
    private final Map<String, String> classes = new HashMap<>();
    private final Map<String, String> obfuscated = new HashMap<>();
    /** Per obfuscated class: obfuscated method name and descriptor, and field name, to SRG name. */
    private final Map<String, Map<String, String>> members = new HashMap<>();
    /** Obfuscated supertypes (superclass first) of obfuscated classes, read from the jar as needed. */
    private final Map<String, String[]> supertypes = new HashMap<>();

    private final Remapper names = new Remapper() {

        @Override
        public String map(String type) {
            return ModernNatives.this.classes.getOrDefault(type, type);
        }

        @Override
        public String mapMethodName(String owner, String name, String desc) {
            if (name.startsWith("<")) return name;
            String srg = find(owner, name + desc);
            return srg != null ? srg : name;
        }

        @Override
        public String mapFieldName(String owner, String name, String desc) {
            String srg = find(owner, name);
            return srg != null ? srg : name;
        }
    };

    ModernNatives(File minecraft, File mcpConfig) throws IOException {
        this.file = minecraft;
        this.jar = new JarFile(minecraft, false);
        try (ZipFile zip = new ZipFile(mcpConfig)) {
            ZipEntry entry = zip.getEntry("config/joined.tsrg");
            if (entry == null) throw new IOException(mcpConfig.getName() + " has no config/joined.tsrg");
            try (InputStream in = zip.getInputStream(entry)) {
                readTsrg(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        }
    }

    File file() {
        return this.file;
    }

    /** Whether Minecraft 1.13.2 has a class, by its SRG (internal) name. */
    boolean has(String name) {
        return this.obfuscated.containsKey(name);
    }

    /** A class's bytes with SRG names, by its SRG (internal) name, or null if 1.13.2 has no such class. */
    @Nullable
    synchronized byte[] read(String name) throws IOException {
        String obf = this.obfuscated.get(name);
        if (obf == null) return null;
        byte[] bytes = entry(obf);
        if (bytes == null) return null;
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassRemapper(writer, this.names), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    /** joined.tsrg: {@code obf srg} per class, then {@code \tobf srg} per field and {@code \tobf desc srg} per method. */
    private void readTsrg(BufferedReader reader) throws IOException {
        Map<String, String> current = null;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) continue;
            if (line.charAt(0) != '\t') {
                String[] parts = line.split(" ");
                this.classes.put(parts[0], parts[1]);
                this.obfuscated.put(parts[1], parts[0]);
                current = this.members.computeIfAbsent(parts[0], key -> new HashMap<>());
                continue;
            }
            if (current == null) continue;
            String[] parts = line.substring(1).split(" ");
            if (parts.length == 2) {
                current.put(parts[0], parts[1]);
            } else if (parts.length == 3) {
                current.put(parts[0] + parts[1], parts[2]);
            }
        }
    }

    /** The SRG name of a member reached through an obfuscated class: declared there or in a supertype. */
    @Nullable
    private String find(String owner, String key) {
        Map<String, String> declared = this.members.get(owner);
        if (declared != null) {
            String srg = declared.get(key);
            if (srg != null) return srg;
        }
        for (String parent : supertypes(owner)) {
            String srg = find(parent, key);
            if (srg != null) return srg;
        }
        return null;
    }

    private String[] supertypes(String owner) {
        String[] known = this.supertypes.get(owner);
        if (known != null) return known;
        String[] found = new String[0];
        try {
            byte[] bytes = this.members.containsKey(owner) ? entry(owner) : null;
            if (bytes != null) {
                ClassReader reader = new ClassReader(bytes);
                String[] interfaces = reader.getInterfaces();
                found = new String[interfaces.length + 1];
                found[0] = reader.getSuperName();
                System.arraycopy(interfaces, 0, found, 1, interfaces.length);
                if (found[0] == null) found[0] = "java/lang/Object";
            }
        } catch (IOException e) {
            found = new String[0];
        }
        this.supertypes.put(owner, found);
        return found;
    }

    @Nullable
    private byte[] entry(String obf) throws IOException {
        ZipEntry entry = this.jar.getEntry(obf + ".class");
        if (entry == null) return null;
        try (InputStream in = this.jar.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(in.available(), 4096));
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toByteArray();
        }
    }
}
