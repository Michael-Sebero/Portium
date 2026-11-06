package com.michaelsebero.portium.world.gen.jigsaw;

import com.michaelsebero.portium.world.gen.jigsaw.StaticInitReader.Built;
import com.michaelsebero.portium.world.gen.jigsaw.StaticInitReader.FieldRef;
import com.michaelsebero.portium.world.gen.jigsaw.StaticInitReader.PairValue;
import org.objectweb.asm.tree.MethodInsnNode;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 1.16.5's jigsaw template pools and processor lists, read from the official jar's classes (which hold them as code)
 * with {@link StaticInitReader}. Class names are 1.16.5's obfuscated ones: villages (kj plains, kg desert, km savanna,
 * kn snowy, kq taiga), pillager outposts (ki), bastions (jy bridge, jz stables, ka housing, kb start, kc shared, kd
 * treasure), processor lists (kl), and the registries they name things from (bup blocks, aed block tags, cex state
 * properties, kh configured features).
 */
public final class JigsawPools {

    public enum Projection { RIGID, TERRAIN_MATCHING }

    /** A piece of a pool. */
    public abstract static class Element {
        public Projection projection = Projection.RIGID;
    }

    /** One template, with a processor list's name. Legacy pieces place their jigsaw blocks as structure voids would. */
    public static final class Single extends Element {
        public final String template;
        public final String processors;
        public final boolean legacy;

        Single(String template, String processors, boolean legacy) {
            this.template = template;
            this.processors = processors;
            this.legacy = legacy;
        }

        @Override
        public String toString() {
            return (this.legacy ? "legacy " : "single ") + this.template + (this.processors.equals("empty") ? "" : " [" + this.processors + "]");
        }
    }

    public static final class Empty extends Element {
        @Override
        public String toString() {
            return "empty";
        }
    }

    /** A configured feature (a tree, a hay pile...) placed at the piece's jigsaw. */
    public static final class Feature extends Element {
        public final String feature;

        Feature(String feature) {
            this.feature = feature;
        }

        @Override
        public String toString() {
            return "feature " + this.feature;
        }
    }

    /** Several pieces placed together at the same spot. */
    public static final class ListElement extends Element {
        public final List<Element> elements;

        ListElement(List<Element> elements) {
            this.elements = elements;
        }

        @Override
        public String toString() {
            return "list " + this.elements;
        }
    }

    public static final class Pool {
        public final String name;
        public final String fallback;
        /** Each element as many times as its weight. */
        public final List<Element> elements;
        public final Projection projection;

        Pool(String name, String fallback, List<Element> elements, Projection projection) {
            this.name = name;
            this.fallback = fallback;
            this.elements = elements;
            this.projection = projection;
        }
    }

    /** A processor of a processor list, as read: its kind and arguments (rule lists, integrity, heightmap...). */
    public static final class Processor {
        public final String kind;
        public final List<Object> args;

        Processor(String kind, List<Object> args) {
            this.kind = kind;
            this.args = args;
        }

        @Override
        public String toString() {
            return this.kind + this.args;
        }
    }

    /** Something registered under a name: a block, tag, property, feature or processor list. */
    public static final class Named {
        public final String name;
        @Nullable
        public final Object value;

