package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.ComponentResolver;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.ComponentArgument;
import com.michaelsebero.portium.command.arguments.ScoreboardArguments;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code scoreboard objectives|players} (1.13). */
public final class ScoreboardCommand {

    private static final CommandExceptionType.Simple OBJECTIVE_EXISTS = simple("commands.scoreboard.objectives.add.duplicate");
    private static final CommandExceptionType.Simple DISPLAY_ALREADY_EMPTY = simple("commands.scoreboard.objectives.display.alreadyEmpty");
    private static final CommandExceptionType.Simple DISPLAY_ALREADY_SET = simple("commands.scoreboard.objectives.display.alreadySet");
    private static final CommandExceptionType.Simple ENABLE_FAILED = simple("commands.scoreboard.players.enable.failed");
    private static final CommandExceptionType.Simple ENABLE_INVALID = simple("commands.scoreboard.players.enable.invalid");
    private static final CommandExceptionType.Dynamic2 NO_VALUE = new CommandExceptionType.Dynamic2((objective, holder) -> TextMessage.translate("commands.scoreboard.players.get.null", objective, holder));
    private static final CommandExceptionType.Dynamic NAME_TOO_LONG = new CommandExceptionType.Dynamic(max -> TextMessage.translate("commands.scoreboard.objectives.add.longName", max));

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("scoreboard").requires(source -> source.hasPermissionLevel(2))
                .then(literal("objectives")
                        .then(literal("list").executes(context -> listObjectives(context.getSource())))
                        .then(literal("add").then(argument("objective", StringArgumentType.word()).then(argument("criteria", ScoreboardArguments.criteria())
                                .executes(context -> addObjective(context, null))
                                .then(argument("displayName", ComponentArgument.component()).executes(context ->
                                        addObjective(context, ComponentArgument.getComponent(context, "displayName")))))))
                        .then(literal("modify").then(argument("objective", ScoreboardArguments.objective())
                                .then(literal("displayname").then(argument("displayName", ComponentArgument.component()).executes(context -> {
                                    ScoreObjective objective = ScoreboardArguments.getObjective(context, "objective");
                                    ITextComponent name = ComponentResolver.resolve(context.getSource(), ComponentArgument.getComponent(context, "displayName"), null);
                                    objective.setDisplayName(name.getFormattedText());
                                    context.getSource().sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.modify.displayname", objective.getName(), name), true);
                                    return 0;
                                })))
                                .then(literal("rendertype").then(renderType("integer", IScoreCriteria.EnumRenderType.INTEGER)).then(renderType("hearts", IScoreCriteria.EnumRenderType.HEARTS)))))
                        .then(literal("remove").then(argument("objective", ScoreboardArguments.objective()).executes(context -> {
                            ScoreObjective objective = ScoreboardArguments.getObjective(context, "objective");
                            ScoreboardArguments.scoreboard(context.getSource()).removeObjective(objective);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.remove.success", describe(objective)), true);
                            return ScoreboardArguments.scoreboard(context.getSource()).getScoreObjectives().size();
                        })))
                        .then(literal("setdisplay").then(argument("slot", ScoreboardArguments.displaySlot())
                                .executes(context -> clearDisplay(context.getSource(), context.getArgument("slot", Integer.class)))
                                .then(argument("objective", ScoreboardArguments.objective()).executes(context ->
                                        setDisplay(context.getSource(), context.getArgument("slot", Integer.class), ScoreboardArguments.getObjective(context, "objective")))))))
                .then(literal("players")
                        .then(literal("list").executes(context -> listHolders(context.getSource()))
                                .then(argument("target", ScoreboardArguments.scoreHolder(false)).executes(context ->
                                        listScores(context.getSource(), ScoreboardArguments.getScoreHolder(context, "target")))))
                        .then(literal("set").then(argument("targets", ScoreboardArguments.scoreHolder(true)).then(argument("objective", ScoreboardArguments.objective())
                                .then(argument("score", IntegerArgumentType.integer()).executes(context -> setScore(context.getSource(), ScoreboardArguments.getScoreHolders(context, "targets"),
                                        ScoreboardArguments.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score")))))))
                        .then(literal("get").then(argument("target", ScoreboardArguments.scoreHolder(false)).then(argument("objective", ScoreboardArguments.objective())
                                .executes(context -> getScore(context.getSource(), ScoreboardArguments.getScoreHolder(context, "target"), ScoreboardArguments.getObjective(context, "objective"))))))
                        .then(literal("add").then(argument("targets", ScoreboardArguments.scoreHolder(true)).then(argument("objective", ScoreboardArguments.objective())
                                .then(argument("score", IntegerArgumentType.integer(0)).executes(context -> addScore(context.getSource(), ScoreboardArguments.getScoreHolders(context, "targets"),
                                        ScoreboardArguments.getWritableObjective(context, "objective"), IntegerArgumentType.getInteger(context, "score")))))))
                        .then(literal("remove").then(argument("targets", ScoreboardArguments.scoreHolder(true)).then(argument("objective", ScoreboardArguments.objective())
                                .then(argument("score", IntegerArgumentType.integer(0)).executes(context -> addScore(context.getSource(), ScoreboardArguments.getScoreHolders(context, "targets"),
                                        ScoreboardArguments.getWritableObjective(context, "objective"), -IntegerArgumentType.getInteger(context, "score")))))))
                        .then(literal("reset").then(argument("targets", ScoreboardArguments.scoreHolder(true))
                                .executes(context -> reset(context.getSource(), ScoreboardArguments.getScoreHolders(context, "targets"), null))
                                .then(argument("objective", ScoreboardArguments.objective()).executes(context ->
                                        reset(context.getSource(), ScoreboardArguments.getScoreHolders(context, "targets"), ScoreboardArguments.getObjective(context, "objective"))))))
                        .then(literal("enable").then(argument("targets", ScoreboardArguments.scoreHolder(true)).then(argument("objective", ScoreboardArguments.objective())
                                .executes(context -> enable(context.getSource(), ScoreboardArguments.getScoreHolders(context, "targets"), ScoreboardArguments.getObjective(context, "objective"))))))
                        .then(literal("operation").then(argument("targets", ScoreboardArguments.scoreHolder(true)).then(argument("targetObjective", ScoreboardArguments.objective())
                                .then(argument("operation", ScoreboardArguments.operation()).then(argument("source", ScoreboardArguments.scoreHolder(true))
                                        .then(argument("sourceObjective", ScoreboardArguments.objective()).executes(ScoreboardCommand::operation)))))))));
    }

    private static com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder<CommandSource> renderType(String name, IScoreCriteria.EnumRenderType type) {
        return literal(name).executes(context -> {
            ScoreObjective objective = ScoreboardArguments.getObjective(context, "objective");
            if (objective.getRenderType() != type) {
                objective.setRenderType(type);
                ScoreboardArguments.scoreboard(context.getSource()).onObjectiveDisplayNameChanged(objective);
            }
            context.getSource().sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.modify.rendertype", objective.getName()), true);
            return 0;
        });
    }

    /** {@code [displayName]}, hovering to show the objective's id. */
    static ITextComponent describe(ScoreObjective objective) {
        ITextComponent text = new TextComponentString("[" + objective.getDisplayName() + "]");
        text.getStyle().setHoverEvent(new net.minecraft.util.text.event.HoverEvent(net.minecraft.util.text.event.HoverEvent.Action.SHOW_TEXT,
                new TextComponentString(objective.getName())));
        return text;
    }

    private static int addObjective(CommandContext<CommandSource> context, ITextComponent displayName) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "objective");
        IScoreCriteria criteria = context.getArgument("criteria", IScoreCriteria.class);
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(context.getSource());
        if (scoreboard.getObjective(name) != null) throw OBJECTIVE_EXISTS.create();
        if (name.length() > 16) throw NAME_TOO_LONG.create(16);
        ScoreObjective objective = scoreboard.addScoreObjective(name, criteria);
        if (displayName != null) objective.setDisplayName(ComponentResolver.resolve(context.getSource(), displayName, null).getFormattedText());
        context.getSource().sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.add.success", describe(objective)), true);
        return scoreboard.getScoreObjectives().size();
    }

    private static int listObjectives(CommandSource source) {
        Collection<ScoreObjective> objectives = ScoreboardArguments.scoreboard(source).getScoreObjectives();
        if (objectives.isEmpty()) {
            source.sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.list.empty"), false);
        } else {
            ITextComponent list = new TextComponentString("");
            int i = 0;
            for (ScoreObjective objective : objectives) {
                if (i++ > 0) list.appendText(", ");
                list.appendSibling(describe(objective));
            }
            source.sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.list.success", objectives.size(), list), false);
        }
        return objectives.size();
    }

    private static int clearDisplay(CommandSource source, int slot) throws CommandSyntaxException {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        if (scoreboard.getObjectiveInDisplaySlot(slot) == null) throw DISPLAY_ALREADY_EMPTY.create();
        scoreboard.setObjectiveInDisplaySlot(slot, null);
        source.sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.display.cleared", Scoreboard.getObjectiveDisplaySlot(slot)), true);
        return 0;
    }

    private static int setDisplay(CommandSource source, int slot, ScoreObjective objective) throws CommandSyntaxException {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        if (scoreboard.getObjectiveInDisplaySlot(slot) == objective) throw DISPLAY_ALREADY_SET.create();
        scoreboard.setObjectiveInDisplaySlot(slot, objective);
        source.sendFeedback(new TextComponentTranslation("commands.scoreboard.objectives.display.set", Scoreboard.getObjectiveDisplaySlot(slot), describe(objective)), true);
        return 0;
    }

    private static void feedback(CommandSource source, Collection<String> targets, String key, Object... single) {
        Object[] args = single.clone();
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation(key + ".single", withFirst(args, targets.iterator().next())), true);
        } else {
            source.sendFeedback(new TextComponentTranslation(key + ".multiple", withFirst(args, targets.size())), true);
        }
    }

