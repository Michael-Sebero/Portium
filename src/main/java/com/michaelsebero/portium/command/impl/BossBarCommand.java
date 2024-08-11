package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.api.IDataPackSettings;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.ComponentResolver;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.ComponentArgument;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.IdSuggestions;
import com.michaelsebero.portium.command.arguments.ResourceLocationArgument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.BoolArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code bossbar}: custom boss bars that data packs and commands control (1.13). Saved in {@code level.dat}. */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class BossBarCommand {

    private static final CommandExceptionType.Dynamic EXISTS = dynamic("commands.bossbar.create.failed");
    private static final CommandExceptionType.Dynamic UNKNOWN = dynamic("commands.bossbar.unknown");
    private static final CommandExceptionType.Simple NO_CHANGE_PLAYERS = simple("commands.bossbar.set.players.unchanged");
    private static final CommandExceptionType.Simple NO_CHANGE_NAME = simple("commands.bossbar.set.name.unchanged");
    private static final CommandExceptionType.Simple NO_CHANGE_COLOR = simple("commands.bossbar.set.color.unchanged");
    private static final CommandExceptionType.Simple NO_CHANGE_STYLE = simple("commands.bossbar.set.style.unchanged");
    private static final CommandExceptionType.Simple NO_CHANGE_VALUE = simple("commands.bossbar.set.value.unchanged");
    private static final CommandExceptionType.Simple NO_CHANGE_MAX = simple("commands.bossbar.set.max.unchanged");
    private static final CommandExceptionType.Simple ALREADY_HIDDEN = simple("commands.bossbar.set.visibility.unchanged.hidden");
    private static final CommandExceptionType.Simple ALREADY_VISIBLE = simple("commands.bossbar.set.visibility.unchanged.visible");

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    private static CommandExceptionType.Dynamic dynamic(String key) {
        return new CommandExceptionType.Dynamic(arg -> TextMessage.translate(key, arg));
    }

    /** The boss bars of the running server, in creation order. */
    private static final Map<ResourceLocation, CustomBossBar> BARS = new LinkedHashMap<>();
    private static boolean dirty;

    public static final SuggestionProvider<CommandSource> SUGGESTIONS = (context, builder) -> IdSuggestions.suggest(BARS.keySet(), builder);

    /** A boss bar with a value out of a maximum, shown to chosen players (who keep seeing it after relogging). */
    public static final class CustomBossBar extends BossInfoServer {

        private final ResourceLocation id;
        private final Set<UUID> players = new HashSet<>();
        private int value;
        private int max = 100;
        /** 1.12's boss bars don't expose their visibility, so it is mirrored here. */
        private boolean visible = true;

        CustomBossBar(ResourceLocation id, ITextComponent name) {
            super(name, BossInfo.Color.WHITE, BossInfo.Overlay.PROGRESS);
            this.id = id;
            this.setPercent(0.0F);
        }

        public ResourceLocation getId() {
            return this.id;
        }

        public int getValue() {
            return this.value;
        }

        public int getMax() {
            return this.max;
        }

        public void setValue(int value) {
            dirty = true;
            this.value = value;
            this.setPercent(MathHelper.clamp((float) value / this.max, 0.0F, 1.0F));
        }

        public void setMax(int max) {
            dirty = true;
            this.max = Math.max(1, max);
            this.setPercent(MathHelper.clamp((float) this.value / this.max, 0.0F, 1.0F));
        }

        ITextComponent describe() {
            ITextComponent text = new TextComponentString("[").appendSibling(this.getName().createCopy()).appendText("]");
            text.getStyle().setColor(net.minecraft.util.text.TextFormatting.WHITE);
            return text;
        }

        /** Shows the bar to exactly these players; returns whether that changed anything. */
        boolean setPlayers(Collection<EntityPlayerMP> targets) {
            Set<UUID> wanted = new HashSet<>();
            for (EntityPlayerMP player : targets) wanted.add(player.getUniqueID());
            boolean changed = !wanted.equals(this.players);
            for (EntityPlayerMP player : new ArrayList<>(this.getPlayers())) {
                if (!wanted.contains(player.getUniqueID())) this.removePlayer(player);
            }
            for (EntityPlayerMP player : targets) {
                if (!this.getPlayers().contains(player)) this.addPlayer(player);
            }
            this.players.clear();
            this.players.addAll(wanted);
            dirty |= changed;
            return changed;
        }

        @Override
        public void setName(ITextComponent name) {
            dirty = true;
            super.setName(name);
        }

        @Override
        public void setColor(BossInfo.Color color) {
            dirty = true;
            super.setColor(color);
        }

        @Override
        public void setOverlay(BossInfo.Overlay overlay) {
            dirty = true;
            super.setOverlay(overlay);
        }

        @Override
        public void setVisible(boolean visible) {
            dirty = true;
            this.visible = visible;
            super.setVisible(visible);
        }

        public boolean isVisible() {
            return this.visible;
        }

        void onPlayerJoin(EntityPlayerMP player) {
            if (this.players.contains(player.getUniqueID())) this.addPlayer(player);
        }

        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("Name", ITextComponent.Serializer.componentToJson(this.getName()));
            tag.setBoolean("Visible", this.isVisible());
            tag.setInteger("Value", this.value);
            tag.setInteger("Max", this.max);
            tag.setString("Color", this.getColor().name().toLowerCase(Locale.ROOT));
            tag.setString("Overlay", overlayName(this.getOverlay()));
            tag.setBoolean("DarkenScreen", this.shouldDarkenSky());
            tag.setBoolean("PlayBossMusic", this.shouldPlayEndBossMusic());
            tag.setBoolean("CreateWorldFog", this.shouldCreateFog());
            NBTTagList players = new NBTTagList();
            for (UUID uuid : this.players) players.appendTag(NBTUtil.createUUIDTag(uuid));
            tag.setTag("Players", players);
            return tag;
        }

        static CustomBossBar read(ResourceLocation id, NBTTagCompound tag) {
            ITextComponent name = ITextComponent.Serializer.jsonToComponent(tag.getString("Name"));
            CustomBossBar bar = new CustomBossBar(id, name == null ? new TextComponentString(id.toString()) : name);
            bar.setVisible(tag.getBoolean("Visible"));
            bar.setMax(tag.getInteger("Max"));
            bar.setValue(tag.getInteger("Value"));
            BossInfo.Color color = colorByName(tag.getString("Color"));
            if (color != null) bar.setColor(color);
            BossInfo.Overlay overlay = overlayByName(tag.getString("Overlay"));
            if (overlay != null) bar.setOverlay(overlay);
            bar.setDarkenSky(tag.getBoolean("DarkenScreen"));
            bar.setPlayEndBossMusic(tag.getBoolean("PlayBossMusic"));
            bar.setCreateFog(tag.getBoolean("CreateWorldFog"));
            NBTTagList players = tag.getTagList("Players", 10);
            for (int i = 0; i < players.tagCount(); i++) bar.players.add(NBTUtil.getUUIDFromTag(players.getCompoundTagAt(i)));
            return bar;
        }
    }

    private static final String[] OVERLAYS = {"progress", "notched_6", "notched_10", "notched_12", "notched_20"};

    static String overlayName(BossInfo.Overlay overlay) {
        return OVERLAYS[overlay.ordinal()];
    }

    static BossInfo.Overlay overlayByName(String name) {
        for (int i = 0; i < OVERLAYS.length; i++) {
            if (OVERLAYS[i].equals(name)) return BossInfo.Overlay.values()[i];
        }
        return null;
    }

    static BossInfo.Color colorByName(String name) {
        for (BossInfo.Color color : BossInfo.Color.values()) {
            if (color.name().toLowerCase(Locale.ROOT).equals(name)) return color;
        }
        return null;
    }

    // ---- Persistence ----

    public static void load(MinecraftServer server) {
        BARS.clear();
        dirty = false;
        NBTTagCompound saved = IDataPackSettings.of(server.getWorld(0).getWorldInfo()).portium$getCustomBossEvents();
        for (String key : saved.getKeySet()) {
            ResourceLocation id = new ResourceLocation(key);
            BARS.put(id, CustomBossBar.read(id, saved.getCompoundTag(key)));
        }
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            for (CustomBossBar bar : BARS.values()) bar.onPlayerJoin(player);
        }
    }

    private static void save(MinecraftServer server) {
        NBTTagCompound saved = new NBTTagCompound();
        for (CustomBossBar bar : BARS.values()) saved.setTag(bar.getId().toString(), bar.write());
        IDataPackSettings.of(server.getWorld(0).getWorldInfo()).portium$setCustomBossEvents(saved);
    }

    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            for (CustomBossBar bar : BARS.values()) bar.onPlayerJoin((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            for (CustomBossBar bar : BARS.values()) bar.removePlayer((EntityPlayerMP) event.player);
        }
    }

    /** Writes the bars into {@code level.dat}'s data once a tick if anything changed, ready for the next save. */
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent event) {
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END || !dirty) return;
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getWorld(0) == null) return;
        dirty = false;
        save(server);
    }

    // ---- Command ----

    public static CustomBossBar getBossBar(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "id");
        CustomBossBar bar = BARS.get(id);
        if (bar == null) throw UNKNOWN.create(id.toString());
        return bar;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralArgumentBuilder<CommandSource> set = literal("set").then(argument("id", ResourceLocationArgument.resourceLocation()).suggests(SUGGESTIONS)
                .then(literal("name").then(argument("name", ComponentArgument.component()).executes(context -> {
                    CustomBossBar bar = getBossBar(context);
                    ITextComponent name = ComponentResolver.resolve(context.getSource(), ComponentArgument.getComponent(context, "name"), null);
                    if (bar.getName().equals(name)) throw NO_CHANGE_NAME.create();
                    bar.setName(name);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.name.success", bar.describe()), true);
                    return 0;
                })))
                .then(literal("color").then(colorLiteral("pink")).then(colorLiteral("blue")).then(colorLiteral("red")).then(colorLiteral("green"))
                        .then(colorLiteral("yellow")).then(colorLiteral("purple")).then(colorLiteral("white")))
                .then(literal("style").then(styleLiteral("progress")).then(styleLiteral("notched_6")).then(styleLiteral("notched_10"))
                        .then(styleLiteral("notched_12")).then(styleLiteral("notched_20")))
                .then(literal("value").then(argument("value", IntegerArgumentType.integer(0)).executes(context -> {
                    CustomBossBar bar = getBossBar(context);
                    int value = IntegerArgumentType.getInteger(context, "value");
                    if (bar.getValue() == value) throw NO_CHANGE_VALUE.create();
                    bar.setValue(value);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.value.success", bar.describe(), value), true);
                    return value;
                })))
                .then(literal("max").then(argument("max", IntegerArgumentType.integer(1)).executes(context -> {
                    CustomBossBar bar = getBossBar(context);
                    int max = IntegerArgumentType.getInteger(context, "max");
                    if (bar.getMax() == max) throw NO_CHANGE_MAX.create();
                    bar.setMax(max);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.max.success", bar.describe(), max), true);
                    return max;
                })))
                .then(literal("visible").then(argument("visible", BoolArgumentType.bool()).executes(context -> {
                    CustomBossBar bar = getBossBar(context);
                    boolean visible = BoolArgumentType.getBool(context, "visible");
                    if (bar.isVisible() == visible) throw visible ? ALREADY_VISIBLE.create() : ALREADY_HIDDEN.create();
                    bar.setVisible(visible);
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.visible.success." + (visible ? "visible" : "hidden"), bar.describe()), true);
                    return 0;
                })))
                .then(literal("players").executes(context -> setPlayers(context, Collections.emptyList()))
                        .then(argument("targets", EntityArgument.players()).executes(context -> setPlayers(context, EntityArgument.getPlayersAllowingNone(context, "targets"))))));
        dispatcher.register(literal("bossbar").requires(source -> source.hasPermissionLevel(2))
                .then(literal("add").then(argument("id", ResourceLocationArgument.resourceLocation()).then(argument("name", ComponentArgument.component()).executes(context -> {
                    ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "id");
                    if (BARS.containsKey(id)) throw EXISTS.create(id.toString());
                    CustomBossBar bar = new CustomBossBar(id, ComponentResolver.resolve(context.getSource(), ComponentArgument.getComponent(context, "name"), null));
                    BARS.put(id, bar);
                    dirty = true;
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.create.success", bar.describe()), true);
                    return BARS.size();
                }))))
                .then(literal("remove").then(argument("id", ResourceLocationArgument.resourceLocation()).suggests(SUGGESTIONS).executes(context -> {
                    CustomBossBar bar = getBossBar(context);
                    bar.setPlayers(Collections.emptyList());
                    BARS.remove(bar.getId());
                    dirty = true;
                    context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.remove.success", bar.describe()), true);
                    return BARS.size();
                })))
                .then(literal("list").executes(context -> {
                    if (BARS.isEmpty()) {
                        context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.list.bars.none"), false);
                    } else {
                        ITextComponent list = new TextComponentString("");
                        int i = 0;
                        for (CustomBossBar bar : BARS.values()) {
                            if (i++ > 0) list.appendText(", ");
                            list.appendSibling(bar.describe());
                        }
                        context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.list.bars.some", BARS.size(), list), false);
                    }
                    return BARS.size();
                }))
                .then(set)
                .then(literal("get").then(argument("id", ResourceLocationArgument.resourceLocation()).suggests(SUGGESTIONS)
                        .then(literal("value").executes(context -> {
                            CustomBossBar bar = getBossBar(context);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.get.value", bar.describe(), bar.getValue()), true);
                            return bar.getValue();
                        }))
                        .then(literal("max").executes(context -> {
                            CustomBossBar bar = getBossBar(context);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.get.max", bar.describe(), bar.getMax()), true);
                            return bar.getMax();
                        }))
                        .then(literal("visible").executes(context -> {
                            CustomBossBar bar = getBossBar(context);
                            context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.get.visible." + (bar.isVisible() ? "visible" : "hidden"), bar.describe()), true);
                            return bar.isVisible() ? 1 : 0;
                        }))
                        .then(literal("players").executes(context -> {
                            CustomBossBar bar = getBossBar(context);
                            Collection<EntityPlayerMP> players = bar.getPlayers();
                            if (players.isEmpty()) {
                                context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.get.players.none", bar.describe()), true);
                            } else {
                                context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.get.players.some", bar.describe(), players.size(), names(players)), true);
                            }
                            return players.size();
                        })))));
    }

    private static ITextComponent names(Collection<EntityPlayerMP> players) {
        ITextComponent names = new TextComponentString("");
        int i = 0;
        for (EntityPlayerMP player : players) {
            if (i++ > 0) names.appendText(", ");
            names.appendSibling(player.getDisplayName());
        }
        return names;
    }

    private static LiteralArgumentBuilder<CommandSource> colorLiteral(String name) {
        BossInfo.Color color = colorByName(name);
        return literal(name).executes(context -> {
            CustomBossBar bar = getBossBar(context);
            if (bar.getColor() == color) throw NO_CHANGE_COLOR.create();
            bar.setColor(color);
            context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.color.success", bar.describe()), true);
            return 0;
        });
    }

    private static LiteralArgumentBuilder<CommandSource> styleLiteral(String name) {
        BossInfo.Overlay overlay = overlayByName(name);
        return literal(name).executes(context -> {
            CustomBossBar bar = getBossBar(context);
            if (bar.getOverlay() == overlay) throw NO_CHANGE_STYLE.create();
            bar.setOverlay(overlay);
            context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.style.success", bar.describe()), true);
            return 0;
        });
    }

    private static int setPlayers(CommandContext<CommandSource> context, Collection<EntityPlayerMP> players) throws CommandSyntaxException {
        CustomBossBar bar = getBossBar(context);
        if (!bar.setPlayers(players)) throw NO_CHANGE_PLAYERS.create();
        if (players.isEmpty()) context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.players.success.none", bar.describe()), true);
        else context.getSource().sendFeedback(new TextComponentTranslation("commands.bossbar.set.players.success.some", bar.describe(), players.size(), names(players)), true);
        return players.size();
    }

    private BossBarCommand() {}
}
