package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.EntityAnchor;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.arguments.BlockPosArgument;
import com.michaelsebero.portium.command.arguments.BlockPredicateArgument;
import com.michaelsebero.portium.command.arguments.Coordinates;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.MinMaxBounds;
import com.michaelsebero.portium.command.arguments.MiscArguments;
import com.michaelsebero.portium.command.arguments.NbtArguments;
import com.michaelsebero.portium.command.arguments.RegistryArguments;
import com.michaelsebero.portium.command.arguments.ResourceLocationArgument;
import com.michaelsebero.portium.command.arguments.RotationArgument;
import com.michaelsebero.portium.command.arguments.ScoreboardArguments;
import com.michaelsebero.portium.command.arguments.SwizzleArgument;
import com.michaelsebero.portium.command.arguments.Vec3Argument;
import com.michaelsebero.portium.command.brigadier.Command;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.ResultConsumer;
import com.michaelsebero.portium.command.brigadier.arguments.DoubleArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.ArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.tree.CommandNode;
import com.michaelsebero.portium.command.brigadier.tree.LiteralCommandNode;
import com.michaelsebero.portium.command.nbt.NbtPath;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/**
 * 1.13's {@code execute}: changes who, where and how the rest of the command runs ({@code as}, {@code at},
 * {@code positioned}, {@code rotated}, {@code facing}, {@code align}, {@code anchored}, {@code in}), tests conditions
 * ({@code if}/{@code unless} {@code block}, {@code blocks}, {@code entity}, {@code score}) and stores the result
 * ({@code store result|success score|block|entity|bossbar}).
 */
public final class ExecuteCommand {

    private static final CommandExceptionType.Dynamic2 TOO_MANY_BLOCKS = new CommandExceptionType.Dynamic2((max, count) -> TextMessage.translate("commands.execute.blocks.toobig", max, count));
    private static final CommandExceptionType.Simple TEST_FAILED = new CommandExceptionType.Simple(TextMessage.translate("commands.execute.conditional.fail"));
    private static final CommandExceptionType.Dynamic TEST_FAILED_COUNT = new CommandExceptionType.Dynamic(count -> TextMessage.translate("commands.execute.conditional.fail_count", count));

    private interface Condition {
        boolean test(CommandContext<CommandSource> context) throws CommandSyntaxException;
    }

