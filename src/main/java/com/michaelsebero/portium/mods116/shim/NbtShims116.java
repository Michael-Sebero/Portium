package com.michaelsebero.portium.mods116.shim;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 1.16's NBT tag factories ({@code StringNBT.valueOf} and so on), as 1.12 makes its tags with constructors; and 1.16's
 * list tag, which is a Java list, as 1.12's, which is only iterable.
 */
public final class NbtShims116 {

    public static NBTTagString string(String value) {
        return new NBTTagString(value);
    }

    public static NBTTagByte ofByte(byte value) {
        return new NBTTagByte(value);
    }

    public static NBTTagByte ofBoolean(boolean value) {
        return new NBTTagByte((byte) (value ? 1 : 0));
    }

    public static NBTTagShort ofShort(short value) {
        return new NBTTagShort(value);
    }

    public static NBTTagInt ofInt(int value) {
        return new NBTTagInt(value);
    }

    public static NBTTagLong ofLong(long value) {
        return new NBTTagLong(value);
    }

    public static NBTTagFloat ofFloat(float value) {
        return new NBTTagFloat(value);
    }

    public static NBTTagDouble ofDouble(double value) {
        return new NBTTagDouble(value);
    }

    private NbtShims116() {}

    // List tags: 1.16's are lists of one tag type and refuse another, as 1.12's do.

    public static boolean add(NBTTagList list, Object tag) {
        if (!fits(list, tag)) return false;
        list.appendTag((NBTBase) tag);
        return true;
    }

    public static void add(NBTTagList list, int index, Object tag) {
        if (index < 0 || index > list.tagCount()) throw new IndexOutOfBoundsException("Index " + index + " of a list of " + list.tagCount());
        if (!fits(list, tag)) throw new UnsupportedOperationException("Trying to add " + tag + " to a list of type " + list.getTagType());
        List<NBTBase> after = new ArrayList<>();
        while (list.tagCount() > index) after.add(0, list.removeTag(list.tagCount() - 1));
        list.appendTag((NBTBase) tag);
        for (NBTBase moved : after) list.appendTag(moved);
    }

    public static NBTBase get(NBTTagList list, int index) {
        if (index < 0 || index >= list.tagCount()) throw new IndexOutOfBoundsException("Index " + index + " of a list of " + list.tagCount());
        return list.get(index);
    }

    public static NBTBase set(NBTTagList list, int index, NBTBase tag) {
        NBTBase old = get(list, index);
        if (!fits(list, tag) && list.tagCount() > 1) throw new UnsupportedOperationException("Trying to add tag of type " + tag.getId() + " to list of " + list.getTagType());
        list.set(index, tag);
        return old;
    }

    public static NBTBase remove(NBTTagList list, int index) {
        NBTBase old = get(list, index);
        list.removeTag(index);
        return old;
    }

    public static int size(NBTTagList list) {
        return list.tagCount();
    }

    public static boolean isEmpty(NBTTagList list) {
        return list.tagCount() == 0;
    }

    public static void clear(NBTTagList list) {
        while (list.tagCount() > 0) list.removeTag(list.tagCount() - 1);
    }

    public static Stream<NBTBase> stream(NBTTagList list) {
        return StreamSupport.stream(list.spliterator(), false);
    }

    public static boolean contains(NBTTagList list, Object tag) {
        for (NBTBase element : list) if (element.equals(tag)) return true;
        return false;
    }

    public static int indexOf(NBTTagList list, Object tag) {
        for (int i = 0; i < list.tagCount(); i++) if (list.get(i).equals(tag)) return i;
        return -1;
    }

    /** Whether a tag may join the list: one of its type, or any but an end tag while the list is empty. */
    private static boolean fits(NBTTagList list, Object tag) {
        if (!(tag instanceof NBTBase) || ((NBTBase) tag).getId() == 0) return false;
        return list.tagCount() == 0 || list.getTagType() == 0 || list.getTagType() == ((NBTBase) tag).getId();
    }
}
