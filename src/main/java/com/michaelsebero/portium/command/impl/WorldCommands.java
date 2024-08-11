package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.arguments.BlockPosArgument;
import com.michaelsebero.portium.command.arguments.IdSuggestions;
import com.michaelsebero.portium.command.arguments.MiscArguments;
import com.michaelsebero.portium.command.arguments.Vec2Argument;
import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.FloatArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandGameRule;
import net.minecraft.network.play.server.SPacketSpawnPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.storage.WorldInfo;

import java.util.Arrays;
import java.util.Locale;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/**
 * {@code time}, {@code weather}, {@code difficulty}, {@code gamerule}, {@code setworldspawn}, {@code seed},
 * {@code worldborder} and {@code locate} (1.13).
 */
public final class WorldCommands {

    private static final CommandExceptionType.Dynamic DIFFICULTY_UNCHANGED = dynamic("commands.difficulty.failure");
    private static final CommandExceptionType.Simple LOCATE_FAILED = simple("commands.locate.failed");
    private static final CommandExceptionType.Simple BORDER_CENTER_UNCHANGED = simple("commands.worldborder.center.failed");
    private static final CommandExceptionType.Simple BORDER_SIZE_UNCHANGED = simple("commands.worldborder.set.failed.nochange");
    private static final CommandExceptionType.Simple BORDER_TOO_SMALL = simple("commands.worldborder.set.failed.small");
    private static final CommandExceptionType.Simple BORDER_TOO_BIG = simple("commands.worldborder.set.failed.big");
    private static final CommandExceptionType.Simple BORDER_WARNING_TIME_UNCHANGED = simple("commands.worldborder.warning.time.failed");
    private static final CommandExceptionType.Simple BORDER_WARNING_DISTANCE_UNCHANGED = simple("commands.worldborder.warning.distance.failed");
    private static final CommandExceptionType.Simple BORDER_DAMAGE_BUFFER_UNCHANGED = simple("commands.worldborder.damage.buffer.failed");
    private static final CommandExceptionType.Simple BORDER_DAMAGE_AMOUNT_UNCHANGED = simple("commands.worldborder.damage.amount.failed");

    private static final String[] STRUCTURES = {"Buried_Treasure", "Desert_Pyramid", "EndCity", "Fortress", "Igloo", "Jungle_Pyramid", "Mansion",
            "Mineshaft", "Monument", "Ocean_Ruin", "Shipwreck", "Stronghold", "Swamp_Hut", "Village"};

    private static final SuggestionProvider<CommandSource> GAME_RULES = (context, builder) ->
            IdSuggestions.suggest(Arrays.asList(rules(context.getSource().getServer()).getRules()), builder);

    private static final SuggestionProvider<CommandSource> GAME_RULE_VALUES = (context, builder) -> {
        String rule = StringArgumentType.getString(context, "rule");
        if (rules(context.getSource().getServer()).areSameType(rule, GameRules.ValueType.BOOLEAN_VALUE)) {
            return IdSuggestions.suggest(Arrays.asList("true", "false"), builder);
        }
        return builder.build();
    };

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    private static CommandExceptionType.Dynamic dynamic(String key) {
        return new CommandExceptionType.Dynamic(arg -> TextMessage.translate(key, arg));
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        registerTime(dispatcher);
        registerWeather(dispatcher);
        registerDifficulty(dispatcher);
        registerGameRule(dispatcher);
        registerWorldBorder(dispatcher);

        dispatcher.register(literal("setworldspawn").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> setWorldSpawn(context.getSource(), new BlockPos(context.getSource().getPos())))
                .then(argument("pos", BlockPosArgument.blockPos()).executes(context -> setWorldSpawn(context.getSource(), BlockPosArgument.getBlockPos(context, "pos")))));

        dispatcher.register(literal("seed").requires(source -> source.getServer().isSinglePlayer() || source.hasPermissionLevel(2)).executes(context -> {
            long seed = context.getSource().getWorld().getSeed();
            context.getSource().sendFeedback(new TextComponentTranslation("commands.seed.success", seed), false);
            return (int) seed;
        }));

