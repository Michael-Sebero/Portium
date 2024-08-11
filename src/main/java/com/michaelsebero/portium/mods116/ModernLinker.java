package com.michaelsebero.portium.mods116;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Links what 1.13 code asks of 1.12's classes where the names alone don't (see {@link ModernTranslator}):
 * <ul>
 * <li>A method 1.12 has with other parameters, under the same SRG id, is called with the arguments and result
 * converted: cast where 1.13 takes a supertype ({@code IWorldReader} for 1.12's {@code World}), or passed through
 * {@code shim.Conversions}; trailing arguments 1.12 no longer takes are dropped.</li>
 * <li>A mod's override of such a method gets a bridge with 1.12's parameters, so 1.12 calls it.</li>
 * <li>A method or static field 1.12 lacks is Minecraft 1.13.2's own ({@link ModernBridges}).</li>
 * <li>A member 1.12 has but keeps private, or in a package of another class loader, is reached through a method
 * handle ({@link ModernAccess}).</li>
 * </ul>
 */
final class ModernLinker {

    private static final String PORTIUM = "com/michaelsebero/portium/";
    private static final String ADAPTERS = "com/michaelsebero/portium/modern/adapter/";
    private static final String ADAPTERS116 = "com/michaelsebero/portium/mods116/adapter/";

    private static boolean isAdapter(String owner) {
        return owner.startsWith(ADAPTERS) || owner.startsWith(ADAPTERS116);
    }
    private static final String CONVERSIONS = "com/michaelsebero/portium/modern/shim/Conversions.";
    private static final String ACCESS = "com/michaelsebero/portium/modern/ModernAccess";
    private static final String SHAPE = "com/michaelsebero/portium/modern/mc/util/math/shapes/VoxelShape";
    private static final String BOX = "net/minecraft/util/math/AxisAlignedBB";

    private final ModernMappings mappings;
    private final ModernMembers members;
    private final ModernRedirects.Hierarchy hierarchy;
    private final Remapper names;
    @Nullable
    private final ModernBridges bridges;
    private final ModernSignatures signatures = new ModernSignatures();
    /** Static methods ({@code owner.name desc}) converting one type to another, keyed {@code from>to}; 1.12 names. */
    private final Map<String, String> converters = new HashMap<>();

    ModernLinker(ModernMappings mappings, ModernMembers members, ModernRedirects.Hierarchy hierarchy, Remapper names, @Nullable ModernBridges bridges) {
        this.mappings = mappings;
        this.members = members;
        this.hierarchy = hierarchy;
        this.names = names;
        this.bridges = bridges;
        converter("java/util/function/Predicate", "com/google/common/base/Predicate", "guavaPredicate");
        converter("java/util/function/Function", "com/google/common/base/Function", "guavaFunction");
        converter("java/util/function/Supplier", "com/google/common/base/Supplier", "guavaSupplier");
        converter(SHAPE, BOX, "bounds");
        converter(BOX, SHAPE, "shape");
        converter("net/minecraft/util/text/ITextComponent", "java/lang/String", "text");
        converter("net/minecraft/util/registry/IRegistry", "java/util/Map", "registryMap");
    }

    private void converter(String from, String to, String method) {
        this.converters.put(from + ">" + to, CONVERSIONS + method + "(L" + from + ";)L" + to + ";");
    }

    // Conversions

    /** Code turning a value of one type into another (1.12 names), or null if there's no telling how. */
    @Nullable
    private InsnList convert(Type from, Type to) {
        InsnList code = new InsnList();
        if (from.equals(to)) return code;
        // 1.12 seeds what 1.16 hands a random for (a baked model's quads), and the other way round.
        if (from.getSort() == Type.LONG && "java/util/Random".equals(to.getSort() == Type.OBJECT ? to.getInternalName() : null)) {
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS.substring(0, CONVERSIONS.length() - 1), "random", "(J)Ljava/util/Random;", false));
            return code;
        }
        if (to.getSort() == Type.LONG && "java/util/Random".equals(from.getSort() == Type.OBJECT ? from.getInternalName() : null)) {
            code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Random", "nextLong", "()J", false));
            return code;
        }
        if (!isReference(from) || !isReference(to)) return null;
        if (to.getSort() == Type.OBJECT && "java/lang/Object".equals(to.getInternalName())) return code;
        if (from.getSort() == Type.ARRAY || to.getSort() == Type.ARRAY) return null;
        String source = from.getInternalName();
        String target = to.getInternalName();
        if (this.hierarchy.isTranslatedSubtype(source, target)) return code;
        String converter = this.converters.get(source + ">" + target);
        if (converter != null) {
            int dot = converter.indexOf('.');
            int paren = converter.indexOf('(');
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, converter.substring(0, dot), converter.substring(dot + 1, paren),
                    ModernMappings.legacyDesc(converter.substring(paren)), false));
            return code;
        }
        if (this.hierarchy.isTranslatedSubtype(target, source)) {
            code.add(new TypeInsnNode(Opcodes.CHECKCAST, ModernMappings.legacy(target)));
            return code;
        }
        return null;
    }

    private static boolean isReference(Type type) {
        return type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY;
    }

    private static InsnList pop(Type type) {
        InsnList code = new InsnList();
        code.add(new InsnNode(type.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        return code;
    }

    /** Whether a translated class is one of the parent class loader's, other than Java's own. */
    private boolean isParentType(String type) {
        // Libraries the 1.16 mods get their own copies of (fastutil) may share names with the parent's older ones.
        if (ModernClassLoader.owns(type.replace('/', '.'))) return false;
        return !type.startsWith("java/") && !type.startsWith("[") && this.members.type(type) != null;
    }

    /** Whether 1.12's method is the one 1.13 calls by this name: the same SRG id, or for Forge's methods the same name. */
    private boolean sameMethod(String modernName, ModernMembers.Member candidate) {
        if (!modernName.startsWith("func_")) return true;
        return modernName.equals(this.mappings.srgName(candidate.ownerName(), candidate.name, candidate.desc));
    }

    /** Whether one of the parent's classes (by 1.12 name, as 1.16 names it too) has the static method. */
    boolean hasStatic(String owner, String name, String desc) {
        return this.members.method(owner, name, desc) != null;
    }

    /** Whether 1.12's class has a constructor 1.13 code calls, both by 1.13's names. */
    boolean hasConstructor(String owner, String desc) {
        return this.members.method(this.mappings.mapClass(owner), "<init>", this.names.mapMethodDesc(desc)) != null;
    }

    // Calls

    /**
     * Links a call through one of 1.12's classes that 1.12 can't make as written; true if the call changed. The method
     * and call use 1.13's names, as the translator sees them before renaming.
     */
    boolean link(MethodNode method, MethodInsnNode call) {
        if ("<init>".equals(call.name)) return linkConstructor(method, call);
        if (call.getOpcode() != Opcodes.INVOKESTATIC && linkRenamed(method, call)) return true;
        if (call.getOpcode() == Opcodes.INVOKESPECIAL) return linkSuper(method, call);
        String owner = this.names.map(call.owner);
        if (!isParentType(owner)) return linkInherited(method, call, owner);
        String name = this.mappings.mapMember(call.owner, call.name);
        String desc = this.names.mapMethodDesc(call.desc);
        boolean isStatic = call.getOpcode() == Opcodes.INVOKESTATIC;
        // 1.12's method must be static or not as the call is (1.13 made World.isOutsideBuildHeight static).
        ModernMembers.Member existing = this.members.method(owner, name, desc);
        if (existing != null && existing.isStatic() == isStatic) return false;
        for (ModernMembers.Member candidate : this.members.methodsNamed(owner, name)) {
            if (candidate.isStatic() != isStatic || candidate.isPrivate() || !sameMethod(call.name, candidate)) continue;
            if (adapt(method, call, desc, candidate)) return true;
        }
        if (owner.startsWith("org/lwjgl/") && linkLwjgl(call, owner, desc)) return true;
        ModernBridges.Bridge bridge = bridge(owner, call.owner, call.name, call.desc, isStatic);
        if (bridge == null) return false;
        call.setOpcode(Opcodes.INVOKESTATIC);
        call.owner = bridge.owner;
        call.name = bridge.name;
        call.desc = bridge.desc;
        call.itf = false;
        return true;
    }

    /**
     * A call through a mod's class (or one of 1.13.2's copies) to a method none of those declare, which it inherits
     * from the first of 1.12's classes above: adapted to 1.12's version there, as a call through that class would be.
     */
    private boolean linkInherited(MethodNode method, MethodInsnNode call, String owner) {
        if (call.getOpcode() == Opcodes.INVOKESTATIC || this.hierarchy.declaresBelowAdapters(call.owner, call.name, call.desc)) return false;
        String ancestor = parentAncestor(owner);
        if (ancestor == null) return false;
        String name = this.mappings.mapMember(call.owner, call.name);
        String desc = this.names.mapMethodDesc(call.desc);
        if (this.members.method(ancestor, name, desc) != null) return false;
        for (ModernMembers.Member candidate : this.members.methodsNamed(ancestor, name)) {
            if (candidate.isStatic() || candidate.isPrivate() || !sameMethod(call.name, candidate)) continue;
            if (adapt(method, call, desc, candidate)) return true;
        }
        return false;
    }

    /**
     * {@code super.m(...)} of a method 1.12's superclass has with other parameters (1.13 takes an {@code IBlockReader}
     * where 1.12 takes a {@code World}): 1.12's, the arguments converted. No bridges: a super call must stay one.
     */
    private boolean linkSuper(MethodNode method, MethodInsnNode call) {
        if ("<init>".equals(call.name)) return false;
        String target = superTarget(call);
        if (target == null) return false;
        String name = this.mappings.mapMember(call.owner, call.name);
        String desc = this.names.mapMethodDesc(call.desc);
        if (this.members.method(target, name, desc) != null) return false;
        for (ModernMembers.Member candidate : this.members.methodsNamed(target, name)) {
            if (candidate.isStatic() || candidate.isPrivate() || !sameMethod(call.name, candidate)) continue;
            if (adapt(method, call, desc, candidate)) return true;
        }
        return false;
    }

    /** The parent's class a super call reaches (its owner, or the first of the parent's above it); null if a 1.13 class has the method. */
    @Nullable
    private String superTarget(MethodInsnNode call) {
        String owner = this.names.map(call.owner);
        if (isParentType(owner)) return owner;
        if (this.hierarchy.declaresBelowAdapters(call.owner, call.name, call.desc)) return null;
        return parentAncestor(owner);
    }

    /**
     * Whether a super call reaches a method the parent's classes have as called, so a stand-in mustn't replace it (an
     * adapter's own method, which a stand-in might call back into).
     */
    boolean superHasMethod(MethodInsnNode call) {
        String target = superTarget(call);
        if (target == null) return true;
        return this.members.method(target, this.mappings.mapMember(call.owner, call.name), this.names.mapMethodDesc(call.desc)) != null;
    }

    /** The first of the parent's classes above a class of this loader's (translated names), or null. */
    @Nullable
    private String parentAncestor(String type) {
        Set<String> seen = new HashSet<>();
        for (String current = type; current != null && seen.add(current); ) {
            if (isParentType(current)) return current;
            String[] supertypes = this.hierarchy.supertypes(current);
            current = supertypes.length > 0 ? supertypes[0] : null;
        }
        return null;
    }

    /**
     * 1.13 classes whose objects 1.13 passes to constructors that 1.12's don't take: a tile entity's or entity's type,
     * which Portium finds by class instead.
     */
    private static final Set<String> DROPPABLE = new HashSet<>(java.util.Arrays.asList(
            "com/michaelsebero/portium/modern/mc/tileentity/TileEntityType", "com/michaelsebero/portium/modern/mc/entity/EntityType"));

    /**
     * A constructor of one of 1.12's classes that 1.12 has with other parameters (as in {@code super(type)} of a 1.13
     * tile entity, or a text component where 1.12 takes a string): 1.12's, with type arguments dropped and the rest
     * converted. Names are 1.13's.
     */
    private boolean linkConstructor(MethodNode method, MethodInsnNode call) {
        String owner = this.names.map(call.owner);
        if (!isParentType(owner) || owner.startsWith(PORTIUM)) return false;
        String desc = this.names.mapMethodDesc(call.desc);
        if (this.members.method(owner, "<init>", desc) != null) return false;
        Type[] modern = Type.getArgumentTypes(desc);
        List<Integer> kept = new ArrayList<>();
        for (int i = 0; i < modern.length; i++) {
            if (modern[i].getSort() != Type.OBJECT || !DROPPABLE.contains(modern[i].getInternalName())) kept.add(i);
        }
        List<List<Integer>> options = new ArrayList<>();
        options.add(kept);
        options.add(allIndices(modern.length));
        // 1.13 entities take their size in the constructor; 1.12's set it after.
        List<Integer> sized = null;
        int n = kept.size();
        if (n >= 2 && modern[kept.get(n - 1)].getSort() == Type.FLOAT && modern[kept.get(n - 2)].getSort() == Type.FLOAT && isEntity(owner)) {
            sized = new ArrayList<>(kept.subList(0, n - 2));
            options.add(sized);
        }
        for (ModernMembers.Member candidate : this.members.methodsNamed(owner, "<init>")) {
            if (!candidate.ownerName().equals(owner)) continue;
            Type[] legacy = Type.getArgumentTypes(candidate.desc);
            for (List<Integer> from : options) {
                if (from.size() != legacy.length) continue;
                // In order, or else the one way 1.13's arguments fit 1.12's (1.13 moved the world to the end).
                int[] order = inOrder(from, modern, legacy);
                if (order == null) order = matching(from, modern, legacy);
                if (order == null) continue;
                InsnList[] conversions = new InsnList[legacy.length];
                for (int i = 0; i < legacy.length; i++) conversions[i] = convert(modern[order[i]], legacy[i]);
                int[] slots = new int[modern.length];
                int next = method.maxLocals;
                for (int i = 0; i < modern.length; i++) {
                    slots[i] = next;
                    next += modern[i].getSize();
                }
                InsnList code = new InsnList();
                for (int i = modern.length - 1; i >= 0; i--) code.add(new VarInsnNode(modern[i].getOpcode(Opcodes.ISTORE), slots[i]));
                for (int i = 0; i < legacy.length; i++) {
                    code.add(new VarInsnNode(modern[order[i]].getOpcode(Opcodes.ILOAD), slots[order[i]]));
                    code.add(conversions[i]);
                }
                method.maxLocals = next;
                method.maxStack += 4;
                method.instructions.insertBefore(call, code);
                call.desc = ModernMappings.legacyDesc(candidate.desc);
                if (from == sized) {
                    InsnList size = new InsnList();
                    size.add(isThisInit(method, call) ? new VarInsnNode(Opcodes.ALOAD, 0) : new InsnNode(Opcodes.DUP));
                    size.add(new VarInsnNode(Opcodes.FLOAD, slots[kept.get(n - 2)]));
                    size.add(new VarInsnNode(Opcodes.FLOAD, slots[kept.get(n - 1)]));
                    size.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/entity/Entity", "func_70105_a", "(FF)V", false));
                    method.instructions.insert(call, size);
                }
                return true;
            }
        }
        return false;
    }

    private boolean isEntity(String owner) {
        Class<?> type = this.members.type(owner);
        Class<?> entity = this.members.type("net/minecraft/entity/Entity");
        return type != null && entity != null && entity.isAssignableFrom(type);
    }

    /** 1.13's arguments for 1.12's in the same order, if they convert; null otherwise. */
    @Nullable
    private int[] inOrder(List<Integer> from, Type[] modern, Type[] legacy) {
        int[] order = new int[legacy.length];
        for (int i = 0; i < legacy.length; i++) {
            if (convert(modern[from.get(i)], legacy[i]) == null) return null;
            order[i] = from.get(i);
        }
        return order;
    }

    /**
     * The only way to give each of 1.12's arguments a different one of 1.13's that converts to it; or, if there are
     * several (six doubles in a row), the one keeping arguments of a type in their order. Null if none.
     */
    @Nullable
    private int[] matching(List<Integer> from, Type[] modern, Type[] legacy) {
        int[][] found = new int[2][];
        int[] count = {0};
        match(0, new int[legacy.length], new boolean[from.size()], from, modern, legacy, found, count);
        if (count[0] == 1) return found[0];
        if (count[0] == 0) return null;
        // Each of 1.12's arguments takes the first unused one of 1.13's of exactly its type, else the first that converts.
        int[] order = new int[legacy.length];
        boolean[] used = new boolean[from.size()];
        for (int i = 0; i < legacy.length; i++) {
            int chosen = -1;
            for (int j = 0; j < from.size() && chosen < 0; j++) {
                if (!used[j] && modern[from.get(j)].equals(legacy[i])) chosen = j;
            }
            for (int j = 0; j < from.size() && chosen < 0; j++) {
                if (!used[j] && convert(modern[from.get(j)], legacy[i]) != null) chosen = j;
            }
            if (chosen < 0) return null;
            used[chosen] = true;
            order[i] = from.get(chosen);
        }
        return order;
    }

    private void match(int index, int[] order, boolean[] used, List<Integer> from, Type[] modern, Type[] legacy, int[][] found, int[] count) {
        if (count[0] > 1) return;
        if (index == legacy.length) {
            found[Math.min(count[0], 1)] = order.clone();
            count[0]++;
            return;
        }
        for (int j = 0; j < from.size(); j++) {
            if (used[j] || convert(modern[from.get(j)], legacy[index]) == null) continue;
            used[j] = true;
            order[index] = from.get(j);
            match(index + 1, order, used, from, modern, legacy, found, count);
            used[j] = false;
        }
    }

    /** Whether a constructor call initialises the object under construction ({@code super(...)} or {@code this(...)}). */
    private static boolean isThisInit(MethodNode method, MethodInsnNode call) {
        if (!"<init>".equals(method.name)) return false;
        int pending = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null && insn != call; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.NEW) {
                pending++;
            } else if (insn.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(((MethodInsnNode) insn).name)) {
                if (pending == 0) return false;
                pending--;
            }
        }
        return pending == 0;
    }

    private static List<Integer> allIndices(int count) {
        List<Integer> all = new ArrayList<>(count);
        for (int i = 0; i < count; i++) all.add(i);
        return all;
    }

    /**
     * A call to a 1.13 method that's a 1.12 method under another id ({@link ModernSignatures}): to 1.12's, with the
     * arguments rearranged, unless a 1.13 class on the way up declares the 1.13 method itself.
     */
    private boolean linkRenamed(MethodNode method, MethodInsnNode call) {
        ModernSignatures.Renamed renamed = this.signatures.find(call.owner, call.name, call.desc, this.hierarchy);
        if (renamed == null || this.hierarchy.declaresBelowAdapters(call.owner, call.name, call.desc)) return false;
        Type[] modern = Type.getArgumentTypes(this.names.mapMethodDesc(call.desc));
        Type[] legacy = Type.getArgumentTypes(renamed.legacyDesc);
        InsnList[] arguments = new InsnList[legacy.length];
        int[] slots = new int[modern.length];
        int next = method.maxLocals;
        for (int i = 0; i < modern.length; i++) {
            slots[i] = next;
            next += modern[i].getSize();
        }
        for (int i = 0; i < legacy.length; i++) {
            int from = renamed.legacyFrom[i];
            if (from >= 0) {
                InsnList conversion = convert(modern[from], legacy[i]);
                if (conversion == null) return false;
                arguments[i] = new InsnList();
                arguments[i].add(new VarInsnNode(modern[from].getOpcode(Opcodes.ILOAD), slots[from]));
                arguments[i].add(conversion);
            } else {
                arguments[i] = standIn(from, legacy[i], modern, slots);
                if (arguments[i] == null) return false;
            }
        }
        InsnList returned = returnConversion(Type.getReturnType(renamed.legacyDesc), Type.getReturnType(this.names.mapMethodDesc(call.desc)));
        if (returned == null) return false;
        InsnList code = new InsnList();
        for (int i = modern.length - 1; i >= 0; i--) code.add(new VarInsnNode(modern[i].getOpcode(Opcodes.ISTORE), slots[i]));
        for (InsnList argument : arguments) code.add(argument);
        method.maxLocals = next;
        method.maxStack += 4;
        method.instructions.insertBefore(call, code);
        method.instructions.insert(call, returned);
        call.name = renamed.legacyName;
        call.desc = ModernMappings.legacyDesc(renamed.legacyDesc);
        return true;
    }

    /** What 1.13's result becomes for the caller, from 1.12's: converted, or popped if 1.13 returns nothing. */
    @Nullable
    private InsnList returnConversion(Type result, Type wanted) {
        if (wanted.getSort() == Type.VOID) return result.getSort() == Type.VOID ? new InsnList() : pop(result);
        if (result.getSort() == Type.VOID) return null;
        return convert(result, wanted);
    }

    /**
     * A value for an argument with nothing behind it ({@link ModernSignatures#NONE} and the like), given the other
     * side's arguments in locals; null if it can't be made.
     */
    @Nullable
    private InsnList standIn(int kind, Type type, Type[] available, int[] slots) {
        InsnList code = new InsnList();
        if (kind == ModernSignatures.AIR) {
            code.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/minecraft/init/Blocks", "field_150350_a", "Lnet/minecraft/block/Block;"));
            code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/block/Block", "func_176223_P", "()Lnet/minecraft/block/state/IBlockState;", false));
            return code;
        }
        if (kind == ModernSignatures.FLUID_AT) {
            int world = -1;
            int pos = -1;
            for (int i = 0; i < available.length; i++) {
                if (available[i].getSort() != Type.OBJECT) continue;
                String name = available[i].getInternalName();
                if (world < 0 && this.hierarchy.isTranslatedSubtype(name, "net/minecraft/world/IBlockAccess")) world = i;
                if (pos < 0 && "net/minecraft/util/math/BlockPos".equals(name)) pos = i;
            }
            if (world < 0 || pos < 0) return null;
            code.add(new VarInsnNode(Opcodes.ALOAD, slots[world]));
            code.add(new VarInsnNode(Opcodes.ALOAD, slots[pos]));
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/michaelsebero/portium/modern/shim/FluidShims", "getFluidState",
                    "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)Lcom/michaelsebero/portium/modern/mc/fluid/IFluidState;", false));
            return code;
        }
        if (kind == ModernSignatures.STATE_AT && "net/minecraft/block/state/IBlockState".equals(type.getInternalName())) {
            int world = -1;
            int pos = -1;
            for (int i = 0; i < available.length; i++) {
                if (available[i].getSort() != Type.OBJECT) continue;
                String name = available[i].getInternalName();
                if (world < 0 && this.hierarchy.isTranslatedSubtype(name, "net/minecraft/world/IBlockAccess")) world = i;
                if (pos < 0 && "net/minecraft/util/math/BlockPos".equals(name)) pos = i;
            }
            if (world < 0 || pos < 0) return null;
            code.add(new VarInsnNode(Opcodes.ALOAD, slots[world]));
            code.add(new VarInsnNode(Opcodes.ALOAD, slots[pos]));
            code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "net/minecraft/world/IBlockAccess", "func_180495_p",
                    "(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", true));
            return code;
        }
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                code.add(new InsnNode(Opcodes.ICONST_0));
                break;
            case Type.LONG:
                code.add(new InsnNode(Opcodes.LCONST_0));
                break;
            case Type.FLOAT:
                code.add(new InsnNode(Opcodes.FCONST_0));
                break;
            case Type.DOUBLE:
                code.add(new InsnNode(Opcodes.DCONST_0));
                break;
            default:
                code.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        return code;
    }

    /**
     * LWJGL 3 names OpenGL's array getters and setters as OpenGL does ({@code glGetIntegerv}); LWJGL 2 left the type
     * suffix off ({@code glGetInteger}). Same parameters.
     */
    private boolean linkLwjgl(MethodInsnNode call, String owner, String desc) {
        for (String suffix : new String[] {"iv", "fv", "dv", "uiv", "v"}) {
            if (!call.name.endsWith(suffix)) continue;
            String name = call.name.substring(0, call.name.length() - suffix.length());
            if (this.members.method(owner, name, desc) != null) {
                call.name = name;
                return true;
            }
        }
        return false;
    }

    /** A method reference ({@code Owner::method}) to a method 1.12 lacks: to 1.13.2's, bridged; null if unchanged. */
    @Nullable
    Handle link(Handle handle) {
        int tag = handle.getTag();
        if (tag != Opcodes.H_INVOKEVIRTUAL && tag != Opcodes.H_INVOKESTATIC && tag != Opcodes.H_INVOKEINTERFACE && tag != Opcodes.H_INVOKESPECIAL) return null;
        String owner = this.names.map(handle.getOwner());
        if (!isParentType(owner)) return null;
        if (this.members.method(owner, this.mappings.mapMember(handle.getOwner(), handle.getName()), this.names.mapMethodDesc(handle.getDesc())) != null) return null;
        ModernBridges.Bridge bridge = bridge(owner, handle.getOwner(), handle.getName(), handle.getDesc(), tag == Opcodes.H_INVOKESTATIC);
        return bridge == null ? null : new Handle(Opcodes.H_INVOKESTATIC, bridge.owner, bridge.name, bridge.desc, false);
    }

    /**
     * A method reference javac made to one of Forge 1.16's extension interfaces ({@code ItemStack::serializeNBT} names
     * {@code IForgeItemStack}), whose objects here are 1.12's, which don't implement it: to the object's own class
     * instead (from the lambda's type), where Forge 1.12 has such methods. Null if it isn't one.
     */
    @Nullable
    Handle extensionHandle(Handle handle, @Nullable Type instantiated) {
        if (handle.getTag() != Opcodes.H_INVOKEINTERFACE || !handle.getOwner().startsWith("net/minecraftforge/common/extensions/IForge")) return null;
        if (instantiated == null || instantiated.getArgumentTypes().length == 0) return null;
        Type receiver = instantiated.getArgumentTypes()[0];
        if (receiver.getSort() != Type.OBJECT || receiver.getInternalName().equals(handle.getOwner())) return null;
        Class<?> type = this.members.type(this.names.map(receiver.getInternalName()));
        if (type == null) return null;
        boolean itf = type.isInterface();
        return new Handle(itf ? Opcodes.H_INVOKEINTERFACE : Opcodes.H_INVOKEVIRTUAL, receiver.getInternalName(), handle.getName(), handle.getDesc(), itf);
    }

    @Nullable
    private ModernBridges.Bridge bridge(String owner, String modernOwner, String name, String desc, boolean isStatic) {
        // Portium's stand-ins aren't 1.13's classes, so 1.13's bodies don't fit them; static ones touch no instance, and do.
        if (this.bridges == null || owner.startsWith(PORTIUM) && !isStatic) return null;
        return this.bridges.method(modernOwner, name, desc, isStatic, isStatic ? null : adapterHaving(modernOwner, name, desc));
    }

    /** Portium's adapter of a class (1.13 name), if it has the method itself (1.13 names); null otherwise. */
    @Nullable
    private String adapterHaving(String modernOwner, String name, String desc) {
        String adapter = ModernRedirects.adapterOf(modernOwner);
        if (adapter == null) return null;
        return this.members.method(adapter, this.mappings.mapMember(adapter, name), this.names.mapMethodDesc(desc)) != null ? adapter : null;
    }

    /** Calls 1.12's version of a method instead, converting what goes in and comes out; false if they don't convert. */
    private boolean adapt(MethodNode method, MethodInsnNode call, String desc, ModernMembers.Member candidate) {
        Type[] from = Type.getArgumentTypes(desc);
        Type[] to = Type.getArgumentTypes(candidate.desc);
        if (from.length < to.length || from.length != to.length && !call.name.startsWith("func_")) return false;
        InsnList[] conversions = new InsnList[to.length];
        for (int i = 0; i < to.length; i++) {
            conversions[i] = convert(from[i], to[i]);
            if (conversions[i] == null) return false;
        }
        Type result = Type.getReturnType(candidate.desc);
        Type wanted = Type.getReturnType(desc);
        InsnList returned;
        if (wanted.getSort() == Type.VOID) {
            returned = result.getSort() == Type.VOID ? new InsnList() : pop(result);
        } else {
            if (result.getSort() == Type.VOID) return false;
            returned = convert(result, wanted);
            if (returned == null) return false;
        }
        boolean inPlace = from.length == to.length;
        for (int i = 0; i < to.length - 1 && inPlace; i++) inPlace = conversions[i].size() == 0;
        InsnList code = new InsnList();
        if (inPlace) {
            if (to.length > 0) code.add(conversions[to.length - 1]);
        } else {
            // Park 1.13's arguments in fresh locals and load 1.12's from them; nothing branches in between.
            int[] slots = new int[from.length];
            int next = method.maxLocals;
            for (int i = 0; i < from.length; i++) {
                slots[i] = next;
                next += from[i].getSize();
            }
            method.maxLocals = next;
            for (int i = from.length - 1; i >= 0; i--) code.add(new VarInsnNode(from[i].getOpcode(Opcodes.ISTORE), slots[i]));
            for (int i = 0; i < to.length; i++) {
                code.add(new VarInsnNode(from[i].getOpcode(Opcodes.ILOAD), slots[i]));
                code.add(conversions[i]);
            }
        }
        method.instructions.insertBefore(call, code);
        method.instructions.insert(call, returned);
        call.desc = ModernMappings.legacyDesc(candidate.desc);
        method.maxStack += 2;
        return true;
    }

    // Fields

    /**
     * A read of a static field 1.12 lacks: of 1.13.2's value instead, or for {@code Blocks} and {@code Items}, 1.13's
     * flattened block or item ({@code flat.Flattening}); true if the instruction changed.
     */
    boolean link(MethodNode method, FieldInsnNode field) {
        if (field.getOpcode() != Opcodes.GETSTATIC || this.bridges == null) return false;
        String owner = this.names.map(field.owner);
        if (!isParentType(owner)) return false;
        if (this.members.field(owner, this.mappings.mapMember(field.owner, field.name), this.names.mapDesc(field.desc)) != null) return false;
        if (owner.startsWith(PORTIUM)) {
            // A stand-in's static field 1.16 added (a codec, say): 1.16's own value, as for 1.12's classes. One the
            // stand-in has with a wider type is read and cast instead (see retype).
            if (this.members.fieldNamed(owner, this.mappings.mapMember(field.owner, field.name)) != null) return false;
            String bridge = this.bridges.field(field.owner, field.name, field.desc);
            if (bridge == null) return false;
            field.owner = bridge;
            field.name = ModernBridges.FIELD;
            return true;
        }
        if (isFlatBlocks(field.owner) || isFlatItems(field.owner)) return flattened(method, field);
        if (MATERIAL_COLOR.equals(field.owner)) return mapColor(method, field);
        Class<?> type = this.members.type(owner);
        if (type != null && type.isEnum()) return enumConstant(method, field, owner);
        String bridge = this.bridges.field(field.owner, field.name, field.desc);
        if (bridge == null) return false;
        field.owner = bridge;
        field.name = ModernBridges.FIELD;
        return true;
    }

    private static final String FLAT_BLOCKS = "net/minecraft/init/Blocks";
    private static final String FLAT_ITEMS = "net/minecraft/init/Items";

    /** 1.13's {@code Blocks} or 1.16's (which moved it). */
    private static boolean isFlatBlocks(String owner) {
        return FLAT_BLOCKS.equals(owner) || "net/minecraft/block/Blocks".equals(owner);
    }

    private static boolean isFlatItems(String owner) {
        return FLAT_ITEMS.equals(owner) || "net/minecraft/item/Items".equals(owner);
    }
    private static final String FLATTENING = "com/michaelsebero/portium/modern/flat/Flattening";
    private final Map<String, Map<String, String>> registeredNames = new java.util.concurrent.ConcurrentHashMap<>();

    /** Every 1.13 name in {@code Blocks} or {@code Items}, from Minecraft 1.13.2's own. */
    List<String> registeredNames(String owner) {
        return this.bridges == null ? new ArrayList<>() : new ArrayList<>(names(owner).values());
    }

    private Map<String, String> names(String owner) {
        return this.registeredNames.computeIfAbsent(owner, key -> this.bridges.registeredNames(key));
    }

    private static final String MATERIAL_COLOR = "net/minecraft/block/material/MaterialColor";

    /** A constant of one of 1.12's enums under another name: 1.12's of that name, else of that ordinal. */
    private boolean enumConstant(MethodNode method, FieldInsnNode field, String owner) {
        Object[] constant = this.bridges.enumConstant(field.owner, field.name, field.desc);
        if (constant == null) return false;
        InsnList code = new InsnList();
        code.add(new org.objectweb.asm.tree.LdcInsnNode(Type.getObjectType(owner)));
        code.add(new org.objectweb.asm.tree.LdcInsnNode(constant[0]));
        code.add(new org.objectweb.asm.tree.LdcInsnNode(constant[1]));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS.substring(0, CONVERSIONS.length() - 1), "enumConstant",
                "(Ljava/lang/Class;Ljava/lang/String;I)Ljava/lang/Enum;", false));
        code.add(new TypeInsnNode(Opcodes.CHECKCAST, owner));
        method.instructions.insert(field, code);
        method.instructions.remove(field);
        method.maxStack += 3;
        return true;
    }

    /** A 1.13 map colour 1.12 has under another field: 1.12's colour of the same index (they're numbered alike). */
    private boolean mapColor(MethodNode method, FieldInsnNode field) {
        Integer index = this.bridges.constructorIndex(field.owner, field.name, field.desc);
        if (index == null) return false;
        InsnList code = new InsnList();
        code.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/minecraft/block/material/MapColor", "field_76281_a", "[Lnet/minecraft/block/material/MapColor;"));
        code.add(new org.objectweb.asm.tree.LdcInsnNode(index));
        code.add(new InsnNode(Opcodes.AALOAD));
        method.instructions.insert(field, code);
        method.instructions.remove(field);
        method.maxStack += 2;
        return true;
    }

    private boolean flattened(MethodNode method, FieldInsnNode field) {
        String name = names(field.owner).get(field.name);
        if (name == null) return false;
        boolean block = isFlatBlocks(field.owner);
        String base = block ? "net/minecraft/block/Block" : "net/minecraft/item/Item";
        InsnList code = new InsnList();
        code.add(new org.objectweb.asm.tree.LdcInsnNode(name));
        code.add(new org.objectweb.asm.tree.LdcInsnNode(Type.getType(field.desc)));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, FLATTENING, block ? "block" : "item", "(Ljava/lang/String;Ljava/lang/Class;)L" + base + ";", false));
        if (!field.desc.equals("L" + base + ";")) code.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getType(field.desc).getInternalName()));
        method.instructions.insert(field, code);
        method.instructions.remove(field);
        method.maxStack += 2;
        return true;
    }

    // Overrides

    /**
     * Bridges for a class's methods that override one of 1.12's under another descriptor (by SRG id), so that 1.12,
     * calling its own, reaches them. Written to the class visitor before renaming, with 1.13's names.
     */
    void bridgeOverrides(ClassVisitor out, String modernName, @Nullable String superName, @Nullable String[] interfaces, List<MethodNode> methods,
                         boolean isInterface) {
        if (isInterface) return;
        List<String> parents = parentTypes(superName, interfaces);
        if (parents.isEmpty()) return;
        Set<String> declared = new HashSet<>();
        for (MethodNode method : methods) declared.add(this.mappings.mapMember(this.names.map(modernName), method.name) + this.names.mapMethodDesc(method.desc));
        Set<String> made = new HashSet<>();
        // The fullest of a method's variants gets the bridge: Forge 1.16's model quads with model data before 1.16's.
        List<MethodNode> byArguments = new ArrayList<>(methods);
        byArguments.sort((a, b) -> Type.getArgumentTypes(b.desc).length - Type.getArgumentTypes(a.desc).length);
        for (MethodNode method : byArguments) {
            if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0 || method.name.startsWith("<")) continue;
            ModernSignatures.Renamed renamed = this.signatures.find(modernName, method.name, method.desc, this.hierarchy);
            if (renamed == null || renamed.callsOnly) continue;
            String key = this.mappings.mapMember(this.names.map(modernName), renamed.legacyName) + renamed.legacyDesc;
            if (declared.contains(key) || made.contains(key)) continue;
            ModernMembers.Member reached = parents.isEmpty() ? null : this.members.method(parents.get(0), this.mappings.mapMember(this.names.map(modernName), renamed.legacyName), renamed.legacyDesc);
            if (reached != null && isAdapter(reached.ownerName())) continue;
            if (bridgeRenamed(out, modernName, method, renamed)) made.add(key);
        }
        for (MethodNode method : methods) {
            if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0 || !method.name.startsWith("func_")) continue;
            String name = this.mappings.mapMember(this.names.map(modernName), method.name);
            String desc = this.names.mapMethodDesc(method.desc);
            for (String parent : parents) {
                for (ModernMembers.Member candidate : this.members.methodsNamed(parent, name)) {
                    if ((candidate.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL)) != 0 || candidate.desc.equals(desc)) continue;
                    String key = name + candidate.desc;
                    if (declared.contains(key) || made.contains(key) || !sameMethod(method.name, candidate)) continue;
                    // Portium's adapters already override 1.12's method to call the 1.13 one; but a newer method an
                    // adapter declares itself, with loose parameter types (a 1.16 matrix stack as an object), is the
                    // one 1.12's calls reach, so the mod's override still needs the bridge to it.
                    ModernMembers.Member reached = this.members.method(parent, name, candidate.desc);
                    boolean declared116 = isAdapter(candidate.ownerName()) && this.mappings.isAnnotated(candidate.ownerName(), candidate.name, candidate.desc);
                    if (!declared116 && reached != null && isAdapter(reached.ownerName())) continue;
                    if (bridgeOverride(out, modernName, method, candidate)) made.add(key);
                }
            }
        }
    }

    /** 1.12's method calling a class's 1.13 one that it's the same as, arguments rearranged. */
    private boolean bridgeRenamed(ClassVisitor out, String modernName, MethodNode target, ModernSignatures.Renamed renamed) {
        Type[] legacy = Type.getArgumentTypes(renamed.legacyDesc);
        Type[] modern = Type.getArgumentTypes(this.names.mapMethodDesc(target.desc));
        int[] slots = new int[legacy.length];
        int slot = 1;
        for (int i = 0; i < legacy.length; i++) {
            slots[i] = slot;
            slot += legacy[i].getSize();
        }
        MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, renamed.legacyName, ModernMappings.legacyDesc(renamed.legacyDesc), null, null);
        bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        for (int i = 0; i < modern.length; i++) {
            int from = renamed.modernFrom[i];
            String constant = renamed.constants.get(i);
            if (constant != null) {
                int dot = constant.indexOf('.');
                int colon = constant.indexOf(':');
                bridge.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, constant.substring(0, dot), constant.substring(dot + 1, colon), constant.substring(colon + 1)));
            } else if (from >= 0) {
                InsnList conversion = convert(legacy[from], modern[i]);
                if (conversion == null) return false;
                bridge.instructions.add(new VarInsnNode(legacy[from].getOpcode(Opcodes.ILOAD), slots[from]));
                bridge.instructions.add(conversion);
            } else {
                InsnList standIn = standIn(from, modern[i], legacy, slots);
                if (standIn == null) return false;
                bridge.instructions.add(standIn);
            }
        }
        Type wanted = Type.getReturnType(renamed.legacyDesc);
        InsnList returned = returnConversion(Type.getReturnType(this.names.mapMethodDesc(target.desc)), wanted);
        if (returned == null) return false;
        bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, modernName, target.name, target.desc, false));
        bridge.instructions.add(returned);
        bridge.instructions.add(new InsnNode(wanted.getOpcode(Opcodes.IRETURN)));
        bridge.maxLocals = slot;
        bridge.maxStack = 2 + 2 * modern.length + 4;
        bridge.accept(out);
        return true;
    }

    private boolean bridgeOverride(ClassVisitor out, String modernName, MethodNode target, ModernMembers.Member candidate) {
        Type[] from = Type.getArgumentTypes(candidate.desc);
        Type[] to = Type.getArgumentTypes(this.names.mapMethodDesc(target.desc));
        if (from.length < to.length || from.length != to.length && !target.name.startsWith("func_")) return false;
        InsnList[] conversions = new InsnList[to.length];
        for (int i = 0; i < to.length; i++) {
            conversions[i] = convert(from[i], to[i]);
            if (conversions[i] == null) return false;
        }
        Type result = Type.getReturnType(this.names.mapMethodDesc(target.desc));
        Type wanted = Type.getReturnType(candidate.desc);
        InsnList returned;
        if (wanted.getSort() == Type.VOID) {
            returned = result.getSort() == Type.VOID ? new InsnList() : pop(result);
        } else {
            if (result.getSort() == Type.VOID) return false;
            returned = convert(result, wanted);
            if (returned == null) return false;
        }
        MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, target.name, ModernMappings.legacyDesc(candidate.desc), null, null);
        bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        int stack = 1;
        for (int i = 0; i < from.length; i++) {
            if (i < to.length) {
                bridge.instructions.add(new VarInsnNode(from[i].getOpcode(Opcodes.ILOAD), slot));
                bridge.instructions.add(conversions[i]);
                stack += to[i].getSize();
            }
            slot += from[i].getSize();
        }
        bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, modernName, target.name, target.desc, false));
        bridge.instructions.add(returned);
        bridge.instructions.add(new InsnNode(wanted.getOpcode(Opcodes.IRETURN)));
        bridge.maxLocals = slot;
        bridge.maxStack = Math.max(stack, result.getSize()) + 1;
        bridge.accept(out);
        return true;
    }

    /** The parent's classes a class extends or implements nearest: its first parent superclass and parent interfaces. */
    private List<String> parentTypes(@Nullable String superName, @Nullable String[] interfaces) {
        Set<String> found = new LinkedHashSet<>();
        Set<String> seen = new HashSet<>();
        List<String> queue = new ArrayList<>();
        if (superName != null) queue.add(this.names.map(superName));
        if (interfaces != null) for (String itf : interfaces) queue.add(this.names.map(itf));
        while (!queue.isEmpty()) {
            String type = queue.remove(0);
            if (type == null || !seen.add(type) || type.startsWith("java/")) continue;
            if (isParentType(type)) {
                found.add(type);
            } else {
                for (String parent : this.hierarchy.supertypes(type)) if (parent != null) queue.add(parent);
            }
        }
        return new ArrayList<>(found);
    }

    // Access

    /**
     * Turns field accesses and calls the JVM would refuse this class, once renamed, into {@code invokedynamic}s that
     * {@link ModernAccess} links with access.
     */
    void fixAccess(ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                AbstractInsnNode replacement = null;
                if (insn instanceof FieldInsnNode) {
                    retype(method, (FieldInsnNode) insn);
                    replacement = access(node.name, (FieldInsnNode) insn);
                } else if (insn instanceof MethodInsnNode) {
                    replacement = access(node.name, (MethodInsnNode) insn);
                }
                if (replacement != null) {
                    method.instructions.set(insn, replacement);
                    insn = replacement;
                }
            }
        }
    }

    /**
     * A field 1.12 declares with a wider type than 1.13 reads it as (a stand-in's {@code Fluids.WATER} is a {@code
     * Fluid}, 1.13's a {@code FlowingFluid}): read as 1.12 declares it, then cast; written as it is.
     */
    private void retype(MethodNode method, FieldInsnNode field) {
        if (!isParentType(field.owner) || this.members.field(field.owner, field.name, field.desc) != null) return;
        Type wanted = Type.getType(field.desc);
        if (!isReference(wanted)) return;
        ModernMembers.Member found = this.members.fieldNamed(field.owner, field.name);
        if (found == null) return;
        Type actual = Type.getType(found.desc);
        if (actual.getSort() != Type.OBJECT) return;
        Class<?> actualType = this.members.type(actual.getInternalName());
        Class<?> wantedType = wanted.getSort() == Type.OBJECT ? this.members.type(wanted.getInternalName()) : null;
        // 1.13's type must be 1.12's or below it; one only this loader has can only be checked when cast.
        if (actualType == null || wantedType != null && !actualType.isAssignableFrom(wantedType)) return;
        int opcode = field.getOpcode();
        field.desc = found.desc;
        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
            method.instructions.insert(field, new TypeInsnNode(Opcodes.CHECKCAST, wanted.getSort() == Type.ARRAY ? wanted.getDescriptor() : wanted.getInternalName()));
        }
    }

    @Nullable
    private AbstractInsnNode access(String caller, FieldInsnNode field) {
        if (!isParentType(field.owner)) return null;
        ModernMembers.Member member = this.members.field(field.owner, field.name, field.desc);
        if (member == null || accessible(caller, field.owner, member)) return null;
        String owner = publicType(field.owner);
        switch (field.getOpcode()) {
            case Opcodes.GETFIELD:
                return indy(ModernAccess.GET_FIELD, field.owner, field.name, "(L" + owner + ";)" + field.desc);
            case Opcodes.PUTFIELD:
                return indy(ModernAccess.PUT_FIELD, field.owner, field.name, "(L" + owner + ";" + field.desc + ")V");
            case Opcodes.GETSTATIC:
                return indy(ModernAccess.GET_STATIC, field.owner, field.name, "()" + field.desc);
            default:
                return indy(ModernAccess.PUT_STATIC, field.owner, field.name, "(" + field.desc + ")V");
        }
    }

    @Nullable
    private AbstractInsnNode access(String caller, MethodInsnNode call) {
        if (call.getOpcode() == Opcodes.INVOKESPECIAL || "<init>".equals(call.name) || !isParentType(call.owner)) return null;
        ModernMembers.Member member = this.members.method(call.owner, call.name, call.desc);
        if (member == null || accessible(caller, call.owner, member)) return null;
        if (call.getOpcode() == Opcodes.INVOKESTATIC) return indy(ModernAccess.INVOKE_STATIC, call.owner, call.name, call.desc);
        return indy(ModernAccess.INVOKE_VIRTUAL, call.owner, call.name, "(L" + publicType(call.owner) + ";" + call.desc.substring(1));
    }

    /** Whether the JVM lets a class of this loader reach a member of the parent's through {@code owner}. */
    private boolean accessible(String caller, String owner, ModernMembers.Member member) {
        Class<?> type = this.members.type(owner);
        if (type == null || !Modifier.isPublic(type.getModifiers())) return false;
        if (Modifier.isPublic(member.access)) return true;
        if (!Modifier.isProtected(member.access)) return false;
        return this.hierarchy.isTranslatedSubtype(caller, member.ownerName())
                && (member.isStatic() || this.hierarchy.isTranslatedSubtype(owner, caller));
    }

    /** The class, or its nearest public superclass: what a call site may name of it. */
    private String publicType(String owner) {
        Class<?> type = this.members.type(owner);
        while (type != null && !Modifier.isPublic(type.getModifiers())) type = type.getSuperclass();
        return type == null ? "java/lang/Object" : Type.getInternalName(type);
    }

    private static InvokeDynamicInsnNode indy(int kind, String owner, String name, String desc) {
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, ACCESS, "bootstrap", ModernAccess.BOOTSTRAP_DESC, false);
        return new InvokeDynamicInsnNode("access", desc, bootstrap, owner.replace('/', '.'), name, kind);
    }
}