    /** The message arguments with the target (or target count) put where the {@code TARGET} marker is. */
    private static Object[] withFirst(Object[] args, Object target) {
        Object[] result = args.clone();
        for (int i = 0; i < result.length; i++) {
            if (result[i] == TARGET) result[i] = target;
        }
        return result;
    }

    private static final Object TARGET = new Object();

    private static int setScore(CommandSource source, Collection<String> targets, ScoreObjective objective, int value) {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        for (String target : targets) scoreboard.getOrCreateScore(target, objective).setScorePoints(value);
        feedback(source, targets, "commands.scoreboard.players.set.success", describe(objective), TARGET, value);
        return value * targets.size();
    }

    private static int getScore(CommandSource source, String target, ScoreObjective objective) throws CommandSyntaxException {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        if (!scoreboard.entityHasObjective(target, objective)) throw NO_VALUE.create(objective.getName(), target);
        int value = scoreboard.getOrCreateScore(target, objective).getScorePoints();
        source.sendFeedback(new TextComponentTranslation("commands.scoreboard.players.get.success", target, value, describe(objective)), false);
        return value;
    }

    private static int addScore(CommandSource source, Collection<String> targets, ScoreObjective objective, int amount) {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        int total = 0;
        for (String target : targets) {
            Score score = scoreboard.getOrCreateScore(target, objective);
            score.setScorePoints(score.getScorePoints() + amount);
            total += score.getScorePoints();
        }
        String key = amount >= 0 ? "commands.scoreboard.players.add.success" : "commands.scoreboard.players.remove.success";
        if (targets.size() == 1) {
            source.sendFeedback(new TextComponentTranslation(key + ".single", Math.abs(amount), describe(objective), targets.iterator().next(), total), true);
        } else {
            source.sendFeedback(new TextComponentTranslation(key + ".multiple", Math.abs(amount), describe(objective), targets.size()), true);
        }
        return total;
    }

