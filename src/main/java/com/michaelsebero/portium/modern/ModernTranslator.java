package com.michaelsebero.portium.modern;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites a class compiled against Minecraft and Forge 1.13.2 so it links against 1.12.2: calls to methods 1.12
 * lacks go to Portium's stand-ins and constructors 1.12 has with other parameters get their arguments converted
 * ({@link ModernRedirects}), then classes and members are renamed
 * ({@link ModernMappings}). Stack map frames are renamed along with everything else rather than recomputed, so no
 * classes are loaded while translating.
 */
final class ModernTranslator {

    private static final String HOOKS = "com/michaelsebero/portium/modern/ModernHooks";

    private final ModernMappings mappings;
    private final ModernRedirects redirects;
    private final ModernRedirects.Hierarchy hierarchy;
    /** The side this runs on, as Forge 1.13's {@code Dist} names it: CLIENT or DEDICATED_SERVER. */
    private final String dist;
    private final ModernLinker linker;
    /**
     * While translating one of Minecraft 1.13.2's classes moved under {@link ModernMappings#NATIVE} (see
     * {@link #superclass}): its original name, outermost class, so it and its nested classes are renamed with it.
     */
    private final ThreadLocal<String> relocated = new ThreadLocal<>();
    private final Remapper names = new Remapper() {

        @Override
        public String map(String type) {
            if (type.startsWith(ModernMappings.LEGACY)) return type.substring(ModernMappings.LEGACY.length());
            String relocated = relocate(type);
            return relocated != type ? relocated : ModernTranslator.this.mappings.mapClass(type);
        }

        @Override
        public String mapMethodName(String owner, String name, String desc) {
            return ModernTranslator.this.mappings.mapMember(relocate(unmark(owner)), name);
        }

        /** A lambda's method: named as the interface it implements names it. */
        @Override
        public String mapInvokeDynamicMethodName(String name, String desc) {
            Type returned = Type.getReturnType(desc);
            if (returned.getSort() != Type.OBJECT) return name;
            return ModernTranslator.this.mappings.mapMember(relocate(unmark(returned.getInternalName())), name);
        }

        @Override
        public String mapFieldName(String owner, String name, String desc) {
            return ModernTranslator.this.mappings.mapMember(relocate(unmark(owner)), name);
        }

        @Override
        public Object mapValue(Object value) {
            return value instanceof String ? ModernTranslator.this.mappings.mapString((String) value) : super.mapValue(value);
        }

        @Override
        protected SignatureVisitor createSignatureRemapper(SignatureVisitor next) {
            return new Signatures(next);
        }
    };

    private static String unmark(String type) {
        return type.startsWith(ModernMappings.LEGACY) ? type.substring(ModernMappings.LEGACY.length()) : type;
    }

    /** A class's name under {@link ModernMappings#NATIVE} if it's the relocated class being translated, else the name itself. */
    private String relocate(String type) {
        String outer = this.relocated.get();
        if (outer == null) return type;
        if (type.equals(outer)) return ModernMappings.NATIVE + type;
        // Nested enums and interfaces 1.12 has too are values both share (a pressure plate's sensitivity); the rest
        // (anonymous classes and the like) are the moved class's own.
        if (type.startsWith(outer + "$") && !this.hierarchy.isLegacyValueType(type)) return ModernMappings.NATIVE + type;
        return type;
    }

    ModernTranslator(ModernMappings mappings, ModernRedirects redirects, ModernRedirects.Hierarchy hierarchy, ModernMembers members,
                     @Nullable ModernBridges bridges, boolean client) {
        this.mappings = mappings;
        this.redirects = redirects;
        this.hierarchy = hierarchy;
        this.dist = client ? "CLIENT" : "DEDICATED_SERVER";
        this.linker = new ModernLinker(mappings, members, hierarchy, this.names, bridges);
        this.completion = new ModernCompletion(members, hierarchy);
    }

    private final ModernCompletion completion;

    /** A method's name and descriptor as translated, declared by a class (translated name). */
    String methodKey(String owner, String name, String desc) {
        return this.names.mapMethodName(owner, name, desc) + this.names.mapMethodDesc(desc);
    }

    String memberName(String owner, String srg) {
        return this.mappings.mapMember(owner, srg);
    }