        LiteralArgumentBuilder<CommandSource> locate = literal("locate").requires(source -> source.hasPermissionLevel(2));
        for (String structure : STRUCTURES) locate.then(literal(structure).executes(context -> locate(context.getSource(), structure)));
        dispatcher.register(locate);
    }

    // ---- time ----

    private static void registerTime(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("time").requires(source -> source.hasPermissionLevel(2))
                .then(literal("set")
                        .then(literal("day").executes(context -> setTime(context.getSource(), 1000)))
                        .then(literal("noon").executes(context -> setTime(context.getSource(), 6000)))
                        .then(literal("night").executes(context -> setTime(context.getSource(), 13000)))
                        .then(literal("midnight").executes(context -> setTime(context.getSource(), 18000)))
                        .then(argument("time", MiscArguments.time()).executes(context -> setTime(context.getSource(), context.getArgument("time", Integer.class)))))
                .then(literal("add").then(argument("time", MiscArguments.time()).executes(context -> addTime(context.getSource(), context.getArgument("time", Integer.class)))))
                .then(literal("query")
                        .then(literal("daytime").executes(context -> queryTime(context.getSource(), dayTime(context.getSource().getWorld()))))
                        .then(literal("gametime").executes(context -> queryTime(context.getSource(), (int) (context.getSource().getWorld().getTotalWorldTime() % Integer.MAX_VALUE))))
                        .then(literal("day").executes(context -> queryTime(context.getSource(), (int) (context.getSource().getWorld().getWorldTime() / 24000L % Integer.MAX_VALUE))))));
    }

    private static int dayTime(WorldServer world) {
        return (int) (world.getWorldTime() % 24000L);
    }

    private static int setTime(CommandSource source, int time) {
        for (WorldServer world : source.getServer().worlds) world.setWorldTime(time);
        source.sendFeedback(new TextComponentTranslation("commands.time.set", time), true);
        return dayTime(source.getWorld());
    }

    private static int addTime(CommandSource source, int amount) {
        for (WorldServer world : source.getServer().worlds) world.setWorldTime(world.getWorldTime() + amount);
        int time = dayTime(source.getWorld());
        source.sendFeedback(new TextComponentTranslation("commands.time.set", time), true);
        return time;
    }

    private static int queryTime(CommandSource source, int value) {
        source.sendFeedback(new TextComponentTranslation("commands.time.query", value), false);
        return value;
    }

    // ---- weather ----

    private enum Weather {
        CLEAR, RAIN, THUNDER
    }

    private static void registerWeather(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> weather = literal("weather").requires(source -> source.hasPermissionLevel(2));
        for (Weather type : Weather.values()) {
            weather.then(literal(type.name().toLowerCase(Locale.ROOT)).executes(context -> setWeather(context.getSource(), type, 6000))
                    .then(argument("duration", IntegerArgumentType.integer(0, 1000000)).executes(context ->
                            setWeather(context.getSource(), type, IntegerArgumentType.getInteger(context, "duration") * 20))));
        }
        dispatcher.register(weather);
    }

    private static int setWeather(CommandSource source, Weather type, int time) {
        WorldInfo info = source.getServer().worlds[0].getWorldInfo();
        info.setCleanWeatherTime(type == Weather.CLEAR ? time : 0);
        info.setRainTime(type == Weather.CLEAR ? 0 : time);
        info.setThunderTime(type == Weather.CLEAR ? 0 : time);
        info.setRaining(type != Weather.CLEAR);
        info.setThundering(type == Weather.THUNDER);
        source.sendFeedback(new TextComponentTranslation("commands.weather.set." + type.name().toLowerCase(Locale.ROOT)), true);
        return time;
    }

    // ---- difficulty ----

    private static void registerDifficulty(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> difficulty = literal("difficulty").requires(source -> source.hasPermissionLevel(2)).executes(context -> {
            EnumDifficulty current = context.getSource().getServer().worlds[0].getDifficulty();
            context.getSource().sendFeedback(new TextComponentTranslation("commands.difficulty.query", displayName(current)), false);
            return current.getId();
        });
        for (EnumDifficulty value : EnumDifficulty.values()) {
            difficulty.then(literal(value.name().toLowerCase(Locale.ROOT)).executes(context -> {
                MinecraftServer server = context.getSource().getServer();
                if (server.worlds[0].getDifficulty() == value) throw DIFFICULTY_UNCHANGED.create(value.name().toLowerCase(Locale.ROOT));
                server.setDifficultyForAllWorlds(value);
                context.getSource().sendFeedback(new TextComponentTranslation("commands.difficulty.success", displayName(value)), true);
                return 0;
            }));
        }
        dispatcher.register(difficulty);
    }

    private static ITextComponent displayName(EnumDifficulty difficulty) {
        return new TextComponentTranslation("options.difficulty." + difficulty.name().toLowerCase(Locale.ROOT));
    }

    // ---- gamerule ----

    private static GameRules rules(MinecraftServer server) {
        return server.worlds[0].getGameRules();
    }

    /** Rules are listed from the world, so rules other mods add work too; values are checked against the rule's type. */
    private static void registerGameRule(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("gamerule").requires(source -> source.hasPermissionLevel(2))
                .then(argument("rule", StringArgumentType.word()).suggests(GAME_RULES)
                        .executes(context -> {
                            String rule = knownRule(context);
                            String value = rules(context.getSource().getServer()).getString(rule);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.gamerule.query", rule, value), false);
                            return rules(context.getSource().getServer()).getInt(rule);
                        })
                        .then(argument("value", StringArgumentType.greedyString()).suggests(GAME_RULE_VALUES).executes(context -> {
                            String rule = knownRule(context);
                            String value = StringArgumentType.getString(context, "value");
                            MinecraftServer server = context.getSource().getServer();
                            GameRules rules = rules(server);
                            if (rules.areSameType(rule, GameRules.ValueType.BOOLEAN_VALUE) && !value.equals("true") && !value.equals("false")) {
                                throw BuiltInExceptions.READER_INVALID_BOOL.create(value);
                            }
                            if (rules.areSameType(rule, GameRules.ValueType.NUMERICAL_VALUE)) {
                                try {
                                    Integer.parseInt(value);
                                } catch (NumberFormatException e) {
                                    throw BuiltInExceptions.READER_INVALID_INT.create(value);
                                }
                            }
                            rules.setOrCreateGameRule(rule, value);
                            CommandGameRule.notifyGameRuleChange(rules, rule, server);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.gamerule.set", rule, value), true);
                            return rules.getInt(rule);
                        }))));
    }

    private static String knownRule(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String rule = StringArgumentType.getString(context, "rule");
        if (!rules(context.getSource().getServer()).hasRule(rule)) throw BuiltInExceptions.DISPATCHER_UNKNOWN_ARGUMENT.create();
        return rule;
    }

    // ---- setworldspawn ----

    private static int setWorldSpawn(CommandSource source, BlockPos pos) {
        source.getWorld().setSpawnPoint(pos);
        source.getServer().getPlayerList().sendPacketToAllPlayers(new SPacketSpawnPosition(pos));
        source.sendFeedback(new TextComponentTranslation("commands.setworldspawn.success", pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    // ---- locate ----

    private static int locate(CommandSource source, String structure) throws CommandSyntaxException {
        BlockPos origin = new BlockPos(source.getPos());
        BlockPos found = source.getWorld().findNearestStructure(structure, origin, false);
        if (found == null) throw LOCATE_FAILED.create();
        int distance = MathHelper.floor(Math.sqrt((double) (found.getX() - origin.getX()) * (found.getX() - origin.getX())
                + (double) (found.getZ() - origin.getZ()) * (found.getZ() - origin.getZ())));
        ITextComponent coordinates = new TextComponentTranslation("chat.coordinates", found.getX(), "~", found.getZ());
        ITextComponent link = new net.minecraft.util.text.TextComponentString("[").appendSibling(coordinates).appendText("]");
        link.getStyle().setColor(TextFormatting.GREEN)
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + found.getX() + " ~ " + found.getZ()))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("chat.coordinates.tooltip")));
        source.sendFeedback(new TextComponentTranslation("commands.locate.success", structure, link, distance), false);
        return distance;
    }

    // ---- worldborder ----

    private static void registerWorldBorder(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("worldborder").requires(source -> source.hasPermissionLevel(2))
                .then(literal("add").then(argument("distance", FloatArgumentType.floatArg(-6.0E7F, 6.0E7F))
                        .executes(context -> setBorderSize(context.getSource(), border(context).getDiameter() + FloatArgumentType.getFloat(context, "distance"), 0L))
                        .then(argument("time", IntegerArgumentType.integer(0)).executes(context -> setBorderSize(context.getSource(),
                                border(context).getDiameter() + FloatArgumentType.getFloat(context, "distance"),
                                border(context).getTimeUntilTarget() + IntegerArgumentType.getInteger(context, "time") * 1000L)))))
                .then(literal("set").then(argument("distance", FloatArgumentType.floatArg(-6.0E7F, 6.0E7F))
                        .executes(context -> setBorderSize(context.getSource(), FloatArgumentType.getFloat(context, "distance"), 0L))
                        .then(argument("time", IntegerArgumentType.integer(0)).executes(context ->
                                setBorderSize(context.getSource(), FloatArgumentType.getFloat(context, "distance"), IntegerArgumentType.getInteger(context, "time") * 1000L)))))
                .then(literal("center").then(argument("pos", Vec2Argument.vec2()).executes(context -> {
                    WorldBorder border = border(context);
                    Vec2 center = Vec2Argument.getVec2(context, "pos");
                    if (border.getCenterX() == center.x && border.getCenterZ() == center.y) throw BORDER_CENTER_UNCHANGED.create();
                    border.setCenter(center.x, center.y);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.worldborder.center.success",
                            String.format(Locale.ROOT, "%.2f", center.x), String.format(Locale.ROOT, "%.2f", center.y)), true);
                    return 0;
                })))
                .then(literal("damage")
                        .then(literal("amount").then(argument("damagePerBlock", FloatArgumentType.floatArg(0.0F)).executes(context -> {
                            WorldBorder border = border(context);
                            float amount = FloatArgumentType.getFloat(context, "damagePerBlock");
                            if (border.getDamageAmount() == amount) throw BORDER_DAMAGE_AMOUNT_UNCHANGED.create();
                            border.setDamageAmount(amount);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.worldborder.damage.amount.success",
                                    String.format(Locale.ROOT, "%.2f", amount)), true);
                            return (int) amount;
                        })))
                        .then(literal("buffer").then(argument("distance", FloatArgumentType.floatArg(0.0F)).executes(context -> {
                            WorldBorder border = border(context);
                            float distance = FloatArgumentType.getFloat(context, "distance");
                            if (border.getDamageBuffer() == distance) throw BORDER_DAMAGE_BUFFER_UNCHANGED.create();
                            border.setDamageBuffer(distance);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.worldborder.damage.buffer.success",
                                    String.format(Locale.ROOT, "%.2f", distance)), true);
                            return (int) distance;
                        }))))
                .then(literal("get").executes(context -> {
                    double diameter = border(context).getDiameter();
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.worldborder.get", String.format(Locale.ROOT, "%.0f", diameter)), false);
                    return MathHelper.floor(diameter + 0.5D);
                }))
                .then(literal("warning")
                        .then(literal("distance").then(argument("distance", IntegerArgumentType.integer(0)).executes(context -> {
                            WorldBorder border = border(context);
                            int distance = IntegerArgumentType.getInteger(context, "distance");
                            if (border.getWarningDistance() == distance) throw BORDER_WARNING_DISTANCE_UNCHANGED.create();
                            border.setWarningDistance(distance);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.worldborder.warning.distance.success", distance), true);
                            return distance;
                        })))
                        .then(literal("time").then(argument("time", IntegerArgumentType.integer(0)).executes(context -> {
                            WorldBorder border = border(context);
                            int time = IntegerArgumentType.getInteger(context, "time");
                            if (border.getWarningTime() == time) throw BORDER_WARNING_TIME_UNCHANGED.create();
                            border.setWarningTime(time);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.worldborder.warning.time.success", time), true);
                            return time;
                        })))));
    }

    private static WorldBorder border(CommandContext<CommandSource> context) {
        return context.getSource().getWorld().getWorldBorder();
    }

    private static int setBorderSize(CommandSource source, double size, long time) throws CommandSyntaxException {
        WorldBorder border = source.getWorld().getWorldBorder();
        double current = border.getDiameter();
        if (current == size) throw BORDER_SIZE_UNCHANGED.create();
        if (size < 1.0D) throw BORDER_TOO_SMALL.create();
        if (size > 6.0E7D) throw BORDER_TOO_BIG.create();
        String formatted = String.format(Locale.ROOT, "%.1f", size);
        if (time > 0L) {
            border.setTransition(current, size, time);
            source.sendFeedback(new TextComponentTranslation(size > current ? "commands.worldborder.set.grow" : "commands.worldborder.set.shrink",
                    formatted, Long.toString(time / 1000L)), true);
        } else {
            border.setTransition(size);
            source.sendFeedback(new TextComponentTranslation("commands.worldborder.set.immediate", formatted), true);
        }
        return (int) (size - current);
    }

    private WorldCommands() {}
}
