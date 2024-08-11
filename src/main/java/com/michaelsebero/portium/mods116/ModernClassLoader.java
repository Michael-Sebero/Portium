package com.michaelsebero.portium.mods116;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Loads 1.13.2 mods, Forge 1.13.2 and Portium's glue for them, each class translated to link against 1.12.2.
 * <p>
 * Forge 1.13's classes are loaded here under {@code forge113.net.minecraftforge} (see {@link ModernMappings}); 1.12
 * code never sees them. So are the libraries Forge 1.13 brought that 1.12 lacks, Portium's glue ({@code modern.host}),
 * and anything inside a mod jar. Everything else, Minecraft, Forge 1.12 and Portium's stand-ins included, comes from
 * the parent, except Minecraft 1.13 classes the parent doesn't have, which are Minecraft 1.13.2's own
 * ({@link ModernNatives}).
 */
final class ModernClassLoader extends ClassLoader implements ModernRedirects.Hierarchy {

    static {
        registerAsParallelCapable();
    }

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    private static final String FORGE = ModernMappings.FORGE.replace('/', '.');
    private static final String NATIVE = ModernMappings.NATIVE.replace('/', '.');
    private static final String[] OWN = {FORGE, "com.electronwill.nightconfig.", "net.jodah.typetools.", "com.mojang.brigadier.",
            "com.mojang.datafixers.", "com.mojang.serialization.", "cpw.mods.modlauncher.", "com.michaelsebero.portium.mods116.host.",
            // 1.16's fastutil (8), apart from 1.12's (7).
            "it.unimi.dsi.fastutil."};

    /** A jar or classes directory. */
    private static final class Source {

        final File file;
        @Nullable
        final JarFile jar;
        @Nullable
        final Manifest manifest;
        final ProtectionDomain domain;

        Source(File file) throws IOException {
            this.file = file;
            this.jar = file.isDirectory() ? null : new JarFile(file, false);
            this.manifest = this.jar == null ? null : this.jar.getManifest();
            this.domain = new ProtectionDomain(new CodeSource(file.toURI().toURL(), (Certificate[]) null), null);
        }

        boolean has(String path) {
            return this.jar != null ? this.jar.getEntry(path) != null : new File(this.file, path).isFile();
        }

        @Nullable
        byte[] read(String path) throws IOException {
            if (this.jar != null) {
                ZipEntry entry = this.jar.getEntry(path);
                if (entry == null) return null;
                try (InputStream in = this.jar.getInputStream(entry)) {
                    return readAll(in);
                }
            }
            File file = new File(this.file, path);
            if (!file.isFile()) return null;
            try (InputStream in = new FileInputStream(file)) {
                return readAll(in);
            }
        }

        URL url(String path) throws MalformedURLException {
            return this.jar != null ? new URL("jar:" + this.file.toURI().toURL() + "!/" + path) : new File(this.file, path).toURI().toURL();
        }
    }

    /** Forge 1.13, its libraries and Portium's glue; searched for {@link #OWN} packages. */
    private final List<Source> runtime = new ArrayList<>();
    /** 1.13 mod jars, searched first for anything outside Minecraft. */
    private final List<Source> mods = new ArrayList<>();
    @Nullable
    private final ModernNatives natives;
    @Nullable
    private final Source nativeSource;
    @Nullable
    private final ModernBridges bridges;
    private final ModernMappings mappings;
    private final ModernRedirects redirects;
    private final ModernTranslator translator;
    private final Map<String, String[]> supertypes = new HashMap<>();
    private final Map<String, Boolean> existing = new HashMap<>();

    ModernClassLoader(ClassLoader parent, List<File> runtime, List<File> mods, @Nullable ModernNatives natives, ModernMappings mappings) throws IOException {
        super(parent);
        for (File file : runtime) this.runtime.add(new Source(file));
        for (File file : mods) this.mods.add(new Source(file));
        this.natives = natives;
        this.nativeSource = natives == null ? null : new Source(natives.file());
        this.bridges = natives == null ? null : new ModernBridges(natives);
        this.mappings = mappings;
        mappings.setLegacyClasses(this::exists);
        mappings.setSupertypes(this::translatedSupertypes);
        this.redirects = new ModernRedirects();
        String dump = System.getProperty("portium.dev.redirects");
        if (dump != null) {
            try {
                this.redirects.dump(new File(dump));
            } catch (IOException e) {
                LOGGER.warn("Couldn't write the redirects to {}", dump, e);
            }
        }
        this.translator = new ModernTranslator(mappings, this.redirects, this, new ModernMembers(parent), this.bridges, Mods116.isClient());
    }

