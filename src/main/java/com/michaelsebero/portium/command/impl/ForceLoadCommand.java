package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.ColumnPosArgument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.world.ForcedChunks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code forceload} (1.13.1): keeps chunks loaded without a player nearby. */
public final class ForceLoadCommand {

    private static final int MAX_CHUNKS = 256;

    private static final CommandExceptionType.Dynamic2 TOO_BIG = new CommandExceptionType.Dynamic2((max, count) -> TextMessage.translate("commands.forceload.toobig", max, count));
    private static final CommandExceptionType.Dynamic2 QUERY_FAILURE = new CommandExceptionType.Dynamic2((chunk, dimension) ->
            TextMessage.translate("commands.forceload.query.failure", chunk, dimension));
    private static final CommandExceptionType.Simple ADDED_FAILURE = new CommandExceptionType.Simple(TextMessage.translate("commands.forceload.added.failure"));
    private static final CommandExceptionType.Simple REMOVED_FAILURE = new CommandExceptionType.Simple(TextMessage.translate("commands.forceload.removed.failure"));

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("forceload").requires(source -> source.hasPermissionLevel(2))
                .then(literal("add").then(argument("from", ColumnPosArgument.columnPos())
                        .executes(context -> change(context.getSource(), ColumnPosArgument.getColumnPos(context, "from"), ColumnPosArgument.getColumnPos(context, "from"), true))
                        .then(argument("to", ColumnPosArgument.columnPos()).executes(context ->
                                change(context.getSource(), ColumnPosArgument.getColumnPos(context, "from"), ColumnPosArgument.getColumnPos(context, "to"), true)))))
                .then(literal("remove")
                        .then(argument("from", ColumnPosArgument.columnPos())
                                .executes(context -> change(context.getSource(), ColumnPosArgument.getColumnPos(context, "from"), ColumnPosArgument.getColumnPos(context, "from"), false))
                                .then(argument("to", ColumnPosArgument.columnPos()).executes(context ->
                                        change(context.getSource(), ColumnPosArgument.getColumnPos(context, "from"), ColumnPosArgument.getColumnPos(context, "to"), false))))
                        .then(literal("all").executes(context -> removeAll(context.getSource()))))
                .then(literal("query").executes(context -> list(context.getSource()))
                        .then(argument("pos", ColumnPosArgument.columnPos()).executes(context -> query(context.getSource(), ColumnPosArgument.getColumnPos(context, "pos"))))));
    }

    private static String dimensionName(WorldServer world) {
        return "minecraft:" + world.provider.getDimensionType().getName();
    }

    private static int query(CommandSource source, BlockPos pos) throws CommandSyntaxException {
        WorldServer world = source.getWorld();
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        if (!ForcedChunks.get(world).isForced(chunk.x, chunk.z)) throw QUERY_FAILURE.create(chunk, dimensionName(world));
        source.sendFeedback(new TextComponentTranslation("commands.forceload.query.success", chunk, dimensionName(world)), false);
        return 1;
    }

    private static int list(CommandSource source) {
        WorldServer world = source.getWorld();
        List<Long> keys = new ArrayList<>(ForcedChunks.get(world).getChunks());
        if (keys.isEmpty()) {
            source.sendErrorMessage(new TextComponentTranslation("commands.forceload.added.none", dimensionName(world)));
            return 0;
        }
        String chunks = keys.stream().sorted()
                .map(key -> new ChunkPos((int) (long) key, (int) (key >> 32)).toString()).collect(Collectors.joining(", "));
        if (keys.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.forceload.list.single", dimensionName(world), chunks), false);
        else source.sendFeedback(new TextComponentTranslation("commands.forceload.list.multiple", keys.size(), dimensionName(world), chunks), false);
        return keys.size();
    }

    private static int removeAll(CommandSource source) {
        WorldServer world = source.getWorld();
        ForcedChunks forced = ForcedChunks.get(world);
        for (long key : new ArrayList<>(forced.getChunks())) forced.setForced(world, (int) key, (int) (key >> 32), false);
        source.sendFeedback(new TextComponentTranslation("commands.forceload.removed.all", dimensionName(world)), true);
        return 0;
    }

    private static int change(CommandSource source, BlockPos from, BlockPos to, boolean add) throws CommandSyntaxException {
        int minX = Math.min(from.getX(), to.getX());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxX = Math.max(from.getX(), to.getX());
        int maxZ = Math.max(from.getZ(), to.getZ());
        if (minX < -30000000 || minZ < -30000000 || maxX >= 30000000 || maxZ >= 30000000) throw CommandExceptions.POS_OUT_OF_WORLD.create();
        int chunkMinX = minX >> 4;
        int chunkMinZ = minZ >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMaxZ = maxZ >> 4;
        long area = ((long) (chunkMaxX - chunkMinX) + 1L) * ((long) (chunkMaxZ - chunkMinZ) + 1L);
        if (area > MAX_CHUNKS) throw TOO_BIG.create(MAX_CHUNKS, area);
        WorldServer world = source.getWorld();
        ForcedChunks forced = ForcedChunks.get(world);
        ChunkPos first = null;
        int changed = 0;
        for (int x = chunkMinX; x <= chunkMaxX; x++) {
            for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                if (!forced.setForced(world, x, z, add)) continue;
                if (first == null) first = new ChunkPos(x, z);
                changed++;
            }
        }
        if (changed == 0) throw (add ? ADDED_FAILURE : REMOVED_FAILURE).create();
        if (changed == 1) {
            source.sendFeedback(new TextComponentTranslation(add ? "commands.forceload.added.single" : "commands.forceload.removed.single", first, dimensionName(world)), true);
        } else {
            source.sendFeedback(new TextComponentTranslation(add ? "commands.forceload.added.multiple" : "commands.forceload.removed.multiple", changed, dimensionName(world),
                    new ChunkPos(chunkMinX, chunkMinZ), new ChunkPos(chunkMaxX, chunkMaxZ)), true);
        }
        return changed;
    }

    private ForceLoadCommand() {}
}
