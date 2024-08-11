package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.ComponentResolver;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.ComponentArgument;
import com.michaelsebero.portium.command.arguments.ScoreboardArguments;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.BoolArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code team} and {@code trigger} (1.13). */
public final class TeamCommand {

    private static final CommandExceptionType.Simple TEAM_EXISTS = simple("commands.team.add.duplicate");
    private static final CommandExceptionType.Dynamic NAME_TOO_LONG = new CommandExceptionType.Dynamic(max -> TextMessage.translate("commands.team.add.longName", max));
    private static final CommandExceptionType.Simple EMPTY_UNCHANGED = simple("commands.team.empty.unchanged");
    private static final CommandExceptionType.Simple OPTION_UNCHANGED = simple("commands.team.option.unchanged");
    private static final CommandExceptionType.Simple TRIGGER_UNPRIMED = simple("commands.trigger.failed.unprimed");
    private static final CommandExceptionType.Simple TRIGGER_INVALID = simple("commands.trigger.failed.invalid");

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("team").requires(source -> source.hasPermissionLevel(2))
                .then(literal("list").executes(context -> listTeams(context.getSource()))
                        .then(argument("team", ScoreboardArguments.team()).executes(context -> listMembers(context.getSource(), ScoreboardArguments.getTeam(context, "team")))))
                .then(literal("add").then(argument("team", StringArgumentType.word())
                        .executes(context -> addTeam(context, null))
                        .then(argument("displayName", ComponentArgument.component()).executes(context -> addTeam(context, ComponentArgument.getComponent(context, "displayName"))))))
                .then(literal("remove").then(argument("team", ScoreboardArguments.team()).executes(context -> {
                    ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
                    ScoreboardArguments.scoreboard(context.getSource()).removeTeam(team);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.team.remove.success", describe(team)), true);
                    return ScoreboardArguments.scoreboard(context.getSource()).getTeams().size();
                })))
                .then(literal("empty").then(argument("team", ScoreboardArguments.team()).executes(context -> {
                    ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
                    Collection<String> members = new java.util.ArrayList<>(team.getMembershipCollection());
                    if (members.isEmpty()) throw EMPTY_UNCHANGED.create();
                    for (String member : members) ScoreboardArguments.scoreboard(context.getSource()).removePlayerFromTeam(member, team);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.team.empty.success", members.size(), describe(team)), true);
                    return members.size();
                })))
                .then(literal("join").then(argument("team", ScoreboardArguments.team())
                        .executes(context -> join(context.getSource(), ScoreboardArguments.getTeam(context, "team"),
                                java.util.Collections.singleton(com.michaelsebero.portium.command.ComponentResolver.scoreHolder(context.getSource().assertIsEntity()))))
                        .then(argument("members", ScoreboardArguments.scoreHolder(true)).executes(context ->
                                join(context.getSource(), ScoreboardArguments.getTeam(context, "team"), ScoreboardArguments.getScoreHolders(context, "members"))))))
                .then(literal("leave").then(argument("members", ScoreboardArguments.scoreHolder(true)).executes(context -> {
                    Collection<String> members = ScoreboardArguments.getScoreHolders(context, "members");
                    Scoreboard scoreboard = ScoreboardArguments.scoreboard(context.getSource());
                    for (String member : members) scoreboard.removePlayerFromTeams(member);
                    if (members.size() == 1) context.getSource().sendFeedback(new TextComponentTranslation("commands.team.leave.success.single", members.iterator().next()), true);
                    else context.getSource().sendFeedback(new TextComponentTranslation("commands.team.leave.success.multiple", members.size()), true);
                    return members.size();
                })))
                .then(literal("modify").then(argument("team", ScoreboardArguments.team())
                        .then(literal("displayName").then(argument("displayName", ComponentArgument.component()).executes(context -> {
                            ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
                            String name = ComponentResolver.resolve(context.getSource(), ComponentArgument.getComponent(context, "displayName"), null).getFormattedText();
                            if (team.getDisplayName().equals(name)) throw OPTION_UNCHANGED.create();
                            team.setDisplayName(name);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.team.option.name.success", describe(team)), true);
                            return 0;
                        })))
                        .then(literal("color").then(argument("value", ScoreboardArguments.color()).executes(context -> {
                            ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
                            TextFormatting color = context.getArgument("value", TextFormatting.class);
                            team.setColor(color);
                            team.setPrefix(color.toString());
                            team.setSuffix(TextFormatting.RESET.toString());
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.team.option.color.success", describe(team), color.getFriendlyName()), true);
                            return 0;
                        })))
                        .then(literal("friendlyFire").then(argument("allowed", BoolArgumentType.bool()).executes(context ->
                                option(context, "friendlyfire", ScorePlayerTeam::getAllowFriendlyFire, ScorePlayerTeam::setAllowFriendlyFire, BoolArgumentType.getBool(context, "allowed")))))
                        .then(literal("seeFriendlyInvisibles").then(argument("allowed", BoolArgumentType.bool()).executes(context ->
                                option(context, "seeFriendlyInvisibles", ScorePlayerTeam::getSeeFriendlyInvisiblesEnabled, ScorePlayerTeam::setSeeFriendlyInvisiblesEnabled,
                                        BoolArgumentType.getBool(context, "allowed")))))
                        .then(visibility("nametagVisibility", "nametagVisibility", ScorePlayerTeam::getNameTagVisibility, ScorePlayerTeam::setNameTagVisibility))
                        .then(visibility("deathMessageVisibility", "deathMessageVisibility", ScorePlayerTeam::getDeathMessageVisibility, ScorePlayerTeam::setDeathMessageVisibility))
                        .then(literal("collisionRule").then(enumOption("never", Team.CollisionRule.NEVER)).then(enumOption("always", Team.CollisionRule.ALWAYS))
                                .then(enumOption("pushOwnTeam", Team.CollisionRule.HIDE_FOR_OWN_TEAM)).then(enumOption("pushOtherTeams", Team.CollisionRule.HIDE_FOR_OTHER_TEAMS)))
                        .then(literal("prefix").then(argument("prefix", ComponentArgument.component()).executes(context -> {
                            ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
                            team.setPrefix(ComponentResolver.resolve(context.getSource(), ComponentArgument.getComponent(context, "prefix"), null).getFormattedText());
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.team.option.prefix.success", team.getPrefix()), false);
                            return 1;
                        })))
                        .then(literal("suffix").then(argument("suffix", ComponentArgument.component()).executes(context -> {
                            ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
                            team.setSuffix(ComponentResolver.resolve(context.getSource(), ComponentArgument.getComponent(context, "suffix"), null).getFormattedText());
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.team.option.suffix.success", team.getSuffix()), false);
                            return 1;
                        }))))));

        dispatcher.register(literal("trigger").then(argument("objective", ScoreboardArguments.objective())
                .executes(context -> trigger(context, score -> score + 1, "commands.trigger.simple.success", 0))
                .then(literal("add").then(argument("value", com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType.integer()).executes(context -> {
                    int value = com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType.getInteger(context, "value");
                    return trigger(context, score -> score + value, "commands.trigger.add.success", value);
                })))
                .then(literal("set").then(argument("value", com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType.integer()).executes(context -> {
                    int value = com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType.getInteger(context, "value");
                    return trigger(context, score -> value, "commands.trigger.set.success", value);
                })))));
    }

    private static ITextComponent describe(ScorePlayerTeam team) {
        ITextComponent text = new TextComponentString("[" + team.getDisplayName() + "]");
        text.getStyle().setColor(team.getColor()).setHoverEvent(new net.minecraft.util.text.event.HoverEvent(net.minecraft.util.text.event.HoverEvent.Action.SHOW_TEXT,
                new TextComponentString(team.getName())));
        return text;
    }

    private static int addTeam(CommandContext<CommandSource> context, ITextComponent displayName) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "team");
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(context.getSource());
        if (scoreboard.getTeam(name) != null) throw TEAM_EXISTS.create();
        if (name.length() > 16) throw NAME_TOO_LONG.create(16);
        ScorePlayerTeam team = scoreboard.createTeam(name);
        if (displayName != null) team.setDisplayName(ComponentResolver.resolve(context.getSource(), displayName, null).getFormattedText());
        context.getSource().sendFeedback(new TextComponentTranslation("commands.team.add.success", describe(team)), true);
        return scoreboard.getTeams().size();
    }

    private static int join(CommandSource source, ScorePlayerTeam team, Collection<String> members) {
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        for (String member : members) scoreboard.addPlayerToTeam(member, team.getName());
        if (members.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.team.join.success.single", members.iterator().next(), describe(team)), true);
        else source.sendFeedback(new TextComponentTranslation("commands.team.join.success.multiple", members.size(), describe(team)), true);
        return members.size();
    }

    private static int option(CommandContext<CommandSource> context, String key, Function<ScorePlayerTeam, Boolean> getter, BiConsumer<ScorePlayerTeam, Boolean> setter,
                              boolean value) throws CommandSyntaxException {
        ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
        if (getter.apply(team) == value) throw OPTION_UNCHANGED.create();
        setter.accept(team, value);
        context.getSource().sendFeedback(new TextComponentTranslation("commands.team.option." + key + "." + (value ? "enabled" : "disabled"), describe(team)), true);
        return 0;
    }

    private static LiteralArgumentBuilder<CommandSource> visibility(String literalName, String key, Function<ScorePlayerTeam, Team.EnumVisible> getter,
                                                                    BiConsumer<ScorePlayerTeam, Team.EnumVisible> setter) {
        LiteralArgumentBuilder<CommandSource> builder = literal(literalName);
        String[][] values = {{"never", "NEVER"}, {"hideForOtherTeams", "HIDE_FOR_OTHER_TEAMS"}, {"hideForOwnTeam", "HIDE_FOR_OWN_TEAM"}, {"always", "ALWAYS"}};
        for (String[] value : values) {
            Team.EnumVisible visible = Team.EnumVisible.valueOf(value[1]);
            builder.then(literal(value[0]).executes(context -> {
                ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
                if (getter.apply(team) == visible) throw OPTION_UNCHANGED.create();
                setter.accept(team, visible);
                context.getSource().sendFeedback(new TextComponentTranslation("commands.team.option." + key + ".success", describe(team), value[0]), true);
                return 0;
            }));
        }
        return builder;
    }

    private static LiteralArgumentBuilder<CommandSource> enumOption(String name, Team.CollisionRule rule) {
        return literal(name).executes(context -> {
            ScorePlayerTeam team = ScoreboardArguments.getTeam(context, "team");
            if (team.getCollisionRule() == rule) throw OPTION_UNCHANGED.create();
            team.setCollisionRule(rule);
            context.getSource().sendFeedback(new TextComponentTranslation("commands.team.option.collisionRule.success", describe(team), name), true);
            return 0;
        });
    }

    private static int listTeams(CommandSource source) {
        Collection<ScorePlayerTeam> teams = ScoreboardArguments.scoreboard(source).getTeams();
        if (teams.isEmpty()) {
            source.sendFeedback(new TextComponentTranslation("commands.team.list.teams.empty"), false);
        } else {
            ITextComponent list = new TextComponentString("");
            int i = 0;
            for (ScorePlayerTeam team : teams) {
                if (i++ > 0) list.appendText(", ");
                list.appendSibling(describe(team));
            }
            source.sendFeedback(new TextComponentTranslation("commands.team.list.teams.success", teams.size(), list), false);
        }
        return teams.size();
    }

    private static int listMembers(CommandSource source, ScorePlayerTeam team) {
        Collection<String> members = team.getMembershipCollection();
        if (members.isEmpty()) source.sendFeedback(new TextComponentTranslation("commands.team.list.members.empty", describe(team)), false);
        else source.sendFeedback(new TextComponentTranslation("commands.team.list.members.success", describe(team), members.size(), String.join(", ", members)), false);
        return members.size();
    }

    private interface ScoreChange {
        int apply(int score);
    }

    /** {@code trigger}: players change their own trigger score once each time an operator enables it. */
    private static int trigger(CommandContext<CommandSource> context, ScoreChange change, String key, int value) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        ScoreObjective objective = ScoreboardArguments.getObjective(context, "objective");
        if (objective.getCriteria() != IScoreCriteria.TRIGGER) throw TRIGGER_INVALID.create();
        String holder = ComponentResolver.scoreHolder(source.asPlayer());
        Scoreboard scoreboard = ScoreboardArguments.scoreboard(source);
        if (!scoreboard.entityHasObjective(holder, objective)) throw TRIGGER_UNPRIMED.create();
        Score score = scoreboard.getOrCreateScore(holder, objective);
        if (score.isLocked()) throw TRIGGER_UNPRIMED.create();
        score.setLocked(true);
        score.setScorePoints(change.apply(score.getScorePoints()));
        source.sendFeedback(new TextComponentTranslation(key, ScoreboardCommand.describe(objective), value), true);
        return score.getScorePoints();
    }

    private TeamCommand() {}
}
