package com.michaelsebero.portium.command.nbt;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reads stringified NBT ({@code {Count:3b,Tags:["a"]}}) from a command, with 1.13's grammar and errors. */
public final class SnbtParser {

    private static final Pattern DOUBLE_NO_SUFFIX = Pattern.compile("[-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOUBLE = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?d", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOAT = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?f", Pattern.CASE_INSENSITIVE);
    private static final Pattern BYTE = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)l", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern INT = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)");

    private final StringReader reader;

    public SnbtParser(StringReader reader) {
        this.reader = reader;
    }

    /** Parses a whole string as a compound, allowing nothing after it. */
    public static NBTTagCompound parseCompound(String text) throws CommandSyntaxException {
        StringReader reader = new StringReader(text);
        NBTTagCompound compound = new SnbtParser(reader).readStruct();
        reader.skipWhitespace();
        if (reader.canRead()) throw CommandExceptions.NBT_TRAILING.createWithContext(reader);
        return compound;
    }

    public NBTTagCompound readStruct() throws CommandSyntaxException {
        this.reader.expect('{');
        NBTTagCompound compound = new NBTTagCompound();
        this.reader.skipWhitespace();
        while (this.reader.canRead() && this.reader.peek() != '}') {
            int start = this.reader.getCursor();
            String key = this.readKey();
            if (key.isEmpty()) {
                this.reader.setCursor(start);
                throw CommandExceptions.NBT_EXPECTED_KEY.createWithContext(this.reader);
            }
            this.reader.expect(':');
            compound.setTag(key, this.readValue());
            if (!this.hasElementSeparator()) break;
            if (!this.reader.canRead()) throw CommandExceptions.NBT_EXPECTED_KEY.createWithContext(this.reader);
        }
        this.reader.expect('}');
        return compound;
    }

    private String readKey() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) throw CommandExceptions.NBT_EXPECTED_KEY.createWithContext(this.reader);
        return this.reader.readString();
    }

    public NBTBase readValue() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) throw CommandExceptions.NBT_EXPECTED_VALUE.createWithContext(this.reader);
        char next = this.reader.peek();
        if (next == '{') return this.readStruct();
        return next == '[' ? this.readList() : this.readTypedValue();
    }

    private NBTBase readTypedValue() throws CommandSyntaxException {
        this.reader.skipWhitespace();
        int start = this.reader.getCursor();
        if (StringReader.isQuotedStringStart(this.reader.peek())) return new NBTTagString(this.reader.readQuotedString());
        String text = this.reader.readUnquotedString();
        if (text.isEmpty()) {
            this.reader.setCursor(start);
            throw CommandExceptions.NBT_EXPECTED_VALUE.createWithContext(this.reader);
        }
        return type(text);
    }

    /** The tag for an unquoted value: a number with its type suffix, a boolean (as a byte), or else a string. */
    static NBTBase type(String text) {
        try {
            if (FLOAT.matcher(text).matches()) return new NBTTagFloat(Float.parseFloat(text.substring(0, text.length() - 1)));
            if (BYTE.matcher(text).matches()) return new NBTTagByte(Byte.parseByte(text.substring(0, text.length() - 1)));
            if (LONG.matcher(text).matches()) return new NBTTagLong(Long.parseLong(text.substring(0, text.length() - 1)));
            if (SHORT.matcher(text).matches()) return new NBTTagShort(Short.parseShort(text.substring(0, text.length() - 1)));
            if (INT.matcher(text).matches()) return new NBTTagInt(Integer.parseInt(text));
            if (DOUBLE.matcher(text).matches()) return new NBTTagDouble(Double.parseDouble(text.substring(0, text.length() - 1)));
            if (DOUBLE_NO_SUFFIX.matcher(text).matches()) return new NBTTagDouble(Double.parseDouble(text));
            if ("true".equalsIgnoreCase(text)) return new NBTTagByte((byte) 1);
            if ("false".equalsIgnoreCase(text)) return new NBTTagByte((byte) 0);
        } catch (NumberFormatException ignored) {
            // Out of range for its type: 1.13 keeps such values as strings.
        }
        return new NBTTagString(text);
    }

    private NBTBase readList() throws CommandSyntaxException {
        if (this.reader.canRead(3) && !StringReader.isQuotedStringStart(this.reader.peek(1)) && this.reader.peek(2) == ';') return this.readArrayTag();
        return this.readListTag();
    }

    private NBTTagList readListTag() throws CommandSyntaxException {
        this.reader.expect('[');
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) throw CommandExceptions.NBT_EXPECTED_VALUE.createWithContext(this.reader);
        NBTTagList list = new NBTTagList();
        int type = -1;
        while (this.reader.peek() != ']') {
            int start = this.reader.getCursor();
            NBTBase element = this.readValue();
            if (type < 0) {
                type = element.getId();
            } else if (element.getId() != type) {
                this.reader.setCursor(start);
                throw CommandExceptions.NBT_MIXED_LIST.createWithContext(this.reader, NBTBase.getTypeName(element.getId()));
            }
            list.appendTag(element);
            if (!this.hasElementSeparator()) break;
            if (!this.reader.canRead()) throw CommandExceptions.NBT_EXPECTED_VALUE.createWithContext(this.reader);
        }
        this.reader.expect(']');
        return list;
    }

    private NBTBase readArrayTag() throws CommandSyntaxException {
        this.reader.expect('[');
        int start = this.reader.getCursor();
        char type = this.reader.read();
        this.reader.read();
        this.reader.skipWhitespace();
        if (!this.reader.canRead()) throw CommandExceptions.NBT_EXPECTED_VALUE.createWithContext(this.reader);
        switch (type) {
            case 'B': {
                List<Number> values = this.readArray(1);
                byte[] bytes = new byte[values.size()];
                for (int i = 0; i < bytes.length; i++) bytes[i] = values.get(i).byteValue();
                return new NBTTagByteArray(bytes);
            }
            case 'I': {
                List<Number> values = this.readArray(3);
                int[] ints = new int[values.size()];
                for (int i = 0; i < ints.length; i++) ints[i] = values.get(i).intValue();
                return new NBTTagIntArray(ints);
            }
            case 'L': {
                List<Number> values = this.readArray(4);
                long[] longs = new long[values.size()];
                for (int i = 0; i < longs.length; i++) longs[i] = values.get(i).longValue();
                return new NBTTagLongArray(longs);
            }
            default:
                this.reader.setCursor(start);
                throw CommandExceptions.NBT_INVALID_ARRAY.createWithContext(this.reader, String.valueOf(type));
        }
    }

    private List<Number> readArray(int elementType) throws CommandSyntaxException {
        List<Number> values = new ArrayList<>();
        while (this.reader.peek() != ']') {
            int start = this.reader.getCursor();
            NBTBase element = this.readValue();
            if (element.getId() != elementType) {
                this.reader.setCursor(start);
                throw CommandExceptions.NBT_MIXED_ARRAY.createWithContext(this.reader, NBTBase.getTypeName(element.getId()));
            }
            values.add(elementType == 4 ? (Number) ((NBTPrimitive) element).getLong() : (Number) ((NBTPrimitive) element).getInt());
            if (!this.hasElementSeparator()) break;
            if (!this.reader.canRead()) throw CommandExceptions.NBT_EXPECTED_VALUE.createWithContext(this.reader);
        }
        this.reader.expect(']');
        return values;
    }

    private boolean hasElementSeparator() {
        this.reader.skipWhitespace();
        if (this.reader.canRead() && this.reader.peek() == ',') {
            this.reader.skip();
            this.reader.skipWhitespace();
            return true;
        }
        return false;
    }
}