    /** A class as it's defined: for the side this runs on (see {@link ModernDist}), and linking against 1.12. */
    byte[] translate(byte[] bytes) {
        return translate(bytes, true);
    }

    /** A class with 1.12's names, for reading its annotations: as Forge 1.13 scans mods, whichever side it's for. */
    byte[] translateForScan(byte[] bytes) {
        return translate(bytes, false);
    }

    /** 1.13's block or item names, from {@code Blocks} or {@code Items} (1.13 names). */
    List<String> registeredNames(String owner) {
        return this.linker.registeredNames(owner);
    }

    /** One of Minecraft 1.13.2's classes moved under {@link ModernMappings#NATIVE}, by its original name. */
    byte[] translateRelocated(byte[] bytes, String name) {
        int dollar = name.indexOf('$');
        this.relocated.set(dollar < 0 ? name : name.substring(0, dollar));
        try {
            return translate(bytes, true);
        } finally {
            this.relocated.remove();
        }
    }

    /**
     * The class a 1.13 class extends in place of {@code superName} (1.13's name), or null to extend it as renamed:
     * Portium's adapter of it ({@link ModernRedirects#adapter}), or else, for a block or item class 1.12 has too,
     * Minecraft 1.13.2's own version of it, moved under {@link ModernMappings#NATIVE}. 1.13's block and item classes
     * differ from 1.12's in nearly everything (flattened states, shapes, properties, fluids), so a mod's door or bucket
     * is built on 1.13's door or bucket, and that on Portium's 1.13 {@code Block} or {@code Item}; other references to
     * the class still mean 1.12's, so vanilla doors are still doors to 1.13 code.
     */
    @Nullable
    String superclass(String superName) {
        String adapter = this.redirects.adapter(superName);
        if (adapter != null) return adapter;
        return nativeVersion(superName);
    }

    @Nullable
    private String nativeVersion(String name) {
        if (!name.startsWith("net/minecraft/block/") && !name.startsWith("net/minecraft/item/")) return null;
        return nativeVersionOfAny(name);
    }

    /** Minecraft 1.13.2's own version of any class 1.12 has by the same name, moved under {@link ModernMappings#NATIVE}. */
    @Nullable
    private String nativeVersionOfAny(String name) {
        if (!name.startsWith("net/minecraft/") || !this.mappings.mapClass(name).equals(name)) return null;
        if (!this.hierarchy.exists(name) || !this.hierarchy.inMinecraft113(name)) return null;
        return ModernMappings.NATIVE + name;
    }

    private byte[] translate(byte[] bytes, boolean forSide) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode node = new ClassNode();
        // Expanded: ASM 5.2's remapper fails on compressed frames that drop locals.
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        if (forSide) ModernDist.clean(node, this.dist);
        ClassNode translated = new ClassNode();
        node.accept(new Redirecting(new ClassRemapper(translated, this.names)));
        this.linker.fixAccess(translated);
        this.completion.complete(translated);
        ClassWriter writer = new ClassWriter(0);
        translated.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Renames generic signatures, dropping the type arguments of Minecraft classes neither 1.12, 1.13.2's jar nor
     * Portium has. Those load as placeholders without type parameters (see {@link ModernClassLoader}), and reflection refuses a
     * signature that gives a class more type arguments than it declares, as Forge's event bus finds out when it reads
     * a listener's {@code RegistryEvent.Register<TileEntityType<?>>}.
     */
    private final class Signatures extends SignatureRemapper {

        private final Deque<Boolean> stripping = new ArrayDeque<>();

        Signatures(SignatureVisitor next) {
            super(Opcodes.ASM5, next, ModernTranslator.this.names);
        }

        @Override
        public void visitClassType(String name) {
            this.stripping.push(isMissing(name));
            super.visitClassType(name);
        }

        @Override
        public void visitTypeArgument() {
            if (!stripping()) super.visitTypeArgument();
        }

        @Override
        public SignatureVisitor visitTypeArgument(char wildcard) {
            return stripping() ? IGNORED : super.visitTypeArgument(wildcard);
        }

        @Override
        public void visitEnd() {
            this.stripping.pop();
            super.visitEnd();
        }

        private boolean stripping() {
            return !this.stripping.isEmpty() && this.stripping.peek();
        }

