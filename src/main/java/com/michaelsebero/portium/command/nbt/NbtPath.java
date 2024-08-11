package com.michaelsebero.portium.command.nbt;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * A path into NBT such as {@code Inventory[0].tag.display}, with the filters later versions added
 * ({@code Items[{Slot:0b}]}, {@code Passengers[]}, {@code {OnGround:1b}}) so 1.13-1.16 datapacks work.
 */
public final class NbtPath {

    private final String text;
    private final List<Node> nodes;

    private NbtPath(String text, List<Node> nodes) {
        this.text = text;
        this.nodes = nodes;
    }

    @Override
    public String toString() {
        return this.text;
    }

    public static NbtPath parse(StringReader reader) throws CommandSyntaxException {
        List<Node> nodes = new ArrayList<>();
        int start = reader.getCursor();
        boolean first = true;
        while (reader.canRead() && reader.peek() != ' ') {
            nodes.add(parseNode(reader, first));
            first = false;
            if (reader.canRead()) {
                char c = reader.peek();
                if (c != ' ' && c != '[' && c != '{') reader.expect('.');
            }
        }
        return new NbtPath(reader.getString().substring(start, reader.getCursor()), nodes);
    }

    private static Node parseNode(StringReader reader, boolean first) throws CommandSyntaxException {
        switch (reader.peek()) {
            case '"':
                return compoundChild(reader, reader.readString());
            case '[': {
                reader.skip();
                char next = reader.peek();
                if (next == '{') {
                    NBTTagCompound filter = new SnbtParser(reader).readStruct();
                    reader.expect(']');
                    return new ListFilter(filter);
                }
                if (next == ']') {
                    reader.skip();
                    return AllElements.INSTANCE;
                }
                int index = reader.readInt();
                reader.expect(']');
                return new ListIndex(index);
            }
            case '{':
                if (!first) throw CommandExceptions.NBT_PATH_NODE_INVALID.createWithContext(reader);
                return new RootFilter(new SnbtParser(reader).readStruct());
            default:
                return compoundChild(reader, readTagName(reader));
        }
    }