    ModernTranslator translator() {
        return this.translator;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                Source source = sourceOf(name);
                if (source != null) {
                    loaded = define(name, source);
                } else if (name.startsWith(NATIVE)) {
                    loaded = defineNative(name);
                } else if (ModernBridges.isBridgeName(name)) {
                    if (this.bridges == null) throw new ClassNotFoundException(name);
                    loaded = define(name, this.nativeSource, this.bridges.generate(name.replace('.', '/')));
                } else if (owns(name)) {
                    throw new ClassNotFoundException(name);
                } else {
                    try {
                        loaded = getParent().loadClass(name);
                    } catch (ClassNotFoundException e) {
                        if (!isMinecraft(name)) throw e;
                        loaded = hasNative(name) ? defineNative(name) : placeholder(name);
                    }
                }
            }
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    /** Whether a class (dotted name) is Minecraft's: its own packages, and Mojang's rendering library inside its jar. */
    private static boolean isMinecraft(String name) {
        return name.startsWith("net.minecraft.") || name.startsWith("com.mojang.blaze3d.");
    }

    static boolean owns(String name) {
        if (ModernBridges.isBridgeName(name)) return true;
        for (String prefix : OWN) {
            if (name.startsWith(prefix)) return true;
        }
        return false;
    }

    /** Where a class is found in its jar: Forge 1.13's under their original names. */
    private static String path(String name) {
        return (name.startsWith(FORGE) ? name.substring(FORGE.length()) : name).replace('.', '/') + ".class";
    }

    /** Where this loader finds a class, or null if the parent should load it. */
    @Nullable
    private Source sourceOf(String name) {
        if (name.startsWith("java.") || name.startsWith("net.minecraft.") || name.startsWith("net.minecraftforge.")) return null;
        String path = path(name);
        if (owns(name)) {
            for (Source source : this.runtime) {
                if (source.has(path)) return source;
            }
        }
        for (Source source : this.mods) {
            if (source.has(path)) return source;
        }
        return null;
    }