    private static int reset(CommandSource source, Collection<String> targets, ScoreObjective objective) {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        for (String target : targets) scoreboard.removeObjectiveFromEntity(target, objective);
        if (objective == null) feedback(source, targets, "commands.scoreboard.players.reset.all", TARGET);
        else feedback(source, targets, "commands.scoreboard.players.reset.specific", describe(objective), TARGET);
        return targets.size();
    }

    /** Lets players use {@code /trigger} on a trigger objective once. */
    private static int enable(CommandSource source, Collection<String> targets, ScoreObjective objective) throws CommandSyntaxException {
        if (objective.getCriteria() != IScoreCriteria.TRIGGER) throw ENABLE_INVALID.create();
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        int count = 0;
        for (String target : targets) {
            Score score = scoreboard.getOrCreateScore(target, objective);
            if (score.isLocked()) {
                score.setLocked(false);
                count++;
            }
        }
        if (count == 0) throw ENABLE_FAILED.create();
        feedback(source, targets, "commands.scoreboard.players.enable.success", describe(objective), TARGET);
        return count;
    }

    private static int operation(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<String> targets = ScoreboardArguments.getScoreHolders(context, "targets");
        ScoreObjective targetObjective = ScoreboardArguments.getWritableObjective(context, "targetObjective");
        ScoreboardArguments.Operation operation = context.getArgument("operation", ScoreboardArguments.Operation.class);
        Collection<String> sources = ScoreboardArguments.getScoreHolders(context, "source");
        ScoreObjective sourceObjective = ScoreboardArguments.getObjective(context, "sourceObjective");
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(context.getSource());
        int total = 0;
        for (String target : targets) {
            Score score = scoreboard.getOrCreateScore(target, targetObjective);
            for (String sourceName : sources) operation.apply(score, scoreboard.getOrCreateScore(sourceName, sourceObjective));
            total += score.getScorePoints();
        }
        if (targets.size() == 1) {
            context.getSource().sendFeedback(new TextComponentTranslation("commands.scoreboard.players.operation.success.single", describe(targetObjective),
                    targets.iterator().next(), total), true);
        } else {
            context.getSource().sendFeedback(new TextComponentTranslation("commands.scoreboard.players.operation.success.multiple", describe(targetObjective), targets.size()), true);
        }
        return total;
    }

    private static int listHolders(CommandSource source) {
        Collection<String> holders = ScoreboardArguments.scoreboard(source).getObjectiveNames();
        if (holders.isEmpty()) source.sendFeedback(new TextComponentTranslation("commands.scoreboard.players.list.empty"), false);
        else source.sendFeedback(new TextComponentTranslation("commands.scoreboard.players.list.success", holders.size(), String.join(", ", holders)), false);
        return holders.size();
    }

    private static int listScores(CommandSource source, String holder) {
        Map<ScoreObjective, Score> scores = ScoreboardArguments.scoreboard(source).getObjectivesForEntity(holder);
        if (scores.isEmpty()) {
            source.sendFeedback(new TextComponentTranslation("commands.scoreboard.players.list.entity.empty", holder), false);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.scoreboard.players.list.entity.success", holder, scores.size()), false);
            List<ScoreObjective> objectives = new ArrayList<>(scores.keySet());
            for (ScoreObjective objective : objectives) {
                ITextComponent line = new TextComponentTranslation("commands.scoreboard.players.list.entity.entry", describe(objective), scores.get(objective).getScorePoints());
                source.sendFeedback(line, false);
            }
        }
        return scores.size();
    }

    private ScoreboardCommand() {}
}