    private static Node compoundChild(StringReader reader, String name) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '{') return new FilteredChild(name, new SnbtParser(reader).readStruct());
        return new Child(name);
    }

    private static String readTagName(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && isNameChar(reader.peek())) reader.skip();
        if (reader.getCursor() == start) throw CommandExceptions.NBT_PATH_NODE_INVALID.createWithContext(reader);
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean isNameChar(char c) {
        return c != ' ' && c != '"' && c != '[' && c != ']' && c != '.' && c != '{' && c != '}';
    }

    /** Every tag the path points to. */
    public List<NBTBase> get(NBTBase root) throws CommandSyntaxException {
        List<NBTBase> current = Collections.singletonList(root);
        for (Node node : this.nodes) {
            List<NBTBase> next = new ArrayList<>();
            for (NBTBase element : current) node.get(element, next);
            if (next.isEmpty()) throw node.missing(this.text);
            current = next;
        }
        return current;
    }

    public int count(NBTBase root) {
        try {
            return this.get(root).size();
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

    /** Sets every place the path points to, creating missing compounds and lists along the way. */
    public int set(NBTBase root, Supplier<NBTBase> value) throws CommandSyntaxException {
        List<NBTBase> parents = this.getOrCreateParents(root);
        Node last = this.nodes.get(this.nodes.size() - 1);
        int changed = 0;
        for (NBTBase parent : parents) changed += last.set(parent, value);
        return changed;
    }

    public int remove(NBTBase root) {
        List<NBTBase> parents = Collections.singletonList(root);
        for (int i = 0; i < this.nodes.size() - 1; i++) {
            List<NBTBase> next = new ArrayList<>();
            for (NBTBase element : parents) this.nodes.get(i).get(element, next);
            parents = next;
        }
        int removed = 0;
        Node last = this.nodes.get(this.nodes.size() - 1);
        for (NBTBase parent : parents) removed += last.remove(parent);
        return removed;
    }

    private List<NBTBase> getOrCreateParents(NBTBase root) throws CommandSyntaxException {
        List<NBTBase> current = Collections.singletonList(root);
        for (int i = 0; i < this.nodes.size() - 1; i++) {
            Node node = this.nodes.get(i);
            Node child = this.nodes.get(i + 1);
            List<NBTBase> next = new ArrayList<>();
            for (NBTBase element : current) node.getOrCreate(element, child::createEmpty, next);
            if (next.isEmpty()) throw node.missing(this.text);
            current = next;
        }
        return current;
    }

    // ---- Nodes ----

    private interface Node {

        void get(NBTBase element, List<NBTBase> out);

        /** 1.13's error when nothing matches: by child name or element index (later syntax names the whole path). */
        default CommandSyntaxException missing(String path) {
            return CommandExceptions.NBT_PATH_CHILD_INVALID.create(path);
        }

        void getOrCreate(NBTBase element, Supplier<NBTBase> creator, List<NBTBase> out);

        /** The kind of container this node expects to step into. */
        NBTBase createEmpty();

        int set(NBTBase element, Supplier<NBTBase> value);

        int remove(NBTBase element);
    }

    private static final class Child implements Node {

        private final String name;

        @Override
        public CommandSyntaxException missing(String path) {
            return CommandExceptions.NBT_PATH_CHILD_INVALID.create(this.name);
        }

        Child(String name) {
            this.name = name;
        }

        @Override
        public void get(NBTBase element, List<NBTBase> out) {
            if (element instanceof NBTTagCompound && ((NBTTagCompound) element).hasKey(this.name)) out.add(((NBTTagCompound) element).getTag(this.name));
        }

        @Override
        public void getOrCreate(NBTBase element, Supplier<NBTBase> creator, List<NBTBase> out) {
            if (!(element instanceof NBTTagCompound)) return;
            NBTTagCompound compound = (NBTTagCompound) element;
            if (!compound.hasKey(this.name)) compound.setTag(this.name, creator.get());
            out.add(compound.getTag(this.name));
        }

        @Override
        public NBTBase createEmpty() {
            return new NBTTagCompound();
        }

        @Override
        public int set(NBTBase element, Supplier<NBTBase> value) {
            if (!(element instanceof NBTTagCompound)) return 0;
            NBTTagCompound compound = (NBTTagCompound) element;
            NBTBase newValue = value.get();
            NBTBase old = compound.getTag(this.name);
            compound.setTag(this.name, newValue);
            return newValue.equals(old) ? 0 : 1;
        }

        @Override
        public int remove(NBTBase element) {
            if (!(element instanceof NBTTagCompound) || !((NBTTagCompound) element).hasKey(this.name)) return 0;
            ((NBTTagCompound) element).removeTag(this.name);
            return 1;
        }
    }

    private static final class ListIndex implements Node {

        private final int index;

        @Override
        public CommandSyntaxException missing(String path) {
            return CommandExceptions.NBT_PATH_ELEMENT_INVALID.create(this.index);
        }

        ListIndex(int index) {
            this.index = index;
        }

        private int resolve(NBTTagList list) {
            return this.index < 0 ? list.tagCount() + this.index : this.index;
        }

        @Override
        public void get(NBTBase element, List<NBTBase> out) {
            if (!(element instanceof NBTTagList)) return;
            NBTTagList list = (NBTTagList) element;
            int i = this.resolve(list);
            if (i >= 0 && i < list.tagCount()) out.add(list.get(i));
        }

        @Override
        public void getOrCreate(NBTBase element, Supplier<NBTBase> creator, List<NBTBase> out) {
            this.get(element, out);
        }

        @Override
        public NBTBase createEmpty() {
            return new NBTTagList();
        }

        @Override
        public int set(NBTBase element, Supplier<NBTBase> value) {
            if (!(element instanceof NBTTagList)) return 0;
            NBTTagList list = (NBTTagList) element;
            int i = this.resolve(list);
            if (i < 0 || i >= list.tagCount()) return 0;
            NBTBase newValue = value.get();
            if (list.tagCount() > 0 && newValue.getId() != list.getTagType()) return 0;
            NBTBase old = list.get(i);
            list.set(i, newValue);
            return newValue.equals(old) ? 0 : 1;
        }

        @Override
        public int remove(NBTBase element) {
            if (!(element instanceof NBTTagList)) return 0;
            NBTTagList list = (NBTTagList) element;
            int i = this.resolve(list);
            if (i < 0 || i >= list.tagCount()) return 0;
            list.removeTag(i);
            return 1;
        }
    }

    private static final class AllElements implements Node {

        static final AllElements INSTANCE = new AllElements();

        @Override
        public void get(NBTBase element, List<NBTBase> out) {
            if (!(element instanceof NBTTagList)) return;
            NBTTagList list = (NBTTagList) element;
            for (int i = 0; i < list.tagCount(); i++) out.add(list.get(i));
        }

        @Override
        public void getOrCreate(NBTBase element, Supplier<NBTBase> creator, List<NBTBase> out) {
            if (!(element instanceof NBTTagList)) return;
            NBTTagList list = (NBTTagList) element;
            if (list.isEmpty()) list.appendTag(creator.get());
            this.get(element, out);
        }

        @Override
        public NBTBase createEmpty() {
            return new NBTTagList();
        }

        @Override
        public int set(NBTBase element, Supplier<NBTBase> value) {
            if (!(element instanceof NBTTagList)) return 0;
            NBTTagList list = (NBTTagList) element;
            int changed = 0;
            for (int i = 0; i < list.tagCount(); i++) {
                NBTBase newValue = value.get();
                if (newValue.getId() != list.getTagType()) return changed;
                if (!newValue.equals(list.get(i))) changed++;
                list.set(i, newValue);
            }
            return changed;
        }

        @Override
        public int remove(NBTBase element) {
            if (!(element instanceof NBTTagList)) return 0;
            NBTTagList list = (NBTTagList) element;
            int count = list.tagCount();
            while (!list.isEmpty()) list.removeTag(0);
            return count;
        }
    }

    private static final class ListFilter implements Node {

        private final NBTTagCompound filter;

        ListFilter(NBTTagCompound filter) {
            this.filter = filter;
        }

        private boolean matches(NBTBase candidate) {
            return NBTUtil.areNBTEquals(this.filter, candidate, true);
        }

        @Override
        public void get(NBTBase element, List<NBTBase> out) {
            if (!(element instanceof NBTTagList)) return;
            NBTTagList list = (NBTTagList) element;
            for (int i = 0; i < list.tagCount(); i++) {
                if (this.matches(list.get(i))) out.add(list.get(i));
            }
        }

        @Override
        public void getOrCreate(NBTBase element, Supplier<NBTBase> creator, List<NBTBase> out) {
            if (!(element instanceof NBTTagList)) return;
            int before = out.size();
            this.get(element, out);
            if (out.size() == before) {
                NBTTagCompound created = this.filter.copy();
                ((NBTTagList) element).appendTag(created);
                out.add(created);
            }
        }

        @Override
        public NBTBase createEmpty() {
            return new NBTTagList();
        }

        @Override
        public int set(NBTBase element, Supplier<NBTBase> value) {
            if (!(element instanceof NBTTagList)) return 0;
            NBTTagList list = (NBTTagList) element;
            int changed = 0;
            for (int i = 0; i < list.tagCount(); i++) {
                if (!this.matches(list.get(i))) continue;
                NBTBase newValue = value.get();
                if (!newValue.equals(list.get(i))) changed++;
                list.set(i, newValue);
            }
            return changed;
        }

        @Override
        public int remove(NBTBase element) {
            if (!(element instanceof NBTTagList)) return 0;
            NBTTagList list = (NBTTagList) element;
            int removed = 0;
            for (int i = list.tagCount() - 1; i >= 0; i--) {
                if (this.matches(list.get(i))) {
                    list.removeTag(i);
                    removed++;
                }
            }
            return removed;
        }
    }

    private static final class FilteredChild implements Node {

        private final String name;
        private final NBTTagCompound filter;

        FilteredChild(String name, NBTTagCompound filter) {
            this.name = name;
            this.filter = filter;
        }

        @Override
        public void get(NBTBase element, List<NBTBase> out) {
            if (!(element instanceof NBTTagCompound)) return;
            NBTBase child = ((NBTTagCompound) element).getTag(this.name);
            if (child != null && NBTUtil.areNBTEquals(this.filter, child, true)) out.add(child);
        }

        @Override
        public void getOrCreate(NBTBase element, Supplier<NBTBase> creator, List<NBTBase> out) {
            if (!(element instanceof NBTTagCompound)) return;
            NBTTagCompound compound = (NBTTagCompound) element;
            NBTBase child = compound.getTag(this.name);
            if (child == null) {
                child = this.filter.copy();
                compound.setTag(this.name, child);
                out.add(child);
            } else if (NBTUtil.areNBTEquals(this.filter, child, true)) {
                out.add(child);
            }
        }

        @Override
        public NBTBase createEmpty() {
            return new NBTTagCompound();
        }

        @Override
        public int set(NBTBase element, Supplier<NBTBase> value) {
            if (!(element instanceof NBTTagCompound)) return 0;
            NBTTagCompound compound = (NBTTagCompound) element;
            NBTBase child = compound.getTag(this.name);
            if (child != null && !NBTUtil.areNBTEquals(this.filter, child, true)) return 0;
            NBTBase newValue = value.get();
            compound.setTag(this.name, newValue);
            return newValue.equals(child) ? 0 : 1;
        }

        @Override
        public int remove(NBTBase element) {
            if (!(element instanceof NBTTagCompound)) return 0;
            NBTTagCompound compound = (NBTTagCompound) element;
            NBTBase child = compound.getTag(this.name);
            if (child == null || !NBTUtil.areNBTEquals(this.filter, child, true)) return 0;
            compound.removeTag(this.name);
            return 1;
        }
    }

    private static final class RootFilter implements Node {

        private final NBTTagCompound filter;

        RootFilter(NBTTagCompound filter) {
            this.filter = filter;
        }

        @Override
        public void get(NBTBase element, List<NBTBase> out) {
            if (element instanceof NBTTagCompound && NBTUtil.areNBTEquals(this.filter, element, true)) out.add(element);
        }

        @Override
        public void getOrCreate(NBTBase element, Supplier<NBTBase> creator, List<NBTBase> out) {
            this.get(element, out);
        }

        @Override
        public NBTBase createEmpty() {
            return new NBTTagCompound();
        }

        @Override
        public int set(NBTBase element, Supplier<NBTBase> value) {
            return 0;
        }

        @Override
        public int remove(NBTBase element) {
            return 0;
        }
    }
}