    private Class<?> define(String name, Source source) throws ClassNotFoundException {
        byte[] bytes;
        try {
            bytes = source.read(path(name));
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
        return define(name, source, bytes);
    }

    /** Whether Minecraft 1.13.2 has a class, by its 1.13 name, that the parent lacks. */
    private boolean hasNative(String name) {
        return this.natives != null && this.natives.has(nativeName(name));
    }

    /** A class's name in Minecraft 1.13.2: without the prefix of those moved aside for a 1.12 class of the same name. */
    private static String nativeName(String name) {
        return (name.startsWith(NATIVE) ? name.substring(NATIVE.length()) : name).replace('.', '/');
    }

    @Nullable
    private byte[] readNative(String name) throws IOException {
        return this.natives == null ? null : this.natives.read(nativeName(name));
    }

    private Class<?> defineNative(String name) throws ClassNotFoundException {
        if (this.nativeSource == null) throw new ClassNotFoundException(name);
        byte[] bytes;
        try {
            bytes = readNative(name);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
        return define(name, this.nativeSource, bytes == null ? null : widenAccess(bytes), name.startsWith(NATIVE) ? nativeName(name) : null);
    }

    /**
     * Makes a Minecraft 1.16.5 class and its members public, as mods' access transformers made what they use of them
     * on 1.16 (Create's makes {@code BlockTags.makeWrapperTag} public, for one). Final stays final.
     */
    private static byte[] widenAccess(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        node.access = widen(node.access);
        for (org.objectweb.asm.tree.InnerClassNode inner : node.innerClasses) inner.access = widen(inner.access);
        for (MethodNode method : node.methods) method.access = widen(method.access);
        for (FieldNode field : node.fields) field.access = widen(field.access);
        makeRegistryEntry(node);
        ForgePatches116.apply(node);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * The classes Forge 1.16 registers in its registries, which it patches to extend {@code ForgeRegistryEntry}, by
     * 1.16 name. Minecraft 1.16.5's own copies aren't patched; Portium does it as they load. (Fluids, particle types and
     * stat types are Portium's stand-ins, which are entries already.)
     */
    private static final Set<String> REGISTRY_TYPES = new HashSet<>(java.util.Arrays.asList(
            "net/minecraft/inventory/container/ContainerType", "net/minecraft/entity/item/PaintingType",
            "net/minecraft/entity/merchant/villager/VillagerProfession", "net/minecraft/village/PointOfInterestType",
            "net/minecraft/entity/ai/brain/memory/MemoryModuleType", "net/minecraft/entity/ai/brain/sensor/SensorType",
            "net/minecraft/entity/ai/brain/schedule/Schedule", "net/minecraft/entity/ai/brain/schedule/Activity",
            "net/minecraft/world/gen/carver/WorldCarver", "net/minecraft/world/gen/surfacebuilders/SurfaceBuilder",
            "net/minecraft/world/gen/feature/Feature", "net/minecraft/world/gen/placement/Placement",
            "net/minecraft/world/gen/feature/structure/Structure", "net/minecraft/world/gen/blockstateprovider/BlockStateProviderType",
            "net/minecraft/world/gen/blockplacer/BlockPlacerType", "net/minecraft/world/gen/foliageplacer/FoliagePlacerType",
            "net/minecraft/world/gen/treedecorator/TreeDecoratorType"));
    /** Interfaces Forge 1.16 makes registry entries; their implementations extend {@code ForgeRegistryEntry}. */
    private static final Set<String> REGISTRY_INTERFACES = Collections.singleton("net/minecraft/item/crafting/IRecipeSerializer");
    private static final String REGISTRY_ENTRY = "net/minecraftforge/registries/ForgeRegistryEntry";
    private static final String REGISTRY_ENTRY_INTERFACE = "net/minecraftforge/registries/IForgeRegistryEntry";

    /** Forge 1.16's patch: a registered class extends {@code ForgeRegistryEntry<itself>} (by 1.16 names). */
    private static void makeRegistryEntry(ClassNode node) {
        boolean isInterface = (node.access & Opcodes.ACC_INTERFACE) != 0;
        if (isInterface) {
            if (!REGISTRY_INTERFACES.contains(node.name) || node.interfaces.contains(REGISTRY_ENTRY_INTERFACE)) return;
            node.interfaces.add(REGISTRY_ENTRY_INTERFACE);
            if (node.signature != null) node.signature += "L" + REGISTRY_ENTRY_INTERFACE + "<" + entryType(node.name, node.signature) + ">;";
            return;
        }
        if (!"java/lang/Object".equals(node.superName)) return;
        String registered = REGISTRY_TYPES.contains(node.name) ? node.name : null;
        if (registered == null) {
            for (String type : node.interfaces) if (REGISTRY_INTERFACES.contains(type)) registered = type;
        }
        if (registered == null) return;
        node.superName = REGISTRY_ENTRY;
        String typeArgument = entryType(registered, registered.equals(node.name) ? node.signature : null);
        String superSignature = "L" + REGISTRY_ENTRY + "<" + typeArgument + ">;";
        if (node.signature == null) {
            StringBuilder signature = new StringBuilder(superSignature);
            for (String type : node.interfaces) signature.append('L').append(type).append(';');
            node.signature = signature.toString();
        } else {
            int start = node.signature.startsWith("<") ? endOfTypeParameters(node.signature) : 0;
            node.signature = node.signature.substring(0, start) + node.signature.substring(start).replaceFirst("^Ljava/lang/Object;", java.util.regex.Matcher.quoteReplacement(superSignature));
        }
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)) continue;
            int pending = 0;
            for (org.objectweb.asm.tree.AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.NEW && "java/lang/Object".equals(((org.objectweb.asm.tree.TypeInsnNode) insn).desc)) pending++;
                if (insn.getOpcode() != Opcodes.INVOKESPECIAL) continue;
                org.objectweb.asm.tree.MethodInsnNode call = (org.objectweb.asm.tree.MethodInsnNode) insn;
                if (!"<init>".equals(call.name) || !"java/lang/Object".equals(call.owner)) continue;
                if (pending > 0) {
                    pending--;
                } else {
                    call.owner = REGISTRY_ENTRY;
                    break;
                }
            }
        }
    }

