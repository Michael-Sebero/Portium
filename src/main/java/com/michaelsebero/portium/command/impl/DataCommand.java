package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.BlockPosArgument;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.NbtArguments;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.DoubleArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.ArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.nbt.NbtPath;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code data get|merge|remove block|entity}: reads and changes NBT (1.13). */
public final class DataCommand {

    private static final CommandExceptionType.Simple MERGE_UNCHANGED = simple("commands.data.merge.failed");
    private static final CommandExceptionType.Dynamic GET_INVALID = dynamic("commands.data.get.invalid");
    private static final CommandExceptionType.Dynamic GET_UNKNOWN = dynamic("commands.data.get.unknown");
    private static final CommandExceptionType.Simple GET_MULTIPLE = simple("commands.data.get.multiple");
    private static final CommandExceptionType.Simple REMOVE_UNCHANGED = simple("commands.data.remove.failed");
    private static final CommandExceptionType.Simple ENTITY_INVALID = simple("commands.data.entity.invalid");
    private static final CommandExceptionType.Simple BLOCK_INVALID = simple("commands.data.block.invalid");

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    private static CommandExceptionType.Dynamic dynamic(String key) {
        return new CommandExceptionType.Dynamic(arg -> TextMessage.translate(key, arg));
    }

    /** Something whose NBT the command reads and writes. */
    public interface Target {

        NBTTagCompound getData() throws CommandSyntaxException;

        void setData(NBTTagCompound data) throws CommandSyntaxException;

        ITextComponent modifiedMessage();

        ITextComponent queryMessage(NBTBase tag);

        ITextComponent getMessage(NbtPath path, double scale, int value);
    }

    /** How a command names a target: {@code block <pos>} or {@code entity <target>}. */
    public interface TargetType {

        Target get(CommandContext<CommandSource> context) throws CommandSyntaxException;

        /** Adds the arguments naming the target, then whatever {@code rest} adds after them. */
        ArgumentBuilder<CommandSource, ?> addArguments(ArgumentBuilder<CommandSource, ?> builder,
                                                       Function<ArgumentBuilder<CommandSource, ?>, ArgumentBuilder<CommandSource, ?>> rest);
    }

    public static final TargetType BLOCK = new TargetType() {
        @Override
        public Target get(CommandContext<CommandSource> context) throws CommandSyntaxException {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "targetPos");
            TileEntity tile = context.getSource().getWorld().getTileEntity(pos);
            if (tile == null) throw BLOCK_INVALID.create();
            return new Target() {
                @Override
                public NBTTagCompound getData() {
                    return tile.writeToNBT(new NBTTagCompound());
                }

                @Override
                public void setData(NBTTagCompound data) {
                    data.setInteger("x", pos.getX());
                    data.setInteger("y", pos.getY());
                    data.setInteger("z", pos.getZ());
                    tile.readFromNBT(data);
                    tile.markDirty();
                    IBlockState state = tile.getWorld().getBlockState(pos);
                    tile.getWorld().notifyBlockUpdate(pos, state, state, 3);
                }

                @Override
                public ITextComponent modifiedMessage() {
                    return new TextComponentTranslation("commands.data.block.modified", pos.getX(), pos.getY(), pos.getZ());
                }

                @Override
                public ITextComponent queryMessage(NBTBase tag) {
                    return new TextComponentTranslation("commands.data.block.query", pos.getX(), pos.getY(), pos.getZ(), tag.toString());
                }

                @Override
                public ITextComponent getMessage(NbtPath path, double scale, int value) {
                    return new TextComponentTranslation("commands.data.block.get", path.toString(), pos.getX(), pos.getY(), pos.getZ(), String.format("%.2f", scale), value);
                }
            };
        }