        private boolean isMissing(String name) {
            return name.startsWith("net/minecraft/") && ModernTranslator.this.mappings.mapClass(name).equals(name)
                    && !ModernTranslator.this.hierarchy.exists(name) && !ModernTranslator.this.hierarchy.isNative(name);
        }
    }

    private static final SignatureVisitor IGNORED = new SignatureVisitor(Opcodes.ASM5) {};

    /**
     * Sees each class with 1.13's names, before {@link #names} renames it. Classes that extend one of 1.12's classes
     * Portium adapts ({@link ModernRedirects#adapter}) extend the adapter instead, and those that declare Forge 1.13's
     * {@code getCapability} get 1.12's capability methods bridged to it.
     */
    private final class Redirecting extends ClassVisitor {

        private String name;
        @Nullable
        private String replacedSuper;
        private String superName;
        @Nullable
        private String[] interfaces;
        private boolean isInterface;
        private final List<MethodNode> methods = new ArrayList<>();
        private boolean modernCapabilities;
        private boolean legacyCapabilities;
        /** Tooltip methods this class declares, renamed: 1.13 name, 1.13 descriptor, 1.12 descriptor. */
        private final List<String[]> tooltips = new ArrayList<>();
        /** The superclasses (1.13 names) this class reaches through Minecraft 1.13.2's own versions of them. */
        private List<String> copied = new ArrayList<>();

        Redirecting(ClassVisitor next) {
            super(Opcodes.ASM5, next);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.name = name;
            this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
            String adapter = superName == null ? null : superclass(superName);
            if (adapter != null) {
                this.replacedSuper = superName;
                superName = adapter;
            }
            this.superName = superName;
            this.interfaces = interfaces;
            this.copied = copiedSupers(this.replacedSuper != null ? this.replacedSuper : superName);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            String tooltip = TOOLTIPS.get(name + desc);
            if (tooltip != null && (access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) == 0 && !this.isInterface) {
                this.tooltips.add(new String[] {name, desc, tooltip});
                name = TOOLTIP_PREFIX + name;
            }
            if (CAPABILITY.equals(name) && MODERN_CAPABILITY_DESC.equals(desc) && (access & Opcodes.ACC_STATIC) == 0) this.modernCapabilities = true;
            if (("hasCapability".equals(name) && LEGACY_HAS_DESC.equals(desc)) || (CAPABILITY.equals(name) && LEGACY_CAPABILITY_DESC.equals(desc))) {
                this.legacyCapabilities = true;
            }
            MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
            String className = this.name;
            String replacedSuper = this.replacedSuper;
            String superName = this.superName;
            List<String> copied = this.copied;
            MethodNode method = new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                @Override
                public void visitEnd() {
                    if (!copied.isEmpty()) retargetToCopies(this, copied);
                    redirect(this, className, replacedSuper, superName);
                    accept(next);
                }
            };
            this.methods.add(method);
            return method;
        }