    /** The registry's type as a type argument: {@code LFluid;}, or with wildcards for a generic one ({@code LFeature<*>;}). */
    private static String entryType(String type, @Nullable String signature) {
        if (signature == null || !signature.startsWith("<")) {
            // A generic registered type seen from an implementation: its own parameters aren't at hand; one is usual.
            return REGISTRY_INTERFACES.contains(type) ? "L" + type + "<*>;" : "L" + type + ";";
        }
        int[] count = {0};
        new org.objectweb.asm.signature.SignatureReader(signature).accept(new org.objectweb.asm.signature.SignatureVisitor(Opcodes.ASM5) {
            @Override
            public void visitFormalTypeParameter(String name) {
                count[0]++;
            }
        });
        StringBuilder argument = new StringBuilder("L").append(type).append('<');
        for (int i = 0; i < count[0]; i++) argument.append('*');
        return argument.append(">;").toString();
    }

    private static int endOfTypeParameters(String signature) {
        int depth = 0;
        for (int i = 0; i < signature.length(); i++) {
            char c = signature.charAt(i);
            if (c == '<') depth++;
            else if (c == '>' && --depth == 0) return i + 1;
        }
        return 0;
    }

    private static int widen(int access) {
        return access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED) | Opcodes.ACC_PUBLIC;
    }

    private Class<?> define(String name, Source source, @Nullable byte[] bytes) throws ClassNotFoundException {
        return define(name, source, bytes, null);
    }

    /** @param relocated for a Minecraft 1.13.2 class moved under {@link ModernMappings#NATIVE}, its original name */
    private Class<?> define(String name, Source source, @Nullable byte[] bytes, @Nullable String relocated) throws ClassNotFoundException {
        if (bytes == null) throw new ClassNotFoundException(name);
        try {
            bytes = relocated != null ? this.translator.translateRelocated(bytes, relocated) : this.translator.translate(bytes);
            bytes = prepareEvent(bytes);
        } catch (RuntimeException e) {
            throw new ClassNotFoundException("Couldn't translate " + name + " from " + source.file.getName(), e);
        }
        dump(name, bytes);
        definePackage(name, source);
        try {
            return defineClass(name, bytes, 0, bytes.length, source.domain);
        } catch (VerifyError | IncompatibleClassChangeError e) {
            // Name the class: the JVM's message alone doesn't.
            LinkageError named = e instanceof VerifyError ? new VerifyError(name + ": " + e.getMessage()) : new IncompatibleClassChangeError(name + ": " + e.getMessage());
            named.setStackTrace(e.getStackTrace());
            throw named;
        }
    }

    /** A development aid: {@code -Dportium.modern.dump=<folder>} writes every class as translated there. */
    private static final String DUMP = System.getProperty("portium.forge116.dump");

    private static void dump(String name, byte[] bytes) {
        if (DUMP == null) return;
        try {
            java.nio.file.Path file = java.nio.file.Paths.get(DUMP, name.replace('.', '/') + ".class");
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.write(file, bytes);
        } catch (IOException e) {
            LOGGER.warn("Couldn't dump {}", name, e);
        }
    }

    /**
     * An empty interface standing in for a Minecraft class 1.13 has and neither 1.12 nor Portium does. The JVM's
     * verifier loads classes named in code it checks, even code that never runs; with this, classes that only touch
     * such a class on paths a mod doesn't take still load. Using it fails where it's used.
     */
    private Class<?> placeholder(String name) {
        LOGGER.warn("1.12 has no {}; code that uses it will fail", name);
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, name.replace('.', '/'), null, "java/lang/Object", null);
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        return defineClass(name, bytes, 0, bytes.length);
    }

    private void definePackage(String name, Source source) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) return;
        String pkg = name.substring(0, dot);
        if (getPackage(pkg) != null) return;
        Manifest manifest = source.manifest;
        if (manifest == null) {
            definePackage(pkg, null, null, null, null, null, null, null);
            return;
        }
        Attributes main = manifest.getMainAttributes();
        String original = pkg.startsWith(FORGE) ? pkg.substring(FORGE.length()) : pkg;
        Attributes section = manifest.getAttributes(original.replace('.', '/') + "/");
        definePackage(pkg, attribute(section, main, Attributes.Name.SPECIFICATION_TITLE), attribute(section, main, Attributes.Name.SPECIFICATION_VERSION),
                attribute(section, main, Attributes.Name.SPECIFICATION_VENDOR), attribute(section, main, Attributes.Name.IMPLEMENTATION_TITLE),
                attribute(section, main, Attributes.Name.IMPLEMENTATION_VERSION), attribute(section, main, Attributes.Name.IMPLEMENTATION_VENDOR), null);
    }

    @Nullable
    private static String attribute(@Nullable Attributes section, Attributes main, Attributes.Name name) {
        String value = section == null ? null : section.getValue(name);
        return value != null ? value : main.getValue(name);
    }

    /** Event classes get what Forge 1.12's event bus expects of them (see {@link ModernEventClasses}). */
    private byte[] prepareEvent(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        if (!ModernEventClasses.isEventClass(node, this)) return bytes;
        ModernEventClasses.prepare(node);
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    // For the audit: classes this loader defines, by translated internal name.

    @Nullable
    ModernBridges bridges() {
        return this.bridges;
    }

    boolean ownsTranslated(String type) {
        return sourceOf(type.replace('/', '.')) != null || isNative(type);
    }

    @Nullable
    private ClassNode translatedNode(String type) {
        String name = type.replace('/', '.');
        Source source = sourceOf(name);
        try {
            byte[] bytes = source != null ? source.read(path(name)) : nativeBytes(name);
            if (bytes == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(this.translator.translate(bytes)).accept(node, ClassReader.SKIP_CODE);
            return node;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    boolean declares(String type, String name, String desc) {
        ClassNode node = translatedNode(type);
        if (node == null) return false;
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) return true;
        }
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) return true;
        }
        return false;
    }

    @Nullable
    String superclassOf(String type) {
        ClassNode node = translatedNode(type);
        return node == null ? null : node.superName;
    }

    @Override
    public boolean exists(String type) {
        synchronized (this.existing) {
            Boolean known = this.existing.get(type);
            if (known != null) return known;
        }
        boolean found;
        try {
            Class.forName(type.replace('/', '.'), false, getParent());
            found = true;
        } catch (ClassNotFoundException | LinkageError e) {
            found = false;
        } catch (RuntimeException e) {
            // Forge refusing a client-only class on a server: it exists, just not on this side.
            found = true;
        }
        synchronized (this.existing) {
            this.existing.put(type, found);
        }
        return found;
    }

    // Hierarchy, for redirects: names are 1.13's.

    @Override
    public boolean isSubtype(String type, String supertype) {
        return isSubtypeTranslated(this.mappings.mapClass(type), this.mappings.mapClass(supertype));
    }

    @Nullable
    @Override
    public Boolean isInterface(String type) {
        String name = type.replace('/', '.');
        Source source = sourceOf(name);
        try {
            byte[] bytes = source != null ? source.read(path(name)) : nativeBytes(name);
            if (bytes != null) return (new ClassReader(bytes).getAccess() & Opcodes.ACC_INTERFACE) != 0;
            if (source != null) return null;
            return Class.forName(name, false, getParent()).isInterface();
        } catch (IOException | ClassNotFoundException | LinkageError | RuntimeException e) {
            return null;
        }
    }

    @Override
    public boolean isTranslatedSubtype(String type, String supertype) {
        return isSubtypeTranslated(type, supertype);
    }

    private boolean isSubtypeTranslated(String type, String supertype) {
        if (type.equals(supertype) || supertype.equals("java/lang/Object")) return true;
        for (String parent : translatedSupertypes(type)) {
            if (parent != null && isSubtypeTranslated(parent, supertype)) return true;
        }
        return false;
    }

    /**
     * The superclass (first, null for none) and interfaces of a translated class name. Classes this loader owns are
     * read from their bytes, so asking doesn't define them; others come from the parent without initialising them.
     */
    @Override
    @Nullable
    public String modernSuperName(String type) {
        ClassNode node = modernNode(type);
        return node == null ? null : node.superName;
    }

    @Override
    public boolean nativeDeclares(String type, String name, String desc, boolean field) {
        if (this.natives == null || !this.natives.has(type)) return false;
        ClassNode node = modernNodes.computeIfAbsent(NATIVE.replace('.', '/') + type, key -> {
            try {
                byte[] bytes = this.natives.read(type);
                if (bytes == null) return null;
                ClassNode read = new ClassNode();
                new ClassReader(bytes).accept(read, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return read;
            } catch (IOException e) {
                return null;
            }
        });
        if (node == null) return false;
        if (field) {
            for (FieldNode each : node.fields) if (each.name.equals(name) && each.desc.equals(desc)) return true;
        } else {
            for (MethodNode each : node.methods) if (each.name.equals(name) && each.desc.equals(desc)) return true;
        }
        return false;
    }

    private final Map<String, ClassNode> modernNodes = new java.util.concurrent.ConcurrentHashMap<>();

    /** A class this loader defines (a mod's, or Minecraft 1.13.2's), by 1.13 name, untranslated and without code. */
    @Nullable
    private ClassNode modernNode(String type) {
        return this.untranslatedNodes.computeIfAbsent(type, key -> java.util.Optional.ofNullable(readModernNode(key))).orElse(null);
    }

    private final Map<String, java.util.Optional<ClassNode>> untranslatedNodes = new java.util.concurrent.ConcurrentHashMap<>();

    @Nullable
    private ClassNode readModernNode(String type) {
        String binary = this.mappings.mapClass(type).replace('/', '.');
        Source source = sourceOf(binary);
        try {
            byte[] bytes = source != null ? source.read(path(binary)) : nativeBytes(binary);
            if (bytes == null && this.natives != null && this.natives.has(type)) bytes = this.natives.read(type);
            if (bytes == null) return null;
            ClassNode node = new ClassNode();
            new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean isLegacyValueType(String type) {
        if (!exists(type)) return false;
        try {
            Class<?> found = Class.forName(type.replace('/', '.'), false, getParent());
            return found.isEnum() || found.isInterface();
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Override
    public boolean inMinecraft113(String type) {
        return this.natives != null && this.natives.has(type);
    }

    @Override
    public boolean declaresModern(String type, String name, String desc) {
        return declaresModern(type, name, desc, false);
    }

    @Override
    public Set<String> translatedMethods(String type) {
        return this.translatedMethods.computeIfAbsent(type, key -> {
            Set<String> keys = new HashSet<>();
            ClassNode node = modernNode(key);
            if (node != null) {
                for (MethodNode method : node.methods) {
                    if ((method.access & Opcodes.ACC_ABSTRACT) == 0) keys.add(this.translator.methodKey(key, method.name, method.desc));
                }
            }
            return keys;
        });
    }

    private final Map<String, Set<String>> translatedMethods = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public boolean declaresBelowAdapters(String type, String name, String desc) {
        return declaresModern(type, name, desc, true);
    }

    private boolean declaresModern(String type, String name, String desc, boolean runtimeOnly) {
        Set<String> seen = new HashSet<>();
        boolean below = false;
        for (String current = type; current != null && seen.add(current); ) {
            if (runtimeOnly) {
                // Only classes that run as 1.13 code count: a mod's, 1.13.2's own classes 1.12 lacks, and the block and
                // item classes copied from 1.13.2 under a mod's. Portium's adapters (Block, Item...) and 1.12's
                // classes end the walk.
                if (ModernRedirects.adapterOf(current) != null) return false;
                // A 1.13-only class that Portium renames to one of 1.12's (IWorld is World) runs as 1.12's.
                String mapped = this.mappings.mapClass(current);
                boolean runs = sourceOf(mapped.replace('/', '.')) != null
                        || isNative(current) && (mapped.equals(current) || mapped.startsWith(NATIVE.replace('.', '/')))
                        || below && (current.startsWith("net/minecraft/block/") || current.startsWith("net/minecraft/item/"));
                if (!runs) return false;
                below = true;
            }
            ClassNode node = modernNode(current);
            if (node == null) return false;
            for (MethodNode method : node.methods) {
                if (method.name.equals(name) && method.desc.equals(desc)) return true;
            }
            current = node.superName;
        }
        return false;
    }

    @Override
    public String[] supertypes(String type) {
        return translatedSupertypes(type);
    }

    private String[] translatedSupertypes(String type) {
        synchronized (this.supertypes) {
            String[] known = this.supertypes.get(type);
            if (known != null) return known;
        }
        String[] found = new String[0];
        String name = type.replace('/', '.');
        Source source = sourceOf(name);
        try {
            byte[] bytes = source != null ? source.read(path(name)) : nativeBytes(name);
            if (bytes != null) {
                ClassReader reader = new ClassReader(bytes);
                String[] interfaces = reader.getInterfaces();
                found = new String[interfaces.length + 1];
                String superName = reader.getSuperName();
                String replaced = superName == null ? null : this.translator.superclass(superName);
                found[0] = superName == null ? null : replaced != null ? replaced : this.mappings.mapClass(superName);
                for (int i = 0; i < interfaces.length; i++) found[i + 1] = this.mappings.mapClass(interfaces[i]);
            } else if (!owns(name)) {
                Class<?> loaded = Class.forName(name, false, getParent());
                Class<?>[] interfaces = loaded.getInterfaces();
                found = new String[interfaces.length + 1];
                found[0] = loaded.getSuperclass() == null ? null : loaded.getSuperclass().getName().replace('.', '/');
                for (int i = 0; i < interfaces.length; i++) found[i + 1] = interfaces[i].getName().replace('.', '/');
            }
        } catch (IOException | ClassNotFoundException | LinkageError e) {
            found = new String[0];
        }
        synchronized (this.supertypes) {
            this.supertypes.put(type, found);
        }
        return found;
    }

    /** A Minecraft 1.13.2 class this loader defines rather than the parent, untranslated; null for any other class. */
    @Nullable
    private byte[] nativeBytes(String name) throws IOException {
        if (name.startsWith(NATIVE)) return readNative(name);
        if (!isMinecraft(name) || !hasNative(name) || exists(name.replace('.', '/'))) return null;
        return readNative(name);
    }

    /** A class by its 1.16 name, as 1.16 code gets it (1.12's, a stand-in, or Minecraft 1.16.5's own). */
    Class<?> modernClass(String modernName) throws ClassNotFoundException {
        return loadClass(this.mappings.mapClass(modernName).replace('/', '.'));
    }

    @Override
    public boolean isNative(String type) {
        String name = type.replace('/', '.');
        return name.startsWith(NATIVE) || isMinecraft(name) && hasNative(name) && !exists(type);
    }

    // Resources: mod jars and Forge 1.13 first.

    @Override
    @Nullable
    public URL getResource(String name) {
        URL own = findResource(name);
        return own != null ? own : getParent().getResource(name);
    }

    @Override
    @Nullable
    protected URL findResource(String name) {
        for (Source source : this.mods) {
            if (source.has(name)) return url(source, name);
        }
        for (Source source : this.runtime) {
            if (source.has(name)) return url(source, name);
        }
        return null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        List<URL> urls = new ArrayList<>();
        for (Source source : this.mods) {
            if (source.has(name)) urls.add(url(source, name));
        }
        for (Source source : this.runtime) {
            if (source.has(name)) urls.add(url(source, name));
        }
        return Collections.enumeration(urls);
    }

    @Nullable
    private static URL url(Source source, String name) {
        try {
            return source.url(name);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(in.available(), 4096));
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }
}
