package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.GameProfileArgument;
import com.michaelsebero.portium.command.arguments.IdSuggestions;
import com.michaelsebero.portium.command.arguments.MiscArguments;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import com.michaelsebero.portium.mixin.accessor.UserListEntryBanAccessor;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.server.management.UserListEntryBan;
import net.minecraft.server.management.UserListIPBans;
import net.minecraft.server.management.UserListIPBansEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/**
 * Server administration (1.13): {@code kick}, {@code ban}, {@code ban-ip}, {@code banlist}, {@code pardon},
 * {@code pardon-ip}, {@code op}, {@code deop}, {@code whitelist}, {@code save-all}, {@code save-on},
 * {@code save-off} and {@code stop}. Players may be named even when offline, or picked with selectors. All but
 * {@code kick} are dedicated server commands.
 */
public final class AdminCommands {

    private static final Pattern IP_ADDRESS = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static final SuggestionProvider<CommandSource> NOT_OPS = (context, builder) ->
            IdSuggestions.suggest(onlineNames(context.getSource(), profile -> !players(context.getSource()).canSendCommands(profile)), builder);
    private static final SuggestionProvider<CommandSource> OPS = (context, builder) ->
            IdSuggestions.suggest(Arrays.asList(players(context.getSource()).getOppedPlayerNames()), builder);
    private static final SuggestionProvider<CommandSource> BANNED_PLAYERS = (context, builder) ->
            IdSuggestions.suggest(Arrays.asList(players(context.getSource()).getBannedPlayers().getKeys()), builder);
    private static final SuggestionProvider<CommandSource> BANNED_IPS = (context, builder) ->
            IdSuggestions.suggest(Arrays.asList(players(context.getSource()).getBannedIPs().getKeys()), builder);
    private static final SuggestionProvider<CommandSource> NOT_WHITELISTED = (context, builder) ->
            IdSuggestions.suggest(onlineNames(context.getSource(), profile -> players(context.getSource()).getWhitelistedPlayers().getEntry(profile) == null), builder);
    private static final SuggestionProvider<CommandSource> WHITELISTED = (context, builder) ->
            IdSuggestions.suggest(Arrays.asList(players(context.getSource()).getWhitelistedPlayerNames()), builder);

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("kick").requires(source -> source.hasPermissionLevel(3)).then(argument("targets", EntityArgument.players())
                .executes(context -> kick(context.getSource(), EntityArgument.getPlayers(context, "targets"), new TextComponentTranslation("multiplayer.disconnect.kicked")))
                .then(argument("reason", MiscArguments.message()).executes(context ->
                        kick(context.getSource(), EntityArgument.getPlayers(context, "targets"), MiscArguments.getMessage(context, "reason"))))));

        // The rest only exist on dedicated servers, as in 1.13.
        if (FMLCommonHandler.instance().getSide() != Side.SERVER) return;
        dispatcher.register(literal("ban").requires(source -> players(source).getBannedPlayers().isLanServer() && source.hasPermissionLevel(3))
                .then(argument("targets", GameProfileArgument.gameProfile())
                        .executes(context -> ban(context.getSource(), GameProfileArgument.getGameProfiles(context, "targets"), null))
                        .then(argument("reason", MiscArguments.message()).executes(context ->
                                ban(context.getSource(), GameProfileArgument.getGameProfiles(context, "targets"), MiscArguments.getMessage(context, "reason"))))));
        dispatcher.register(literal("ban-ip").requires(source -> players(source).getBannedIPs().isLanServer() && source.hasPermissionLevel(3))
                .then(argument("target", StringArgumentType.word())
                        .executes(context -> banIp(context.getSource(), StringArgumentType.getString(context, "target"), null))
                        .then(argument("reason", MiscArguments.message()).executes(context ->
                                banIp(context.getSource(), StringArgumentType.getString(context, "target"), MiscArguments.getMessage(context, "reason"))))));
        dispatcher.register(literal("banlist").requires(source ->
                (players(source).getBannedPlayers().isLanServer() || players(source).getBannedIPs().isLanServer()) && source.hasPermissionLevel(3))
                .executes(context -> {
                    Map<String, UserListEntryBan<?>> entries = playerBans(context.getSource());
                    entries.putAll(ipBans(context.getSource()));
                    return showBans(context.getSource(), entries);
                })
                .then(literal("ips").executes(context -> showBans(context.getSource(), ipBans(context.getSource()))))
                .then(literal("players").executes(context -> showBans(context.getSource(), playerBans(context.getSource())))));
        dispatcher.register(literal("pardon").requires(source -> players(source).getBannedPlayers().isLanServer() && source.hasPermissionLevel(3))
                .then(argument("targets", GameProfileArgument.gameProfile()).suggests(BANNED_PLAYERS).executes(context -> {
                    UserListBans bans = players(context.getSource()).getBannedPlayers();
                    int pardoned = 0;
                    for (GameProfile profile : GameProfileArgument.getGameProfiles(context, "targets")) {
                        if (!bans.isBanned(profile)) continue;
                        bans.removeEntry(profile);
                        pardoned++;
                        context.getSource().sendFeedback(new TextComponentTranslation("commands.pardon.success", new TextComponentString(profile.getName())), true);
                    }
                    if (pardoned == 0) throw CommandExceptions.failure("commands.pardon.failed");
                    return pardoned;
                })));
        dispatcher.register(literal("pardon-ip").requires(source -> players(source).getBannedIPs().isLanServer() && source.hasPermissionLevel(3))
                .then(argument("target", StringArgumentType.word()).suggests(BANNED_IPS).executes(context -> {
                    String ip = StringArgumentType.getString(context, "target");
                    if (!IP_ADDRESS.matcher(ip).matches()) throw CommandExceptions.failure("commands.pardonip.invalid");
                    UserListIPBans bans = players(context.getSource()).getBannedIPs();
                    if (bans.getEntry(ip) == null) throw CommandExceptions.failure("commands.pardonip.failed");
                    bans.removeEntry(ip);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.pardonip.success", ip), true);
                    return 1;
                })));

        dispatcher.register(literal("op").requires(source -> source.hasPermissionLevel(3))
                .then(argument("targets", GameProfileArgument.gameProfile()).suggests(NOT_OPS).executes(context -> {
                    PlayerList list = players(context.getSource());
                    int changed = 0;
                    for (GameProfile profile : GameProfileArgument.getGameProfiles(context, "targets")) {
                        if (list.canSendCommands(profile)) continue;
                        list.addOp(profile);
                        changed++;
                        context.getSource().sendFeedback(new TextComponentTranslation("commands.op.success", profile.getName()), true);
                    }
                    if (changed == 0) throw CommandExceptions.failure("commands.op.failed");
                    return changed;
                })));
        dispatcher.register(literal("deop").requires(source -> source.hasPermissionLevel(3))
                .then(argument("targets", GameProfileArgument.gameProfile()).suggests(OPS).executes(context -> {
                    PlayerList list = players(context.getSource());
                    int changed = 0;
                    for (GameProfile profile : GameProfileArgument.getGameProfiles(context, "targets")) {
                        if (!list.canSendCommands(profile)) continue;
                        list.removeOp(profile);
                        changed++;
                        context.getSource().sendFeedback(new TextComponentTranslation("commands.deop.success", profile.getName()), true);
                    }
                    if (changed == 0) throw CommandExceptions.failure("commands.deop.failed");
                    return changed;
                })));

        registerWhitelist(dispatcher);
        registerSaving(dispatcher);
    }

    private static PlayerList players(CommandSource source) {
        return source.getServer().getPlayerList();
    }

    private static List<String> onlineNames(CommandSource source, Predicate<GameProfile> filter) {
        List<String> names = new ArrayList<>();
        for (EntityPlayerMP player : players(source).getPlayers()) {
            if (filter.test(player.getGameProfile())) names.add(player.getGameProfile().getName());
        }
        return names;
    }

    private static ITextComponent joinNames(Collection<EntityPlayerMP> players) {
        ITextComponent names = new TextComponentString("");
        int i = 0;
        for (EntityPlayerMP player : players) {
            if (i++ > 0) names.appendText(", ");
            names.appendSibling(player.getDisplayName());
        }
        return names;
    }

    // ---- kick and bans ----

    private static int kick(CommandSource source, Collection<EntityPlayerMP> targets, ITextComponent reason) {
        for (EntityPlayerMP player : targets) {
            player.connection.disconnect(reason);
            source.sendFeedback(new TextComponentTranslation("commands.kick.success", player.getDisplayName(), reason), true);
        }
        return targets.size();
    }

    private static int ban(CommandSource source, Collection<GameProfile> profiles, @Nullable ITextComponent reason) throws CommandSyntaxException {
        UserListBans bans = players(source).getBannedPlayers();
        int banned = 0;
        for (GameProfile profile : profiles) {
            if (bans.isBanned(profile)) continue;
            UserListBansEntry entry = new UserListBansEntry(profile, null, source.getName(), null, reason == null ? null : reason.getUnformattedText());
            bans.addEntry(entry);
            banned++;
            source.sendFeedback(new TextComponentTranslation("commands.ban.success", new TextComponentString(profile.getName()), entry.getBanReason()), true);
            EntityPlayerMP online = players(source).getPlayerByUUID(profile.getId());
            if (online != null) online.connection.disconnect(new TextComponentTranslation("multiplayer.disconnect.banned"));
        }
        if (banned == 0) throw CommandExceptions.failure("commands.ban.failed");
        return banned;
    }

    /** By address, or by the address of an online player. */
    private static int banIp(CommandSource source, String target, @Nullable ITextComponent reason) throws CommandSyntaxException {
        String ip;
        if (IP_ADDRESS.matcher(target).matches()) {
            ip = target;
        } else {
            EntityPlayerMP player = players(source).getPlayerByUsername(target);
            if (player == null) throw CommandExceptions.failure("commands.banip.invalid");
            ip = player.getPlayerIP();
        }
        UserListIPBans bans = players(source).getBannedIPs();
        if (bans.getEntry(ip) != null) throw CommandExceptions.failure("commands.banip.failed");
        List<EntityPlayerMP> affected = players(source).getPlayersMatchingAddress(ip);
        UserListIPBansEntry entry = new UserListIPBansEntry(ip, null, source.getName(), null, reason == null ? null : reason.getUnformattedText());
        bans.addEntry(entry);
        source.sendFeedback(new TextComponentTranslation("commands.banip.success", ip, entry.getBanReason()), true);
        if (!affected.isEmpty()) source.sendFeedback(new TextComponentTranslation("commands.banip.info", affected.size(), joinNames(affected)), true);
        for (EntityPlayerMP player : affected) player.connection.disconnect(new TextComponentTranslation("multiplayer.disconnect.ip_banned"));
        return affected.size();
    }

    /** Bans by player name. */
    private static Map<String, UserListEntryBan<?>> playerBans(CommandSource source) {
        UserListBans bans = players(source).getBannedPlayers();
        Map<String, UserListEntryBan<?>> entries = new LinkedHashMap<>();
        for (String name : bans.getKeys()) {
            UserListBansEntry entry = bans.getEntry(bans.getBannedProfile(name));
            if (entry != null) entries.put(name, entry);
        }
        return entries;
    }

    /** Bans by address. */
    private static Map<String, UserListEntryBan<?>> ipBans(CommandSource source) {
        UserListIPBans bans = players(source).getBannedIPs();
        Map<String, UserListEntryBan<?>> entries = new LinkedHashMap<>();
        for (String ip : bans.getKeys()) {
            UserListIPBansEntry entry = bans.getEntry(ip);
            if (entry != null) entries.put(ip, entry);
        }
        return entries;
    }

    private static int showBans(CommandSource source, Map<String, UserListEntryBan<?>> entries) {
        if (entries.isEmpty()) {
            source.sendFeedback(new TextComponentTranslation("commands.banlist.none"), false);
            return 0;
        }
        source.sendFeedback(new TextComponentTranslation("commands.banlist.list", entries.size()), false);
        for (Map.Entry<String, UserListEntryBan<?>> entry : entries.entrySet()) {
            source.sendFeedback(new TextComponentTranslation("commands.banlist.entry", entry.getKey(),
                    ((UserListEntryBanAccessor) entry.getValue()).portium$getBannedBy(), entry.getValue().getBanReason()), false);
        }
        return entries.size();
    }

    // ---- whitelist ----

    private static void registerWhitelist(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("whitelist").requires(source -> source.hasPermissionLevel(3))
                .then(literal("on").executes(context -> {
                    if (players(context.getSource()).isWhiteListEnabled()) throw CommandExceptions.failure("commands.whitelist.alreadyOn");
                    players(context.getSource()).setWhiteListEnabled(true);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.whitelist.enabled"), true);
                    return 1;
                }))
                .then(literal("off").executes(context -> {
                    if (!players(context.getSource()).isWhiteListEnabled()) throw CommandExceptions.failure("commands.whitelist.alreadyOff");
                    players(context.getSource()).setWhiteListEnabled(false);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.whitelist.disabled"), true);
                    return 1;
                }))
                .then(literal("list").executes(context -> {
                    String[] names = players(context.getSource()).getWhitelistedPlayerNames();
                    if (names.length == 0) context.getSource().sendFeedback(new TextComponentTranslation("commands.whitelist.none"), false);
                    else context.getSource().sendFeedback(new TextComponentTranslation("commands.whitelist.list", names.length, String.join(", ", names)), false);
                    return names.length;
                }))
                .then(literal("add").then(argument("targets", GameProfileArgument.gameProfile()).suggests(NOT_WHITELISTED).executes(context -> {
                    PlayerList list = players(context.getSource());
                    int added = 0;
                    for (GameProfile profile : GameProfileArgument.getGameProfiles(context, "targets")) {
                        if (list.getWhitelistedPlayers().getEntry(profile) != null) continue;
                        list.addWhitelistedPlayer(profile);
                        added++;
                        context.getSource().sendFeedback(new TextComponentTranslation("commands.whitelist.add.success", new TextComponentString(profile.getName())), true);
                    }
                    if (added == 0) throw CommandExceptions.failure("commands.whitelist.add.failed");
                    return added;
                })))
                .then(literal("remove").then(argument("targets", GameProfileArgument.gameProfile()).suggests(WHITELISTED).executes(context -> {
                    PlayerList list = players(context.getSource());
                    int removed = 0;
                    for (GameProfile profile : GameProfileArgument.getGameProfiles(context, "targets")) {
                        if (list.getWhitelistedPlayers().getEntry(profile) == null) continue;
                        list.removePlayerFromWhitelist(profile);
                        removed++;
                        context.getSource().sendFeedback(new TextComponentTranslation("commands.whitelist.remove.success", new TextComponentString(profile.getName())), true);
                    }
                    if (removed == 0) throw CommandExceptions.failure("commands.whitelist.remove.failed");
                    return removed;
                })))
                .then(literal("reload").executes(context -> {
                    players(context.getSource()).reloadWhitelist();
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.whitelist.reloaded"), true);
                    return 1;
                })));
    }

    // ---- saving and stopping ----

    private static void registerSaving(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("save-all").requires(source -> source.hasPermissionLevel(4))
                .executes(context -> saveAll(context.getSource(), false))
                .then(literal("flush").executes(context -> saveAll(context.getSource(), true))));
        dispatcher.register(literal("save-off").requires(source -> source.hasPermissionLevel(4)).executes(context -> setSaving(context.getSource(), false)));
        dispatcher.register(literal("save-on").requires(source -> source.hasPermissionLevel(4)).executes(context -> setSaving(context.getSource(), true)));
        dispatcher.register(literal("stop").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            context.getSource().sendFeedback(new TextComponentTranslation("commands.stop.stopping"), true);
            context.getSource().getServer().initiateShutdown();
            return 1;
        }));
    }

    /** Saves players and every world, even with saving turned off, and optionally waits for the writes to finish. */
    private static int saveAll(CommandSource source, boolean flush) throws CommandSyntaxException {
        source.sendFeedback(new TextComponentTranslation("commands.save.saving"), false);
        MinecraftServer server = source.getServer();
        server.getPlayerList().saveAllPlayerData();
        try {
            for (WorldServer world : server.worlds) {
                if (world == null) continue;
                boolean disabled = world.disableLevelSaving;
                world.disableLevelSaving = false;
                world.saveAllChunks(true, null);
                world.disableLevelSaving = disabled;
                if (flush) world.flushToDisk();
            }
        } catch (MinecraftException e) {
            throw CommandExceptions.failure("commands.save.failed");
        }
        source.sendFeedback(new TextComponentTranslation("commands.save.success"), true);
        return 1;
    }

    private static int setSaving(CommandSource source, boolean enabled) throws CommandSyntaxException {
        boolean changed = false;
        for (WorldServer world : source.getServer().worlds) {
            if (world != null && world.disableLevelSaving == enabled) {
                world.disableLevelSaving = !enabled;
                changed = true;
            }
        }
        if (!changed) throw CommandExceptions.failure(enabled ? "commands.save.alreadyOn" : "commands.save.alreadyOff");
        source.sendFeedback(new TextComponentTranslation(enabled ? "commands.save.enabled" : "commands.save.disabled"), true);
        return 1;
    }

    private AdminCommands() {}
}
