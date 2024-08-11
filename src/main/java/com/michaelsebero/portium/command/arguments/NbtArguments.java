package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.nbt.NbtPath;
import com.michaelsebero.portium.command.nbt.SnbtParser;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** The NBT arguments: a compound ({@code {...}}), any tag value, and a path ({@code Inventory[0].id}). */
public final class NbtArguments {

    public static ArgumentType<NBTTagCompound> compound() {
        return reader -> new SnbtParser(reader).readStruct();
    }

    public static ArgumentType<NBTBase> tag() {
        return reader -> new SnbtParser(reader).readValue();
    }

    public static ArgumentType<NbtPath> path() {
        return NbtPath::parse;
    }

    public static NBTTagCompound getCompound(CommandContext<?> context, String name) {
        return context.getArgument(name, NBTTagCompound.class);
    }

    public static NBTBase getTag(CommandContext<?> context, String name) {
        return context.getArgument(name, NBTBase.class);
    }

    public static NbtPath getPath(CommandContext<?> context, String name) {
        return context.getArgument(name, NbtPath.class);
    }

    private NbtArguments() {}
}