    private interface Counter {
        int count(CommandContext<CommandSource> context) throws CommandSyntaxException;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralCommandNode<CommandSource> root = dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2)));
        dispatcher.register(literal("execute").requires(source -> source.hasPermissionLevel(2))
                .then(literal("run").redirect(dispatcher.getRoot()))
                .then(conditions(root, literal("if"), true))
                .then(conditions(root, literal("unless"), false))
                .then(literal("as").then(argument("targets", EntityArgument.entities()).fork(root, context -> {
                    List<CommandSource> sources = new ArrayList<>();
                    for (Entity entity : EntityArgument.getEntitiesAllowingNone(context, "targets")) sources.add(context.getSource().withEntity(entity));
                    return sources;
                })))
                .then(literal("at").then(argument("targets", EntityArgument.entities()).fork(root, context -> {
                    List<CommandSource> sources = new ArrayList<>();
                    for (Entity entity : EntityArgument.getEntitiesAllowingNone(context, "targets")) {
                        sources.add(context.getSource().withWorld((WorldServer) entity.world).withPos(entity.getPositionVector())
                                .withRotation(new Vec2(entity.rotationPitch, entity.rotationYaw)));
                    }
                    return sources;
                })))
                .then(literal("store").then(store(root, literal("result"), true)).then(store(root, literal("success"), false)))
                .then(literal("positioned")
                        .then(argument("pos", Vec3Argument.vec3()).redirect(root, context -> context.getSource().withPos(Vec3Argument.getVec3(context, "pos"))))
                        .then(literal("as").then(argument("targets", EntityArgument.entities()).fork(root, context -> {
                            List<CommandSource> sources = new ArrayList<>();
                            for (Entity entity : EntityArgument.getEntitiesAllowingNone(context, "targets")) sources.add(context.getSource().withPos(entity.getPositionVector()));
                            return sources;
                        }))))
                .then(literal("rotated")
                        .then(argument("rot", RotationArgument.rotation()).redirect(root, context ->
                                context.getSource().withRotation(context.getArgument("rot", Coordinates.class).getRotation(context.getSource()))))
                        .then(literal("as").then(argument("targets", EntityArgument.entities()).fork(root, context -> {
                            List<CommandSource> sources = new ArrayList<>();
                            for (Entity entity : EntityArgument.getEntitiesAllowingNone(context, "targets")) {
                                sources.add(context.getSource().withRotation(new Vec2(entity.rotationPitch, entity.rotationYaw)));
                            }
                            return sources;
                        }))))
                .then(literal("facing")
                        .then(literal("entity").then(argument("targets", EntityArgument.entities()).then(argument("anchor", MiscArguments.anchor()).fork(root, context -> {
                            EntityAnchor anchor = context.getArgument("anchor", EntityAnchor.class);
                            List<CommandSource> sources = new ArrayList<>();
                            for (Entity entity : EntityArgument.getEntitiesAllowingNone(context, "targets")) sources.add(context.getSource().facing(entity, anchor));
                            return sources;
                        }))))
                        .then(argument("pos", Vec3Argument.vec3()).redirect(root, context -> context.getSource().facing(Vec3Argument.getVec3(context, "pos")))))
                .then(literal("align").then(argument("axes", SwizzleArgument.swizzle()).redirect(root, context ->
                        context.getSource().withPos(align(context.getSource().getPos(), SwizzleArgument.getSwizzle(context, "axes"))))))
                .then(literal("anchored").then(argument("anchor", MiscArguments.anchor()).redirect(root, context ->
                        context.getSource().withAnchor(context.getArgument("anchor", EntityAnchor.class)))))
                .then(literal("in").then(argument("dimension", RegistryArguments.dimension()).redirect(root, context ->
                        context.getSource().withWorld(context.getSource().getServer().getWorld(context.getArgument("dimension", DimensionType.class).getId()))))));
    }

    private static Vec3d align(Vec3d pos, EnumSet<EnumFacing.Axis> axes) {
        return new Vec3d(axes.contains(EnumFacing.Axis.X) ? Math.floor(pos.x) : pos.x, axes.contains(EnumFacing.Axis.Y) ? Math.floor(pos.y) : pos.y,
                axes.contains(EnumFacing.Axis.Z) ? Math.floor(pos.z) : pos.z);
    }

    // ---- store ----

    private static ResultConsumer<CommandSource> combine(ResultConsumer<CommandSource> first, ResultConsumer<CommandSource> second) {
        return (context, success, result) -> {
            first.onCommandComplete(context, success, result);
            second.onCommandComplete(context, success, result);
        };
    }

    private static ArgumentBuilder<CommandSource, ?> store(LiteralCommandNode<CommandSource> root, LiteralArgumentBuilder<CommandSource> builder, boolean result) {
        builder.then(literal("score").then(argument("targets", ScoreboardArguments.scoreHolder(true)).then(argument("objective", ScoreboardArguments.objective())
                .redirect(root, context -> storeScore(context.getSource(), ScoreboardArguments.getScoreHolders(context, "targets"),
                        ScoreboardArguments.getObjective(context, "objective"), result)))));
        builder.then(literal("bossbar").then(argument("id", ResourceLocationArgument.resourceLocation()).suggests(BossBarCommand.SUGGESTIONS)
                .then(literal("value").redirect(root, context -> storeBossBar(context.getSource(), BossBarCommand.getBossBar(context), true, result)))
                .then(literal("max").redirect(root, context -> storeBossBar(context.getSource(), BossBarCommand.getBossBar(context), false, result)))));
        for (DataCommand.TargetType type : DataCommand.TARGETS) {
            type.addArguments(builder, target -> target.then(argument("path", NbtArguments.path())
                    .then(storeType(root, type, "int", result, scaled -> new NBTTagInt((int) scaled)))
                    .then(storeType(root, type, "float", result, scaled -> new NBTTagFloat((float) scaled)))
                    .then(storeType(root, type, "short", result, scaled -> new NBTTagShort((short) scaled)))
                    .then(storeType(root, type, "long", result, scaled -> new NBTTagLong((long) scaled)))
                    .then(storeType(root, type, "double", result, scaled -> new NBTTagDouble(scaled)))
                    .then(storeType(root, type, "byte", result, scaled -> new NBTTagByte((byte) scaled)))));
        }
        return builder;
    }

    private interface TagFactory {
        NBTBase create(double scaled);
    }

    private static ArgumentBuilder<CommandSource, ?> storeType(LiteralCommandNode<CommandSource> root, DataCommand.TargetType type, String name, boolean result, TagFactory factory) {
        return literal(name).then(argument("scale", DoubleArgumentType.doubleArg()).redirect(root, context -> storeNbt(context.getSource(), type.get(context),
                NbtArguments.getPath(context, "path"), value -> factory.create(value * DoubleArgumentType.getDouble(context, "scale")), result)));
    }

    private static CommandSource storeScore(CommandSource source, Collection<String> targets, ScoreObjective objective, boolean result) {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        return source.withResultConsumer((context, success, value) -> {
            for (String target : targets) scoreboard.getOrCreateScore(target, objective).setScorePoints(result ? value : (success ? 1 : 0));
        }, ExecuteCommand::combine);
    }

    private static CommandSource storeBossBar(CommandSource source, BossBarCommand.CustomBossBar bar, boolean storeValue, boolean result) {
        return source.withResultConsumer((context, success, value) -> {
            int stored = result ? value : (success ? 1 : 0);
            if (storeValue) bar.setValue(stored);
            else bar.setMax(stored);
        }, ExecuteCommand::combine);
    }

    private static CommandSource storeNbt(CommandSource source, DataCommand.Target target, NbtPath path, IntFunction<NBTBase> tag, boolean result) {
        return source.withResultConsumer((context, success, value) -> {
            try {
                NBTTagCompound data = target.getData();
                int stored = result ? value : (success ? 1 : 0);
                path.set(data, () -> tag.apply(stored));
                target.setData(data);
            } catch (CommandSyntaxException ignored) {
                // Storing into a path that doesn't fit just does nothing.
            }
        }, ExecuteCommand::combine);
    }

    // ---- if / unless ----

    private static ArgumentBuilder<CommandSource, ?> conditions(CommandNode<CommandSource> root, LiteralArgumentBuilder<CommandSource> builder, boolean isIf) {
        builder.then(literal("block").then(argument("pos", BlockPosArgument.blockPos()).then(condition(root, argument("block", BlockPredicateArgument.blockPredicate()), isIf,
                context -> BlockPredicateArgument.getBlockPredicate(context, "block").test(context.getSource().getWorld(), BlockPosArgument.getLoadedBlockPos(context, "pos"))))));
        builder.then(literal("score").then(argument("target", ScoreboardArguments.scoreHolder(false)).then(argument("targetObjective", ScoreboardArguments.objective())
                .then(compare(root, "=", isIf, (a, b) -> a.intValue() == b.intValue()))
                .then(compare(root, "<", isIf, (a, b) -> a < b))
                .then(compare(root, "<=", isIf, (a, b) -> a <= b))
                .then(compare(root, ">", isIf, (a, b) -> a > b))
                .then(compare(root, ">=", isIf, (a, b) -> a >= b))
                .then(literal("matches").then(condition(root, argument("range", MiscArguments.intRange()), isIf, context ->
                        matchesRange(context, context.getArgument("range", MinMaxBounds.class))))))));
        builder.then(literal("blocks").then(argument("start", BlockPosArgument.blockPos()).then(argument("end", BlockPosArgument.blockPos())
                .then(argument("destination", BlockPosArgument.blockPos()).then(blocks(root, literal("all"), isIf, false)).then(blocks(root, literal("masked"), isIf, true))))));
        builder.then(literal("entity").then(argument("entities", EntityArgument.entities())
                .fork(root, context -> pass(context, isIf, !EntityArgument.getEntitiesAllowingNone(context, "entities").isEmpty()))
                .executes(counted(isIf, context -> EntityArgument.getEntitiesAllowingNone(context, "entities").size()))));
        return builder;
    }

    private static ArgumentBuilder<CommandSource, ?> compare(CommandNode<CommandSource> root, String operator, boolean isIf, BiPredicate<Integer, Integer> test) {
        return literal(operator).then(argument("source", ScoreboardArguments.scoreHolder(false)).then(condition(root, argument("sourceObjective", ScoreboardArguments.objective()),
                isIf, context -> compareScores(context, test))));
    }

    private static ArgumentBuilder<CommandSource, ?> condition(CommandNode<CommandSource> root, ArgumentBuilder<CommandSource, ?> builder, boolean isIf, Condition condition) {
        return builder.fork(root, context -> pass(context, isIf, condition.test(context))).executes(context -> {
            if (isIf != condition.test(context)) throw TEST_FAILED.create();
            context.getSource().sendFeedback(new TextComponentTranslation("commands.execute.conditional.pass"), false);
            return 1;
        });
    }

    /** {@code if entity} reports how many matched; {@code unless entity} passes only when none did. */
    private static Command<CommandSource> counted(boolean isIf, Counter counter) {
        return isIf ? context -> {
            int count = counter.count(context);
            if (count <= 0) throw TEST_FAILED.create();
            context.getSource().sendFeedback(new TextComponentTranslation("commands.execute.conditional.pass_count", count), false);
            return count;
        } : context -> {
            int count = counter.count(context);
            if (count != 0) throw TEST_FAILED_COUNT.create(count);
            context.getSource().sendFeedback(new TextComponentTranslation("commands.execute.conditional.pass"), false);
            return 1;
        };
    }

    private static Collection<CommandSource> pass(CommandContext<CommandSource> context, boolean isIf, boolean result) {
        return result == isIf ? Collections.singleton(context.getSource()) : Collections.emptyList();
    }

    private static boolean compareScores(CommandContext<CommandSource> context, BiPredicate<Integer, Integer> test) throws CommandSyntaxException {
        String target = ScoreboardArguments.getScoreHolder(context, "target");
        ScoreObjective targetObjective = ScoreboardArguments.getObjective(context, "targetObjective");
        String source = ScoreboardArguments.getScoreHolder(context, "source");
        ScoreObjective sourceObjective = ScoreboardArguments.getObjective(context, "sourceObjective");
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(context.getSource());
        if (!scoreboard.entityHasObjective(target, targetObjective) || !scoreboard.entityHasObjective(source, sourceObjective)) return false;
        return test.test(scoreboard.getOrCreateScore(target, targetObjective).getScorePoints(), scoreboard.getOrCreateScore(source, sourceObjective).getScorePoints());
    }

    private static boolean matchesRange(CommandContext<CommandSource> context, MinMaxBounds range) throws CommandSyntaxException {
        String target = ScoreboardArguments.getScoreHolder(context, "target");
        ScoreObjective objective = ScoreboardArguments.getObjective(context, "targetObjective");
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(context.getSource());
        return scoreboard.entityHasObjective(target, objective) && range.test(scoreboard.getOrCreateScore(target, objective).getScorePoints());
    }

    private static ArgumentBuilder<CommandSource, ?> blocks(CommandNode<CommandSource> root, LiteralArgumentBuilder<CommandSource> builder, boolean isIf, boolean masked) {
        return builder.fork(root, context -> pass(context, isIf, countMatchingBlocks(context, masked).isPresent())).executes(isIf ? context -> {
            OptionalInt count = countMatchingBlocks(context, masked);
            if (!count.isPresent()) throw TEST_FAILED.create();
            context.getSource().sendFeedback(new TextComponentTranslation("commands.execute.conditional.pass_count", count.getAsInt()), false);
            return count.getAsInt();
        } : context -> {
            OptionalInt count = countMatchingBlocks(context, masked);
            if (count.isPresent()) throw TEST_FAILED_COUNT.create(count.getAsInt());
            context.getSource().sendFeedback(new TextComponentTranslation("commands.execute.conditional.pass"), false);
            return 1;
        });
    }

    /** How many blocks match between the two regions, or empty if any differs ({@code masked} ignores air in the source). */
    private static OptionalInt countMatchingBlocks(CommandContext<CommandSource> context, boolean masked) throws CommandSyntaxException {
        WorldServer world = context.getSource().getWorld();
        BlockPos start = BlockPosArgument.getLoadedBlockPos(context, "start");
        BlockPos end = BlockPosArgument.getLoadedBlockPos(context, "end");
        BlockPos destination = BlockPosArgument.getLoadedBlockPos(context, "destination");
        StructureBoundingBox box = new StructureBoundingBox(start, end);
        int volume = box.getXSize() * box.getYSize() * box.getZSize();
        if (volume > 32768) throw TOO_MANY_BLOCKS.create(32768, volume);
        BlockPos offset = new BlockPos(destination.getX() - box.minX, destination.getY() - box.minY, destination.getZ() - box.minZ);
        int count = 0;
        for (int z = box.minZ; z <= box.maxZ; ++z) {
            for (int y = box.minY; y <= box.maxY; ++y) {
                for (int x = box.minX; x <= box.maxX; ++x) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockPos other = pos.add(offset);
                    IBlockState state = world.getBlockState(pos);
                    if (masked && state.getBlock() == Blocks.AIR) continue;
                    if (state != world.getBlockState(other)) return OptionalInt.empty();
                    TileEntity tile = world.getTileEntity(pos);
                    if (tile != null) {
                        TileEntity otherTile = world.getTileEntity(other);
                        if (otherTile == null || !withoutPosition(tile).equals(withoutPosition(otherTile))) return OptionalInt.empty();
                    }
                    ++count;
                }
            }
        }
        return OptionalInt.of(count);
    }

    private static NBTTagCompound withoutPosition(TileEntity tile) {
        NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
        data.removeTag("x");
        data.removeTag("y");
        data.removeTag("z");
        return data;
    }

    private ExecuteCommand() {}
}