        @Override
        public void visitEnd() {
            for (String[] tooltip : this.tooltips) bridgeTooltip(this.cv, this.name, tooltip[0], tooltip[1], tooltip[2]);
            if (this.modernCapabilities && !this.legacyCapabilities && !this.isInterface) bridgeCapabilities(this.cv, this.name);
            ModernTranslator.this.linker.bridgeOverrides(this.cv, this.name, this.superName, this.interfaces, this.methods, this.isInterface);
            super.visitEnd();
        }
    }

    /**
     * Methods filling a tooltip, by 1.13 name and descriptor, to 1.12's descriptor: 1.13 fills the list with text
     * components, 1.12 with text. A class's own is renamed, and 1.12's name calls it with a {@code TooltipList}.
     */
    private static final java.util.Map<String, String> TOOLTIPS = new java.util.HashMap<>();
    private static final String TOOLTIP_PREFIX = "portium$tooltip$";
    private static final String TOOLTIP_LIST = "com/michaelsebero/portium/modern/shim/TooltipList";

    static {
        String rest = "Ljava/util/List;Lnet/minecraft/client/util/ITooltipFlag;)V";
        TOOLTIPS.put("func_77624_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;" + rest, "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;" + rest);
        TOOLTIPS.put("func_190948_a(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/IBlockReader;" + rest, "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;" + rest);
    }

    private static void bridgeTooltip(ClassVisitor out, String owner, String name, String desc, String legacyDesc) {
        MethodVisitor bridge = out.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, legacyDesc, null, null);
        bridge.visitCode();
        bridge.visitVarInsn(Opcodes.ALOAD, 0);
        bridge.visitVarInsn(Opcodes.ALOAD, 1);
        bridge.visitVarInsn(Opcodes.ALOAD, 2);
        bridge.visitVarInsn(Opcodes.ALOAD, 3);
        bridge.visitMethodInsn(Opcodes.INVOKESTATIC, TOOLTIP_LIST, "wrap", "(Ljava/util/List;)Ljava/util/List;", false);
        bridge.visitVarInsn(Opcodes.ALOAD, 4);
        bridge.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, TOOLTIP_PREFIX + name, desc, false);
        bridge.visitInsn(Opcodes.RETURN);
        bridge.visitMaxs(5, 5);
        bridge.visitEnd();
    }

    private static final String CAPABILITY = "getCapability";
    private static final String CAPABILITY_TYPE = "net/minecraftforge/common/capabilities/Capability";
    private static final String LAZY_OPTIONAL = "net/minecraftforge/common/util/LazyOptional";
    private static final String MODERN_CAPABILITY_DESC = "(L" + CAPABILITY_TYPE + ";Lnet/minecraft/util/EnumFacing;)L" + LAZY_OPTIONAL + ";";
    private static final String LEGACY_CAPABILITY_DESC = "(L" + CAPABILITY_TYPE + ";Lnet/minecraft/util/EnumFacing;)Ljava/lang/Object;";
    private static final String LEGACY_HAS_DESC = "(L" + CAPABILITY_TYPE + ";Lnet/minecraft/util/EnumFacing;)Z";
    private static final String CAPABILITY_GLUE = "com/michaelsebero/portium/modern/host/CapabilityGlue";

    /**
     * Forge 1.12 asks a capability provider {@code hasCapability} and {@code getCapability} returning the capability;
     * Forge 1.13 asks only {@code getCapability}, returning a {@code LazyOptional}. A class declaring 1.13's gets
     * 1.12's two, which ask it. (Java can't write both {@code getCapability}s in one class, so they're made here.)
     */
    private static void bridgeCapabilities(ClassVisitor visitor, String owner) {
        MethodVisitor get = visitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, CAPABILITY, LEGACY_CAPABILITY_DESC, null, null);
        get.visitCode();
        get.visitVarInsn(Opcodes.ALOAD, 0);
        get.visitVarInsn(Opcodes.ALOAD, 1);
        get.visitVarInsn(Opcodes.ALOAD, 2);
        get.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, CAPABILITY, MODERN_CAPABILITY_DESC, false);
        get.visitInsn(Opcodes.ACONST_NULL);
        get.visitMethodInsn(Opcodes.INVOKEVIRTUAL, LAZY_OPTIONAL, "orElse", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        get.visitInsn(Opcodes.ARETURN);
        get.visitMaxs(3, 3);
        get.visitEnd();

        MethodVisitor has = visitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "hasCapability", LEGACY_HAS_DESC, null, null);
        has.visitCode();
        has.visitVarInsn(Opcodes.ALOAD, 0);
        has.visitVarInsn(Opcodes.ALOAD, 1);
        has.visitVarInsn(Opcodes.ALOAD, 2);
        has.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner, CAPABILITY, MODERN_CAPABILITY_DESC, false);
        has.visitMethodInsn(Opcodes.INVOKEVIRTUAL, LAZY_OPTIONAL, "isPresent", "()Z", false);
        has.visitInsn(Opcodes.IRETURN);
        has.visitMaxs(3, 3);
        has.visitEnd();
    }

    private void redirect(MethodNode method, String className, @Nullable String replacedSuper, String superName) {
        Set<MethodInsnNode> made = nativeConstructions(method);
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode && made.contains(insn)) continue;
            if (insn instanceof MethodInsnNode) {
                redirect(method, (MethodInsnNode) insn, replacedSuper, superName);
            } else if (insn instanceof InvokeDynamicInsnNode) {
                redirect((InvokeDynamicInsnNode) insn);
            } else if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW && !((TypeInsnNode) insn).desc.startsWith(ModernMappings.NATIVE)) {
                String adapter = this.redirects.adapter(((TypeInsnNode) insn).desc);
                if (adapter != null) ((TypeInsnNode) insn).desc = adapter;
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                int opcode = insn.getOpcode();
                boolean read = opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD;
                String getter = read ? this.redirects.findField(field.owner, field.name, field.desc, this.hierarchy)
                        : this.redirects.findFieldSetter(field.owner, field.name, field.desc, this.hierarchy);
                if (getter == null && opcode != Opcodes.GETSTATIC) continue;
                if (getter != null) {
                    AbstractInsnNode replacement = converter(getter).getFirst();
                    method.instructions.set(insn, replacement);
                    insn = replacement;
                } else {
                    AbstractInsnNode before = insn.getPrevious();
                    if (this.linker.link(method, field)) insn = before == null ? method.instructions.getFirst() : before.getNext();
                }
            }
        }
    }

    private void redirect(MethodNode method, MethodInsnNode call, @Nullable String replacedSuper, String superName) {
        if ("java/lang/Thread".equals(call.owner) && "getContextClassLoader".equals(call.name)) {
            set(call, Opcodes.INVOKESTATIC, HOOKS, "contextClassLoader", "(Ljava/lang/Thread;)Ljava/lang/ClassLoader;");
            return;
        }
        if ("<init>".equals(call.name)) {
            // super(...) of an adapted class, or new of one.
            String adapter = this.redirects.adapter(call.owner);
            if (adapter != null) {
                call.owner = adapter;
                return;
            }
            // super(...) of a class extending 1.13's own version of a block class.
            if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.owner.equals(replacedSuper) && superName.startsWith(ModernMappings.NATIVE)) {
                call.owner = superName;
                return;
            }
            ModernRedirects.Constructor constructor = this.redirects.findConstructor(call.owner, call.desc);
            if (constructor != null) {
                convertArguments(method, call, constructor);
            } else {
                this.linker.link(method, call);
            }
            return;
        }
        if (call.getOpcode() == Opcodes.INVOKESPECIAL && TOOLTIPS.containsKey(call.name + call.desc) && this.hierarchy.declaresModern(call.owner, call.name, call.desc)) {
            // super's tooltip, which a 1.13 superclass has under its renamed name.
            call.name = TOOLTIP_PREFIX + call.name;
        }
        if (call.getOpcode() == Opcodes.INVOKESPECIAL && replacedSuper != null && call.owner.equals(replacedSuper)) call.owner = superName;
        if (call.getOpcode() == Opcodes.INVOKESPECIAL && CAPABILITY.equals(call.name) && MODERN_CAPABILITY_DESC.equals(call.desc) && isParentClass(call.owner)) {
            // super.getCapability: 1.12's, made a LazyOptional.
            call.desc = LEGACY_CAPABILITY_DESC;
            method.instructions.insert(call, new MethodInsnNode(Opcodes.INVOKESTATIC, CAPABILITY_GLUE, "optional",
                    "(Ljava/lang/Object;)L" + LAZY_OPTIONAL + ";", false));
            return;
        }
        ModernRedirects.Redirect redirect = this.redirects.find(call.owner, call.name, call.desc, this.hierarchy);
        // super.m() of a method 1.12's superclass lacks takes the stand-in, as long as nothing up there has the method.
        if (redirect != null && call.getOpcode() == Opcodes.INVOKESPECIAL && this.linker.superHasMethod(call)) redirect = null;
        // A mod's class with its own version of the method keeps it.
        if (redirect != null && call.getOpcode() != Opcodes.INVOKESTATIC && this.hierarchy.declaresBelowAdapters(call.owner, call.name, call.desc)) redirect = null;
        if (redirect != null) {
            set(call, Opcodes.INVOKESTATIC, redirect.owner, redirect.name, redirect.desc(call.getOpcode(), call.desc));
            return;
        }
        this.linker.link(method, call);
        fixInterfaceCall(call);
    }

    /**
     * The superclasses, by 1.13 names, that a class with this superclass reaches through Minecraft 1.13.2's own
     * versions of them (see {@link #superclass}), nearest first.
     */
    private List<String> copiedSupers(@Nullable String superName) {
        List<String> chain = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String type = superName; type != null && seen.add(type); ) {
            String replaced = superclass(type);
            if (replaced != null && !replaced.startsWith(ModernMappings.NATIVE)) break;
            if (replaced != null) chain.add(type);
            type = this.hierarchy.modernSuperName(type);
        }
        return chain;
    }

    /**
     * References to members of superclasses this class reaches through Minecraft 1.13.2's own versions of them go to
     * those versions, which declare them (1.12's classes of those names may not).
     */
    private void retargetToCopies(MethodNode method, List<String> copied) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("<init>".equals(call.name)) continue;
                String copy = declaringCopy(copied, call.owner, call.name, call.desc, false);
                if (copy != null) {
                    call.owner = copy;
                    if (call.getOpcode() == Opcodes.INVOKEINTERFACE) call.setOpcode(Opcodes.INVOKEVIRTUAL);
                    call.itf = false;
                }
            } else if (insn instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) insn;
                String copy = declaringCopy(copied, field.owner, field.name, field.desc, true);
                if (copy != null) field.owner = copy;
            } else if (insn.getOpcode() == Opcodes.CHECKCAST || insn.getOpcode() == Opcodes.INSTANCEOF) {
                // Casts to those superclasses are to the versions this class is built on.
                TypeInsnNode cast = (TypeInsnNode) insn;
                if (copied.contains(cast.desc)) cast.desc = ModernMappings.NATIVE + cast.desc;
            }
        }
    }

    @Nullable
    private String declaringCopy(List<String> copied, String owner, String name, String desc, boolean field) {
        int from = copied.indexOf(owner);
        if (from < 0) return null;
        for (int i = from; i < copied.size(); i++) {
            if (this.hierarchy.nativeDeclares(copied.get(i), name, desc, field)) return ModernMappings.NATIVE + copied.get(i);
        }
        return null;
    }

    /**
     * Makes {@code new X(...)} of a class whose 1.13 constructor 1.12's class lacks (1.13 builds blocks from
     * properties, say) make Minecraft 1.13.2's own X instead (see {@link #superclass}): a mod making a plain slab gets
     * a 1.13 slab. Returns the constructor calls changed.
     */
    private Set<MethodInsnNode> nativeConstructions(MethodNode method) {
        Set<MethodInsnNode> changed = new HashSet<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() != Opcodes.NEW) continue;
            TypeInsnNode made = (TypeInsnNode) insn;
            if (this.redirects.adapter(made.desc) != null) continue;
            // Any class made with a constructor only 1.13's has (a chunk palette, say) is made as 1.13's.
            String nativeVersion = nativeVersionOfAny(made.desc);
            if (nativeVersion == null) continue;
            MethodInsnNode constructor = constructorOf(made);
            if (constructor == null || this.redirects.findConstructor(constructor.owner, constructor.desc) != null) continue;
            if (this.linker.hasConstructor(made.desc, constructor.desc)) continue;
            made.desc = nativeVersion;
            constructor.owner = nativeVersion;
            changed.add(constructor);
        }
        if (!changed.isEmpty()) retargetUses(method);
        return changed;
    }

    /**
     * Code using what a changed {@code new} made, typed by 1.12's class (as in {@code new BlockSlab(...).setRegistryName(...)}),
     * uses 1.13's: calls, field accesses and casts on that value are retargeted. Values are followed through copies
     * (dups, locals, casts) by their sources, which needs no class loading.
     */
    private static void retargetUses(MethodNode method) {
        Frame<SourceValue>[] frames;
        try {
            frames = new Analyzer<>(new SourceInterpreter()).analyze("java/lang/Object", method);
        } catch (AnalyzerException e) {
            return;
        }
        AbstractInsnNode[] insns = method.instructions.toArray();
        Map<AbstractInsnNode, String> derived = new HashMap<>();
        for (AbstractInsnNode insn : insns) {
            if (insn.getOpcode() == Opcodes.NEW && ((TypeInsnNode) insn).desc.startsWith(ModernMappings.NATIVE)) derived.put(insn, ((TypeInsnNode) insn).desc);
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (int i = 0; i < insns.length; i++) {
                AbstractInsnNode insn = insns[i];
                Frame<SourceValue> frame = frames[i];
                if (frame == null || derived.containsKey(insn)) continue;
                SourceValue input = copiedValue(insn, frame);
                String type = input == null ? null : derivedType(input, derived);
                if (type != null) {
                    derived.put(insn, type);
                    grew = true;
                }
            }
        }
        for (int i = 0; i < insns.length; i++) {
            AbstractInsnNode insn = insns[i];
            Frame<SourceValue> frame = frames[i];
            if (frame == null) continue;
            if (insn instanceof MethodInsnNode && insn.getOpcode() != Opcodes.INVOKESTATIC && insn.getOpcode() != Opcodes.INVOKESPECIAL) {
                MethodInsnNode call = (MethodInsnNode) insn;
                int arguments = 0;
                for (Type argument : Type.getArgumentTypes(call.desc)) arguments += argument.getSize();
                String type = receiverType(frame, arguments, derived);
                if (type != null && type.equals(ModernMappings.NATIVE + call.owner)) {
                    call.owner = type;
                    call.setOpcode(Opcodes.INVOKEVIRTUAL);
                    call.itf = false;
                }
            } else if (insn instanceof FieldInsnNode && (insn.getOpcode() == Opcodes.GETFIELD || insn.getOpcode() == Opcodes.PUTFIELD)) {
                FieldInsnNode field = (FieldInsnNode) insn;
                String type = receiverType(frame, insn.getOpcode() == Opcodes.PUTFIELD ? Type.getType(field.desc).getSize() : 0, derived);
                if (type != null && type.equals(ModernMappings.NATIVE + field.owner)) field.owner = type;
            } else if (insn.getOpcode() == Opcodes.CHECKCAST || insn.getOpcode() == Opcodes.INSTANCEOF) {
                TypeInsnNode cast = (TypeInsnNode) insn;
                String type = derivedType(frame.getStack(frame.getStackSize() - 1), derived);
                if (type != null && type.equals(ModernMappings.NATIVE + cast.desc)) cast.desc = type;
            }
        }
    }

    /** The value a copying instruction (dup, load, store, cast) passes on, or null for any other. */
    @Nullable
    private static SourceValue copiedValue(AbstractInsnNode insn, Frame<SourceValue> frame) {
        switch (insn.getOpcode()) {
            case Opcodes.DUP:
            case Opcodes.DUP_X1:
            case Opcodes.DUP_X2:
            case Opcodes.ASTORE:
            case Opcodes.CHECKCAST:
                return frame.getStackSize() > 0 ? frame.getStack(frame.getStackSize() - 1) : null;
            case Opcodes.ALOAD:
                return frame.getLocal(((VarInsnNode) insn).var);
            default:
                return null;
        }
    }

    @Nullable
    private static String derivedType(SourceValue value, Map<AbstractInsnNode, String> derived) {
        for (AbstractInsnNode source : value.insns) {
            String type = derived.get(source);
            if (type != null) return type;
        }
        return null;
    }

    @Nullable
    private static String receiverType(Frame<SourceValue> frame, int argumentSlots, Map<AbstractInsnNode, String> derived) {
        int index = frame.getStackSize() - 1;
        for (int slots = 0; slots < argumentSlots && index >= 0; index--) slots += frame.getStack(index).getSize();
        return index < 0 ? null : derivedType(frame.getStack(index), derived);
    }

    /** The constructor call that initialises what a {@code new} makes, matching nested ones of the same class. */
    @Nullable
    private static MethodInsnNode constructorOf(TypeInsnNode made) {
        int depth = 0;
        for (AbstractInsnNode insn = made.getNext(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.NEW && ((TypeInsnNode) insn).desc.equals(made.desc)) {
                depth++;
            } else if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode call = (MethodInsnNode) insn;
                if ("<init>".equals(call.name) && call.owner.equals(made.desc) && depth-- == 0) return call;
            }
        }
        return null;
    }

    /** Whether a 1.13 class name is one the parent class loader provides: Minecraft's or an adapter. */
    private boolean isParentClass(String owner) {
        return owner.startsWith("net/minecraft/") || owner.startsWith("com/michaelsebero/portium/");
    }

    /**
     * A call through a class 1.13 has as an interface and 1.12 as a class, or the other way round (1.13's
     * {@code IWorld} is 1.12's {@code World}), needs the other invoke instruction.
     */
    private void fixInterfaceCall(MethodInsnNode call) {
        int opcode = call.getOpcode();
        if (opcode != Opcodes.INVOKEINTERFACE && opcode != Opcodes.INVOKEVIRTUAL) return;
        String mapped = this.mappings.mapClass(call.owner);
        if (mapped.equals(call.owner) || mapped.startsWith("[")) return;
        Boolean isInterface = this.hierarchy.isInterface(mapped);
        if (isInterface == null) return;
        if (isInterface && opcode == Opcodes.INVOKEVIRTUAL) {
            call.setOpcode(Opcodes.INVOKEINTERFACE);
            call.itf = true;
        } else if (!isInterface && opcode == Opcodes.INVOKEINTERFACE) {
            call.setOpcode(Opcodes.INVOKEVIRTUAL);
            call.itf = false;
        }
    }

    private static void set(MethodInsnNode call, int opcode, String owner, String name, String desc) {
        call.setOpcode(opcode);
        call.owner = owner;
        call.name = name;
        call.desc = desc;
        call.itf = false;
    }

    /**
     * Makes 1.12's constructor arguments from 1.13's where they lie on the stack. A last argument that only needs
     * converting is converted in place; otherwise 1.13's arguments are parked in fresh locals and 1.12's loaded from
     * them. Nothing branches in between, so the method's frames still hold.
     */
    private static void convertArguments(MethodNode method, MethodInsnNode call, ModernRedirects.Constructor constructor) {
        Type[] modern = Type.getArgumentTypes(call.desc);
        ModernRedirects.Argument[] arguments = constructor.arguments;
        InsnList code = new InsnList();
        if (inPlace(modern.length, arguments)) {
            code.add(converter(arguments[arguments.length - 1].converter));
        } else {
            int[] slots = new int[modern.length];
            int next = method.maxLocals;
            for (int i = 0; i < modern.length; i++) {
                slots[i] = next;
                next += modern[i].getSize();
            }
            method.maxLocals = next;
            for (int i = modern.length - 1; i >= 0; i--) code.add(new VarInsnNode(modern[i].getOpcode(Opcodes.ISTORE), slots[i]));
            for (ModernRedirects.Argument argument : arguments) {
                code.add(new VarInsnNode(modern[argument.from].getOpcode(Opcodes.ILOAD), slots[argument.from]));
                code.add(converter(argument.converter));
            }
        }
        method.instructions.insertBefore(call, code);
        method.maxStack += 2 * arguments.length;
        call.desc = ModernMappings.legacyDesc(constructor.desc);
    }

    /** Whether 1.12's arguments are 1.13's in order, with at most the last converted into another value. */
    private static boolean inPlace(int count, ModernRedirects.Argument[] arguments) {
        if (arguments.length != count || count == 0) return false;
        for (int i = 0; i < count; i++) {
            if (arguments[i].from != i || i < count - 1 && arguments[i].converter != null) return false;
        }
        return arguments[count - 1].producesArgument();
    }

    /** A static call from {@code owner.name desc}; empty for null. */
    private static InsnList converter(@Nullable String method) {
        InsnList code = new InsnList();
        if (method == null) return code;
        int dot = method.indexOf('.');
        int paren = method.indexOf('(');
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, method.substring(0, dot), method.substring(dot + 1, paren), method.substring(paren), false));
        return code;
    }

    /** Method references to redirected methods, as in {@code this::addParticle}. */
    private void redirect(InvokeDynamicInsnNode call) {
        Object[] arguments = call.bsmArgs;
        for (int i = 0; i < arguments.length; i++) {
            if (!(arguments[i] instanceof Handle)) continue;
            Handle handle = (Handle) arguments[i];
            int tag = handle.getTag();
            if (tag != Opcodes.H_INVOKEVIRTUAL && tag != Opcodes.H_INVOKEINTERFACE && tag != Opcodes.H_INVOKESTATIC) continue;
            ModernRedirects.Redirect redirect = this.redirects.find(handle.getOwner(), handle.getName(), handle.getDesc(), this.hierarchy);
            if (redirect == null) {
                Handle linked = this.linker.link(handle);
                if (linked != null) arguments[i] = linked;
                continue;
            }
            int opcode = tag == Opcodes.H_INVOKESTATIC ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
            arguments[i] = new Handle(Opcodes.H_INVOKESTATIC, redirect.owner, redirect.name, redirect.desc(opcode, handle.getDesc()), false);
        }
    }
}