        Named(String name, @Nullable Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /** A block state as a processor names it: a block and the properties set on it. */
    public static final class StateValue {
        public final String block;
        public final Map<String, String> properties = new LinkedHashMap<>();

        StateValue(String block) {
            this.block = block;
        }

        @Override
        public String toString() {
            return this.block + this.properties;
        }
    }

    static final String[] POOL_CLASSES = {"kj", "kg", "km", "kn", "kq", "ki", "jy", "jz", "ka", "kb", "kc", "kd"};
    static final String[] NAME_CLASSES = {"bup", "aed", "cex", "kh"};
    static final String PROCESSOR_CLASS = "kl";

    private final Map<String, Pool> pools = new HashMap<>();
    private final Map<String, List<Processor>> processorLists = new HashMap<>();
    /** Every name registry read: owner -> field -> name. */
    private final Map<String, Map<String, Named>> names = new HashMap<>();

    public Map<String, Pool> getPools() {
        return Collections.unmodifiableMap(this.pools);
    }

    @Nullable
    public Pool getPool(String name) {
        return this.pools.get(name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name);
    }

    public List<Processor> getProcessors(String list) {
        return this.processorLists.getOrDefault(list, Collections.emptyList());
    }

    /**
     * Reads the classes, opened by their obfuscated names (without .class). Missing classes leave their pools out.
     */
    public static JigsawPools read(Function<String, InputStream> classes) {
        JigsawPools pools = new JigsawPools();
        for (String owner : NAME_CLASSES) pools.readNames(owner, classes);
        byte[] processors = bytes(classes, PROCESSOR_CLASS);
        if (processors != null) pools.readProcessors(processors);
        for (String owner : POOL_CLASSES) {
            byte[] bytes = bytes(classes, owner);
            if (bytes != null) pools.readPools(bytes);
        }
        return pools;
    }

    @Nullable
    private static byte[] bytes(Function<String, InputStream> classes, String name) {
        try (InputStream stream = classes.apply(name)) {
            if (stream == null) return null;
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = stream.read(buffer)) > 0; ) out.write(buffer, 0, read);
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** A registry class: each static field set from a call naming it (Blocks.register("stone", ...)) gets that name. */
    private void readNames(String owner, Function<String, InputStream> classes) {
        byte[] bytes = bytes(classes, owner);
        Map<String, Named> fields = new HashMap<>();
        this.names.put(owner, fields);
        if (bytes == null) return;
        Map<String, Object> statics = StaticInitReader.read(bytes, (call, receiver, args) -> named(call, args));
        for (Map.Entry<String, Object> entry : statics.entrySet()) {
            if (entry.getValue() instanceof Named) fields.put(entry.getKey(), (Named) entry.getValue());
        }
    }

    /** A static call whose first argument is a string returns that name (with what it registered). */
    private static Object named(MethodInsnNode call, List<Object> args) {
        if (call.getOpcode() == org.objectweb.asm.Opcodes.INVOKESTATIC && !args.isEmpty() && args.get(0) instanceof String) {
            return new Named((String) args.get(0), args.size() > 1 ? args.get(1) : null);
        }
        return StaticInitReader.NO_ANSWER;
    }

    @Nullable
    private String nameOf(Object value) {
        if (value instanceof Named) return ((Named) value).name;
        if (value instanceof FieldRef) {
            Map<String, Named> fields = this.names.get(((FieldRef) value).owner);
            Named named = fields == null ? null : fields.get(((FieldRef) value).name);
            return named == null ? null : named.name;
        }
        return null;
    }

    private void readProcessors(byte[] bytes) {
        Map<String, Object> statics = StaticInitReader.read(bytes, (call, receiver, args) -> {
            // ProcessorLists.register("name", ImmutableList.of(processors...))
            if (call.owner.equals(PROCESSOR_CLASS) && args.size() == 2 && args.get(0) instanceof String && args.get(1) instanceof List) {
                List<Processor> processors = new ArrayList<>();
                for (Object processor : (List<?>) args.get(1)) processors.add(this.processor(processor));
                this.processorLists.put((String) args.get(0), processors);
                return new Named((String) args.get(0), processors);
            }
            return this.resolveCall(call, receiver, args);
        });
        Map<String, Named> fields = new HashMap<>();
        for (Map.Entry<String, Object> entry : statics.entrySet()) {
            if (entry.getValue() instanceof Named) fields.put(entry.getKey(), (Named) entry.getValue());
        }
        this.names.put(PROCESSOR_CLASS, fields);
    }

    /** Block states and instances named in processors. */
    private Object resolveCall(MethodInsnNode call, @Nullable Object receiver, List<Object> args) {
        // Block.defaultBlockState()
        if (call.owner.equals("buo") && call.name.equals("n") && args.isEmpty()) {
            String block = this.nameOf(receiver);
            return block == null ? StaticInitReader.NO_ANSWER : new StateValue(block);
        }
        // BlockState.setValue(property, value)
        if (receiver instanceof StateValue && args.size() == 2) {
            String property = this.nameOf(args.get(0));
            if (property != null) {
                Object value = args.get(1);
                String text = value instanceof FieldRef ? ((FieldRef) value).name : String.valueOf(value);
                ((StateValue) receiver).properties.put(property, text);
            }
            return receiver;
        }
        return StaticInitReader.NO_ANSWER;
    }

    private Processor processor(Object value) {
        if (value instanceof Built) {
            Built built = (Built) value;
            List<Object> args = new ArrayList<>();
            for (Object arg : built.args) args.add(this.argument(arg));
            return new Processor(built.type, args);
        }
        // Instances kept in static fields (JigsawReplacementProcessor.INSTANCE, BlockIgnoreProcessor.STRUCTURE_BLOCK).
        if (value instanceof FieldRef) return new Processor(((FieldRef) value).owner, Collections.singletonList(((FieldRef) value).name));
        return new Processor("unknown", Collections.singletonList(String.valueOf(value)));
    }

    /** A processor's argument: rules and tests as nested processors, blocks and tags by name, the rest as read. */
    private Object argument(Object arg) {
        if (arg instanceof Built) return this.processor(arg);
        if (arg instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object item : (List<?>) arg) list.add(this.argument(item));
            return list;
        }
        String name = this.nameOf(arg);
        if (name != null) return new Named(name, null);
        if (arg instanceof FieldRef) return new Processor(((FieldRef) arg).owner, Collections.singletonList(((FieldRef) arg).name));
        return arg;
    }