        @Override
        public ArgumentBuilder<CommandSource, ?> addArguments(ArgumentBuilder<CommandSource, ?> builder,
                                                              Function<ArgumentBuilder<CommandSource, ?>, ArgumentBuilder<CommandSource, ?>> rest) {
            return builder.then(literal("block").then(rest.apply(argument("targetPos", BlockPosArgument.blockPos()))));
        }
    };

    public static final TargetType ENTITY = new TargetType() {
        @Override
        public Target get(CommandContext<CommandSource> context) throws CommandSyntaxException {
            Entity entity = EntityArgument.getEntity(context, "target");
            return new Target() {
                @Override
                public NBTTagCompound getData() {
                    return CommandBase.entityToNBT(entity);
                }

                @Override
                public void setData(NBTTagCompound data) throws CommandSyntaxException {
                    if (entity instanceof EntityPlayer) throw ENTITY_INVALID.create();
                    UUID uuid = entity.getUniqueID();
                    entity.readFromNBT(data);
                    entity.setUniqueId(uuid);
                }

                @Override
                public ITextComponent modifiedMessage() {
                    return new TextComponentTranslation("commands.data.entity.modified", entity.getDisplayName());
                }

                @Override
                public ITextComponent queryMessage(NBTBase tag) {
                    return new TextComponentTranslation("commands.data.entity.query", entity.getDisplayName(), tag.toString());
                }

                @Override
                public ITextComponent getMessage(NbtPath path, double scale, int value) {
                    return new TextComponentTranslation("commands.data.entity.get", path.toString(), entity.getDisplayName(), String.format("%.2f", scale), value);
                }
            };
        }

        @Override
        public ArgumentBuilder<CommandSource, ?> addArguments(ArgumentBuilder<CommandSource, ?> builder,
                                                              Function<ArgumentBuilder<CommandSource, ?>, ArgumentBuilder<CommandSource, ?>> rest) {
            return builder.then(literal("entity").then(rest.apply(argument("target", EntityArgument.entity()))));
        }
    };

    public static final List<TargetType> TARGETS = Arrays.asList(BLOCK, ENTITY);

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> data = literal("data").requires(source -> source.hasPermissionLevel(2));
        for (TargetType type : TARGETS) {
            data.then(type.addArguments(literal("merge"), builder -> builder.then(argument("nbt", NbtArguments.compound())
                    .executes(context -> merge(context.getSource(), type.get(context), NbtArguments.getCompound(context, "nbt"))))));
            data.then(type.addArguments(literal("get"), builder -> builder.executes(context -> query(context.getSource(), type.get(context)))
                    .then(argument("path", NbtArguments.path()).executes(context -> get(context.getSource(), type.get(context), NbtArguments.getPath(context, "path")))
                            .then(argument("scale", DoubleArgumentType.doubleArg()).executes(context -> getScaled(context.getSource(), type.get(context),
                                    NbtArguments.getPath(context, "path"), DoubleArgumentType.getDouble(context, "scale")))))));
            data.then(type.addArguments(literal("remove"), builder -> builder.then(argument("path", NbtArguments.path())
                    .executes(context -> remove(context.getSource(), type.get(context), NbtArguments.getPath(context, "path"))))));
        }
        dispatcher.register(data);
    }

    private static int merge(CommandSource source, Target target, NBTTagCompound changes) throws CommandSyntaxException {
        NBTTagCompound data = target.getData();
        NBTTagCompound before = data.copy();
        data.merge(changes);
        if (before.equals(data)) throw MERGE_UNCHANGED.create();
        target.setData(data);
        source.sendFeedback(target.modifiedMessage(), true);
        return 1;
    }

    private static int remove(CommandSource source, Target target, NbtPath path) throws CommandSyntaxException {
        NBTTagCompound data = target.getData();
        int removed = path.remove(data);
        if (removed == 0) throw REMOVE_UNCHANGED.create();
        target.setData(data);
        source.sendFeedback(target.modifiedMessage(), true);
        return removed;
    }

    private static int query(CommandSource source, Target target) throws CommandSyntaxException {
        source.sendFeedback(target.queryMessage(target.getData()), false);
        return 1;
    }

    private static NBTBase single(Target target, NbtPath path) throws CommandSyntaxException {
        List<NBTBase> found = path.get(target.getData());
        if (found.size() != 1) throw GET_MULTIPLE.create();
        return found.get(0);
    }

    /** The tag's "size": a number's value, a list's or compound's length, a string's length. */
    private static int get(CommandSource source, Target target, NbtPath path) throws CommandSyntaxException {
        NBTBase tag = single(target, path);
        int value;
        if (tag instanceof NBTPrimitive) value = MathHelper.floor(((NBTPrimitive) tag).getDouble());
        else if (tag instanceof NBTTagList) value = ((NBTTagList) tag).tagCount();
        else if (tag instanceof NBTTagByteArray) value = ((NBTTagByteArray) tag).getByteArray().length;
        else if (tag instanceof NBTTagIntArray) value = ((NBTTagIntArray) tag).getIntArray().length;
        else if (tag instanceof NBTTagLongArray) {
            // 1.12 keeps a long array's contents private; count the entries in its text form, [L;1L,2L].
            String body = tag.toString();
            body = body.substring(body.indexOf(';') + 1, body.length() - 1);
            value = body.isEmpty() ? 0 : body.split(",").length;
        }
        else if (tag instanceof NBTTagCompound) value = ((NBTTagCompound) tag).getSize();
        else if (tag instanceof NBTTagString) value = ((NBTTagString) tag).getString().length();
        else throw GET_UNKNOWN.create(path.toString());
        source.sendFeedback(target.queryMessage(tag), false);
        return value;
    }

    private static int getScaled(CommandSource source, Target target, NbtPath path, double scale) throws CommandSyntaxException {
        NBTBase tag = single(target, path);
        if (!(tag instanceof NBTPrimitive)) throw GET_INVALID.create(path.toString());
        int value = MathHelper.floor(((NBTPrimitive) tag).getDouble() * scale);
        source.sendFeedback(target.getMessage(path, scale, value), false);
        return value;
    }

    private DataCommand() {}
}
