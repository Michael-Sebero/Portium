package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.ComponentResolver;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.ComponentArgument;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.MiscArguments;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType;
import com.michaelsebero.portium.command.brigadier.tree.LiteralCommandNode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketTitle;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.Collection;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code say}, {@code me}, {@code msg}/{@code tell}/{@code w}, {@code teammsg}/{@code tm}, {@code tellraw} and {@code title} (1.13). */
public final class ChatCommands {

    private static final CommandExceptionType.Simple NO_TEAM = new CommandExceptionType.Simple(TextMessage.translate("commands.teammsg.failed.noteam"));

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("say").requires(source -> source.hasPermissionLevel(2)).then(argument("message", MiscArguments.message()).executes(context -> {
            ITextComponent message = MiscArguments.getMessage(context, "message");
            context.getSource().getServer().getPlayerList().sendMessage(new TextComponentTranslation("chat.type.announcement", context.getSource().getDisplayName(), message));
            return 1;
        })));
        dispatcher.register(literal("me").then(argument("action", com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType.greedyString()).executes(context -> {
            String action = com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType.getString(context, "action");
            context.getSource().getServer().getPlayerList().sendMessage(new TextComponentTranslation("chat.type.emote", context.getSource().getDisplayName(), action));
            return 1;
        })));
        LiteralCommandNode<CommandSource> msg = dispatcher.register(literal("msg").then(argument("targets", EntityArgument.players())
                .then(argument("message", MiscArguments.message()).executes(context -> {
                    Collection<EntityPlayerMP> targets = EntityArgument.getPlayers(context, "targets");
                    ITextComponent message = MiscArguments.getMessage(context, "message");
                    for (EntityPlayerMP target : targets) {
                        ITextComponent outgoing = new TextComponentTranslation("commands.message.display.outgoing", target.getDisplayName(), message.createCopy());
                        outgoing.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
                        context.getSource().sendFeedback(outgoing, false);
                        ITextComponent incoming = new TextComponentTranslation("commands.message.display.incoming", context.getSource().getDisplayName(), message.createCopy());
                        incoming.getStyle().setColor(TextFormatting.GRAY).setItalic(true);
                        target.sendMessage(incoming);
                    }
                    return targets.size();
                }))));
        dispatcher.register(literal("tell").redirect(msg));
        dispatcher.register(literal("w").redirect(msg));
        LiteralCommandNode<CommandSource> teammsg = dispatcher.register(literal("teammsg").then(argument("message", MiscArguments.message()).executes(context -> {
            CommandSource source = context.getSource();
            Entity entity = source.assertIsEntity();
            Team team = entity.getTeam();
            if (!(team instanceof ScorePlayerTeam)) throw NO_TEAM.create();
            ScorePlayerTeam playerTeam = (ScorePlayerTeam) team;
            ITextComponent teamName = new TextComponentString(playerTeam.getDisplayName());
            teamName.getStyle().setColor(playerTeam.getColor()).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(playerTeam.getName())))
                    .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/teammsg "));
            ITextComponent message = MiscArguments.getMessage(context, "message");
            int count = 0;
            for (EntityPlayerMP player : source.getServer().getPlayerList().getPlayers()) {
                if (player.getTeam() != team) continue;
                if (player == entity) player.sendMessage(new TextComponentTranslation("chat.type.team.sent", teamName, source.getDisplayName(), message.createCopy()));
                else player.sendMessage(new TextComponentTranslation("chat.type.team.text", teamName, source.getDisplayName(), message.createCopy()));
                count++;
            }
            return count;
        })));
        dispatcher.register(literal("tm").redirect(teammsg));
        dispatcher.register(literal("tellraw").requires(source -> source.hasPermissionLevel(2)).then(argument("targets", EntityArgument.players())
                .then(argument("message", ComponentArgument.component()).executes(context -> {
                    int count = 0;
                    for (EntityPlayerMP player : EntityArgument.getPlayers(context, "targets")) {
                        player.sendMessage(ComponentResolver.resolve(context.getSource(), ComponentArgument.getComponent(context, "message"), player));
                        count++;
                    }
                    return count;
                }))));
        dispatcher.register(literal("title").requires(source -> source.hasPermissionLevel(2)).then(argument("targets", EntityArgument.players())
                .then(literal("clear").executes(context -> clear(context.getSource(), EntityArgument.getPlayers(context, "targets"))))
                .then(literal("reset").executes(context -> reset(context.getSource(), EntityArgument.getPlayers(context, "targets"))))
                .then(literal("title").then(argument("title", ComponentArgument.component()).executes(context ->
                        show(context.getSource(), EntityArgument.getPlayers(context, "targets"), ComponentArgument.getComponent(context, "title"), SPacketTitle.Type.TITLE, "title"))))
                .then(literal("subtitle").then(argument("title", ComponentArgument.component()).executes(context ->
                        show(context.getSource(), EntityArgument.getPlayers(context, "targets"), ComponentArgument.getComponent(context, "title"), SPacketTitle.Type.SUBTITLE, "subtitle"))))
                .then(literal("actionbar").then(argument("title", ComponentArgument.component()).executes(context ->
                        show(context.getSource(), EntityArgument.getPlayers(context, "targets"), ComponentArgument.getComponent(context, "title"), SPacketTitle.Type.ACTIONBAR, "actionbar"))))
                .then(literal("times").then(argument("fadeIn", IntegerArgumentType.integer(0)).then(argument("stay", IntegerArgumentType.integer(0))
                        .then(argument("fadeOut", IntegerArgumentType.integer(0)).executes(context -> times(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                IntegerArgumentType.getInteger(context, "fadeIn"), IntegerArgumentType.getInteger(context, "stay"), IntegerArgumentType.getInteger(context, "fadeOut")))))))));
    }

    private static void feedback(CommandSource source, Collection<EntityPlayerMP> targets, String key) {
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation(key + ".single", targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation(key + ".multiple", targets.size()), true);
    }

    private static int clear(CommandSource source, Collection<EntityPlayerMP> targets) {
        SPacketTitle packet = new SPacketTitle(SPacketTitle.Type.CLEAR, null);
        for (EntityPlayerMP player : targets) player.connection.sendPacket(packet);
        feedback(source, targets, "commands.title.cleared");
        return targets.size();
    }

    private static int reset(CommandSource source, Collection<EntityPlayerMP> targets) {
        SPacketTitle packet = new SPacketTitle(SPacketTitle.Type.RESET, null);
        for (EntityPlayerMP player : targets) player.connection.sendPacket(packet);
        feedback(source, targets, "commands.title.reset");
        return targets.size();
    }

    private static int show(CommandSource source, Collection<EntityPlayerMP> targets, ITextComponent title, SPacketTitle.Type type, String name)
            throws com.michaelsebero.portium.command.brigadier.CommandSyntaxException {
        for (EntityPlayerMP player : targets) player.connection.sendPacket(new SPacketTitle(type, ComponentResolver.resolve(source, title, player)));
        feedback(source, targets, "commands.title.show." + name);
        return targets.size();
    }

    private static int times(CommandSource source, Collection<EntityPlayerMP> targets, int fadeIn, int stay, int fadeOut) {
        SPacketTitle packet = new SPacketTitle(fadeIn, stay, fadeOut);
        for (EntityPlayerMP player : targets) player.connection.sendPacket(packet);
        feedback(source, targets, "commands.title.times");
        return targets.size();
    }

    private ChatCommands() {}
}