    private void readPools(byte[] bytes) {
        StaticInitReader.read(bytes, (call, receiver, args) -> {
            if (call.owner.equals("coi")) return this.factory(call, args);
            if (call.owner.equals("kk") && args.size() == 1 && args.get(0) instanceof Built) {
                Pool pool = this.pool((Built) args.get(0));
                if (pool != null) this.pools.put(pool.name, pool);
                return args.get(0);
            }
            return StaticInitReader.NO_ANSWER;
        });
    }

    /** {@code StructurePoolElement.legacy/single/empty/feature/list(...)}: a factory waiting for its projection. */
    private Object factory(MethodInsnNode call, List<Object> args) {
        String desc = call.desc;
        if (call.name.equals("g") && args.isEmpty()) return new Empty();
        if (!args.isEmpty() && args.get(0) instanceof String) {
            String processors = "empty";
            if (args.size() > 1) {
                String named = this.nameOf(args.get(1));
                if (named != null) processors = named;
            }
            return new Single((String) args.get(0), processors, call.name.equals("a"));
        }
        if (!args.isEmpty() && args.get(0) instanceof List) {
            List<Element> elements = new ArrayList<>();
            for (Object item : (List<?>) args.get(0)) {
                if (item instanceof Element) elements.add((Element) item);
            }
            return new ListElement(elements);
        }
        if (args.size() == 1 && desc.startsWith("(L")) {
            String feature = this.nameOf(args.get(0));
            return new Feature(feature == null ? "unknown" : feature);
        }
        return StaticInitReader.NO_ANSWER;
    }

    @Nullable
    private Pool pool(Built built) {
        if (!built.type.equals("cok") || built.args.size() < 3) return null;
        String name = location(built.args.get(0));
        String fallback = location(built.args.get(1));
        if (name == null) return null;
        Projection projection = Projection.RIGID;
        if (built.args.size() > 3 && built.args.get(3) instanceof FieldRef) {
            projection = ((FieldRef) built.args.get(3)).name.equals("a") ? Projection.TERRAIN_MATCHING : Projection.RIGID;
        }
        List<Element> elements = new ArrayList<>();
        if (built.args.get(2) instanceof List) {
            for (Object entry : (List<?>) built.args.get(2)) {
                if (!(entry instanceof PairValue)) continue;
                Object element = ((PairValue) entry).left;
                Object weight = ((PairValue) entry).right;
                if (!(element instanceof Element) || !(weight instanceof Integer)) continue;
                setProjection((Element) element, projection);
                for (int i = 0; i < (Integer) weight; i++) elements.add((Element) element);
            }
        }
        return new Pool(name, fallback == null ? "empty" : fallback, elements, projection);
    }

    private static void setProjection(Element element, Projection projection) {
        element.projection = projection;
        if (element instanceof ListElement) {
            for (Element child : ((ListElement) element).elements) setProjection(child, projection);
        }
    }

    @Nullable
    private static String location(Object value) {
        if (value instanceof Built && ((Built) value).type.equals("vk") && !((Built) value).args.isEmpty() && ((Built) value).args.get(0) instanceof String) {
            String name = (String) ((Built) value).args.get(0);
            return name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
        }
        return null;
    }

    /** Dev check: reads the pools from a 1.16.5 client jar and prints them. */
    public static void main(String[] args) throws IOException {
        java.util.zip.ZipFile jar = new java.util.zip.ZipFile(args[0]);
        JigsawPools pools = read(name -> {
            java.util.zip.ZipEntry entry = jar.getEntry(name + ".class");
            try {
                return entry == null ? null : jar.getInputStream(entry);
            } catch (IOException e) {
                return null;
            }
        });
        System.out.println(pools.pools.size() + " pools, " + pools.processorLists.size() + " processor lists");
        for (String name : new java.util.TreeSet<>(pools.pools.keySet())) {
            Pool pool = pools.pools.get(name);
            System.out.println(name + " (" + pool.projection + ", fallback " + pool.fallback + "): " + new java.util.LinkedHashSet<>(pool.elements).size()
                    + " kinds, " + pool.elements.size() + " weight: " + new java.util.LinkedHashSet<>(pool.elements));
        }
        for (Map.Entry<String, List<Processor>> list : new java.util.TreeMap<>(pools.processorLists).entrySet()) {
            System.out.println("processors " + list.getKey() + ": " + list.getValue());
        }
    }
}
