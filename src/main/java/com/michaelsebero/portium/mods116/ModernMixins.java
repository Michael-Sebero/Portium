package com.michaelsebero.portium.mods116;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * 1.16 mods' mixins. They are written against Minecraft 1.16.5's classes and applied by a class transformer Portium
 * can't run over 1.12's classes, so they're read here instead and served another way:
 * <ul>
 * <li>Accessor mixins (interfaces of {@code @Accessor} and {@code @Invoker} methods, which mods cast game objects
 * to): each call becomes a call to a helper class made here, holding plain 1.16 code that reads the field or calls
 * the method. The helper is translated like any 1.16 class, so the member's name, 1.12's access rules and members
 * 1.12 lacks are dealt with as for the mod's own code. Casts to the interface are dropped.</li>
 * <li>Every other mixin is listed in the log once, by target, as not applied: what it changes in Minecraft doesn't
 * happen (see {@link #describeUnapplied}).</li>
 * </ul>
 */
final class ModernMixins {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    static final String HELPER_SUFFIX = "$PortiumAccess116";
    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";

    private static final int GET = 0, SET = 1, CALL = 2, NEW = 3;

    /** An accessor or invoker method: the member of the target class it reaches. */
    private static final class Member {
        final int kind;
        final boolean isStatic;
        final String target;
        final String name;
        final String desc;

        Member(int kind, boolean isStatic, String target, String name, String desc) {
            this.kind = kind;
            this.isStatic = isStatic;
            this.target = target;
            this.name = name;
            this.desc = desc;
        }
    }

    /** An accessor mixin interface: its target class and its methods by name and descriptor. */
    private static final class Accessors {
        final String target;
        final Map<String, Member> members = new LinkedHashMap<>();

        Accessors(String target) {
            this.target = target;
        }
    }

    /** Accessor mixin interfaces by internal name. */
    private final Map<String, Accessors> accessors = new HashMap<>();
    /** Mixins that change Minecraft's code, which Portium can't apply: "mixin class -> targets". */
    private final List<String> unapplied = new ArrayList<>();
    /** Mixin config plugins ("class package"): their {@code onLoad} runs before mods construct (mods set up early there). */
    private final List<String[]> plugins = new ArrayList<>();

    ModernMixins(List<File> jars) {
        for (File jar : jars) {
            try (JarFile zip = new JarFile(jar)) {
                Manifest manifest = zip.getManifest();
                String configs = manifest == null ? null : manifest.getMainAttributes().getValue("MixinConfigs");
                java.util.Set<String> names = new java.util.LinkedHashSet<>();
                if (configs != null) {
                    for (String config : configs.split(",")) if (!config.trim().isEmpty()) names.add(config.trim());
                }
                // A mixin connector (Botania's) adds its configs in code: the jar's mixin configs, by their usual names.
                if (manifest != null && manifest.getMainAttributes().getValue("MixinConnector") != null) {
                    java.util.Enumeration<java.util.jar.JarEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        String entryName = entries.nextElement().getName();
                        if (entryName.endsWith(".mixins.json") || entryName.endsWith("mixins.json")) names.add(entryName);
                    }
                }
                for (String config : names) readConfig(zip, config);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Couldn't read the mixins of {}", jar.getName(), e);
            }
        }
        if (!this.accessors.isEmpty() || !this.unapplied.isEmpty()) {
            LOGGER.info("1.16 mods' mixins: {} accessor interfaces served, {} code-changing mixins not applied", this.accessors.size(), this.unapplied.size());
        }
    }

    private void readConfig(JarFile zip, String name) throws IOException {
        JsonObject config = json(zip, name);
        if (config == null) return;
        String pkg = config.has("package") ? config.get("package").getAsString() : "";
        if (config.has("plugin")) this.plugins.add(new String[] {config.get("plugin").getAsString(), pkg});
        Map<String, Map<String, String>> refmap = new HashMap<>();
        if (config.has("refmap")) {
            JsonObject map = json(zip, config.get("refmap").getAsString());
            if (map != null && map.has("mappings")) {
                for (Map.Entry<String, JsonElement> owner : map.getAsJsonObject("mappings").entrySet()) {
                    Map<String, String> entries = new HashMap<>();
                    for (Map.Entry<String, JsonElement> entry : owner.getValue().getAsJsonObject().entrySet()) entries.put(entry.getKey(), entry.getValue().getAsString());
                    refmap.put(owner.getKey(), entries);
                }
            }
        }
        for (String list : new String[] {"mixins", "client", "server"}) {
            if (!config.has(list)) continue;
            for (JsonElement element : (JsonArray) config.get(list)) {
                String internal = (pkg.isEmpty() ? "" : pkg + ".").replace('.', '/') + element.getAsString().replace('.', '/');
                ZipEntry entry = zip.getEntry(internal + ".class");
                if (entry == null) continue;
                ClassNode node = new ClassNode();
                try (InputStream in = zip.getInputStream(entry)) {
                    new ClassReader(readAll(in)).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                }
                readMixin(node, refmap.getOrDefault(internal, new HashMap<>()));
            }
        }
    }

    private void readMixin(ClassNode node, Map<String, String> refmap) {
        List<String> targets = targets(node);
        boolean accessorOnly = (node.access & Opcodes.ACC_INTERFACE) != 0 && !targets.isEmpty();
        Accessors found = new Accessors(targets.isEmpty() ? "java/lang/Object" : targets.get(0));
        for (MethodNode method : node.methods) {
            AnnotationNode accessor = annotation(method, ACCESSOR);
            AnnotationNode invoker = annotation(method, INVOKER);
            boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
            if (accessor != null) {
                Type[] arguments = Type.getArgumentTypes(method.desc);
                boolean setter = arguments.length == 1 && Type.getReturnType(method.desc).getSort() == Type.VOID;
                String field = value(accessor);
                if (field == null) field = inflect(method.name, "get", "is", "set");
                String mapped = refmap.get(field);
                String name = field;
                String desc = setter ? arguments[0].getDescriptor() : Type.getReturnType(method.desc).getDescriptor();
                if (mapped != null) {
                    int colon = mapped.indexOf(':');
                    String member = colon < 0 ? mapped : mapped.substring(0, colon);
                    name = member.substring(member.lastIndexOf(';') + 1);
                    if (colon >= 0) desc = mapped.substring(colon + 1);
                }
                found.members.put(method.name + method.desc, new Member(setter ? SET : GET, isStatic, found.target, name, desc));
            } else if (invoker != null) {
                String called = value(invoker);
                if (called == null) called = inflect(method.name, "call", "invoke", "new", "create");
                boolean constructs = "<init>".equals(called) || (value(invoker) == null && (method.name.startsWith("new") || method.name.startsWith("create")));
                String mapped = refmap.get(called);
                String name = called;
                String desc = method.desc;
                if (mapped != null) {
                    int paren = mapped.indexOf('(');
                    String member = paren < 0 ? mapped : mapped.substring(0, paren);
                    name = member.substring(member.lastIndexOf(';') + 1);
                    if (paren >= 0) desc = mapped.substring(paren);
                }
                if (constructs) {
                    found.members.put(method.name + method.desc, new Member(NEW, true, found.target, "<init>",
                            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(method.desc))));
                } else {
                    found.members.put(method.name + method.desc, new Member(CALL, isStatic, found.target, name, desc));
                }
            } else if (!isStatic || !method.name.startsWith("<")) {
                // A method of its own (an injector, a shadow, a helper): not just an accessor interface.
                if ((method.access & Opcodes.ACC_ABSTRACT) == 0 || annotation(method, "Lorg/spongepowered/asm/mixin/Shadow;") == null) accessorOnly = false;
            }
        }
        if (accessorOnly && !found.members.isEmpty()) {
            this.accessors.put(node.name, found);
        } else {
            this.unapplied.add(node.name.substring(node.name.lastIndexOf('/') + 1) + " -> " + String.join(", ", simple(targets)));
        }
    }

    private static List<String> simple(List<String> names) {
        List<String> simple = new ArrayList<>();
        for (String name : names) simple.add(name.substring(name.lastIndexOf('/') + 1));
        return simple;
    }

    /** The mixins that change Minecraft's own code, which don't apply here: "Mixin -> Target" lines. */
    List<String> describeUnapplied() {
        return this.unapplied;
    }

    @SuppressWarnings("unchecked")
    private static List<String> targets(ClassNode node) {
        List<String> targets = new ArrayList<>();
        List<AnnotationNode> annotations = new ArrayList<>();
        if (node.visibleAnnotations != null) annotations.addAll(node.visibleAnnotations);
        if (node.invisibleAnnotations != null) annotations.addAll(node.invisibleAnnotations);
        for (AnnotationNode annotation : annotations) {
            if (!MIXIN.equals(annotation.desc) || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                Object value = annotation.values.get(i + 1);
                if ("value".equals(annotation.values.get(i))) {
                    for (Type type : (List<Type>) value) targets.add(type.getInternalName());
                } else if ("targets".equals(annotation.values.get(i))) {
                    for (String name : (List<String>) value) targets.add(name.replace('.', '/'));
                }
            }
        }
        return targets;
    }

    @Nullable
    private static AnnotationNode annotation(MethodNode method, String desc) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (method.visibleAnnotations != null) annotations.addAll(method.visibleAnnotations);
        if (method.invisibleAnnotations != null) annotations.addAll(method.invisibleAnnotations);
        for (AnnotationNode annotation : annotations) {
            if (desc.equals(annotation.desc)) return annotation;
        }
        return null;
    }

    @Nullable
    private static String value(AnnotationNode annotation) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if ("value".equals(annotation.values.get(i))) {
                String value = String.valueOf(annotation.values.get(i + 1));
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /** Mixin's rule for the member a method names: the prefix dropped, the first letter lowered unless all are capitals. */
    private static String inflect(String method, String... prefixes) {
        for (String prefix : prefixes) {
            if (method.startsWith(prefix) && method.length() > prefix.length() && Character.isUpperCase(method.charAt(prefix.length()))) {
                String rest = method.substring(prefix.length());
                return rest.equals(rest.toUpperCase(java.util.Locale.ROOT)) ? rest : Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
            }
        }
        return method;
    }

    @Nullable
    private static JsonObject json(JarFile zip, String name) throws IOException {
        ZipEntry entry = zip.getEntry(name);
        if (entry == null) return null;
        try (InputStream in = zip.getInputStream(entry)) {
            return new JsonParser().parse(new String(readAll(in), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    boolean isEmpty() {
        return this.accessors.isEmpty();
    }

    /** Whether a class (internal name) is an accessor mixin interface served here. */
    boolean isAccessor(String name) {
        return this.accessors.containsKey(name);
    }

    /** Whether a class name (internal or dotted) is a helper's. */
    boolean isHelper(String name) {
        return name.endsWith(HELPER_SUFFIX) && this.accessors.containsKey(name.substring(0, name.length() - HELPER_SUFFIX.length()).replace('.', '/'));
    }

    /**
     * Rewrites a method's uses of accessor mixin interfaces (1.16 names, before translation): casts to them go, type
     * tests ask for the target class, and calls go to the helper.
     */
    void rewrite(MethodNode method) {
        if (this.accessors.isEmpty()) return;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; ) {
            AbstractInsnNode next = insn.getNext();
            if (insn instanceof TypeInsnNode) {
                TypeInsnNode type = (TypeInsnNode) insn;
                Accessors mixin = this.accessors.get(type.desc);
                if (mixin != null && insn.getOpcode() == Opcodes.CHECKCAST) {
                    method.instructions.remove(insn);
                } else if (mixin != null && insn.getOpcode() == Opcodes.INSTANCEOF) {
                    type.desc = mixin.target;
                }
            } else if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                Accessors mixin = this.accessors.get(call.owner);
                Member member = mixin == null ? null : mixin.members.get(call.name + call.desc);
                if (member != null) {
                    boolean instance = call.getOpcode() != Opcodes.INVOKESTATIC;
                    call.desc = instance ? "(Ljava/lang/Object;" + call.desc.substring(1) : call.desc;
                    call.owner = call.owner + HELPER_SUFFIX;
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.itf = false;
                }
            }
            insn = next;
        }
    }

    /** A helper class's bytes, in 1.16 names: one static method for each accessor or invoker of its mixin. */
    @Nullable
    byte[] helper(String helperName) {
        String internal = helperName.replace('.', '/');
        Accessors mixin = this.accessors.get(internal.substring(0, internal.length() - HELPER_SUFFIX.length()));
        if (mixin == null) return null;
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, internal, null, "java/lang/Object", null);
        for (Map.Entry<String, Member> entry : mixin.members.entrySet()) {
            String key = entry.getKey();
            Member member = entry.getValue();
            String name = key.substring(0, key.indexOf('('));
            String desc = key.substring(key.indexOf('('));
            boolean instance = !member.isStatic;
            String helperDesc = instance ? "(Ljava/lang/Object;" + desc.substring(1) : desc;
            MethodVisitor code = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, helperDesc, null, null);
            code.visitCode();
            int local = 0;
            if (member.kind == NEW) {
                code.visitTypeInsn(Opcodes.NEW, member.target);
                code.visitInsn(Opcodes.DUP);
            } else if (instance) {
                code.visitVarInsn(Opcodes.ALOAD, 0);
                code.visitTypeInsn(Opcodes.CHECKCAST, member.target);
                local = 1;
            }
            for (Type argument : Type.getArgumentTypes(desc)) {
                code.visitVarInsn(argument.getOpcode(Opcodes.ILOAD), local);
                local += argument.getSize();
            }
            Type returns = Type.getReturnType(desc);
            switch (member.kind) {
                case GET:
                    code.visitFieldInsn(instance ? Opcodes.GETFIELD : Opcodes.GETSTATIC, member.target, member.name, member.desc);
                    if (returns.getSort() >= Type.ARRAY && !returns.getDescriptor().equals(member.desc)) code.visitTypeInsn(Opcodes.CHECKCAST, returns.getInternalName());
                    break;
                case SET:
                    code.visitFieldInsn(instance ? Opcodes.PUTFIELD : Opcodes.PUTSTATIC, member.target, member.name, member.desc);
                    break;
                case CALL:
                    code.visitMethodInsn(instance ? Opcodes.INVOKEVIRTUAL : Opcodes.INVOKESTATIC, member.target, member.name, member.desc, false);
                    break;
                default:
                    code.visitMethodInsn(Opcodes.INVOKESPECIAL, member.target, "<init>", member.desc, false);
                    break;
            }
            code.visitInsn(returns.getOpcode(Opcodes.IRETURN));
            code.visitMaxs(0, 0);
            code.visitEnd();
        }
        writer.visitEnd();
        return writer.toByteArray();
    }

    /**
     * Runs the mods' mixin config plugins' {@code onLoad}, as Mixin does when it reads the configs, before any mod
     * class loads (Supplementaries builds its registry config there). Their mixins aren't applied, so only that.
     */
    void runPlugins(ClassLoader loader) {
        for (String[] plugin : this.plugins) {
            try {
                Object instance = Class.forName(plugin[0], true, loader).newInstance();
                instance.getClass().getMethod("onLoad", String.class).invoke(instance, plugin[1]);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
                LOGGER.warn("1.16.5 mod problem: mixin config plugin {} failed to load", plugin[0], cause);
            }
        }
    }
}
