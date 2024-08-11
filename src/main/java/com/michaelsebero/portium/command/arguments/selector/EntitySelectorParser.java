package com.michaelsebero.portium.command.arguments.selector;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.MinMaxBounds;
import com.michaelsebero.portium.command.arguments.ResourceLocationArgument;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.command.nbt.SnbtParser;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Reads 1.13 target selectors: {@code @p @a @r @s @e} with {@code [option=value,...]}, player names and UUIDs. */
public class EntitySelectorParser {

    private static final Random RANDOM = new Random();

    public static final BiConsumer<Vec3d, List<? extends Entity>> ARBITRARY = (pos, list) -> {};
    public static final BiConsumer<Vec3d, List<? extends Entity>> NEAREST = (pos, list) -> list.sort((a, b) -> Double.compare(a.getDistanceSq(pos.x, pos.y, pos.z), b.getDistanceSq(pos.x, pos.y, pos.z)));
    public static final BiConsumer<Vec3d, List<? extends Entity>> FURTHEST = (pos, list) -> list.sort((a, b) -> Double.compare(b.getDistanceSq(pos.x, pos.y, pos.z), a.getDistanceSq(pos.x, pos.y, pos.z)));
    public static final BiConsumer<Vec3d, List<? extends Entity>> RANDOM_ORDER = (pos, list) -> Collections.shuffle(list, RANDOM);

    private static final String[] OPTIONS = {"name", "distance", "level", "x", "y", "z", "dx", "dy", "dz", "x_rotation", "y_rotation", "limit", "sort",
            "gamemode", "team", "type", "tag", "nbt", "scores", "advancements"};

    private final StringReader reader;
    private final boolean allowSelectors;
    private int limit;
    private boolean includeNonPlayers;
    private boolean currentWorldOnly;
    private MinMaxBounds distance = MinMaxBounds.UNBOUNDED;
    private MinMaxBounds level = MinMaxBounds.UNBOUNDED;
    @Nullable
    private Double x;
    @Nullable
    private Double y;
    @Nullable
    private Double z;
    @Nullable
    private Double dx;
    @Nullable
    private Double dy;
    @Nullable
    private Double dz;
    private MinMaxBounds xRotation = MinMaxBounds.UNBOUNDED;
    private MinMaxBounds yRotation = MinMaxBounds.UNBOUNDED;
    private Predicate<Entity> filter = entity -> true;
    private BiConsumer<Vec3d, List<? extends Entity>> sorter = ARBITRARY;
    private boolean self;
    @Nullable
    private String username;
    @Nullable
    private UUID uuid;
    /** What to suggest if parsing stops where the cursor is. */
    private Function<SuggestionsBuilder, Suggestions> suggester = builder -> Suggestions.empty();
    private boolean hasName;
    private boolean excludesName;
    private boolean hasLimit;
    private boolean hasSort;
    private boolean hasGamemode;
    private boolean excludesGamemode;
    private boolean hasTeam;
    private boolean excludesTeam;
    private Class<? extends Entity> type = Entity.class;
    private boolean hasType;
    private boolean excludesType;
    private boolean hasScores;
    private boolean hasAdvancements;
    private boolean checkPermission;

    public EntitySelectorParser(StringReader reader) {
        this(reader, true);
    }

    public EntitySelectorParser(StringReader reader, boolean allowSelectors) {
        this.reader = reader;
        this.allowSelectors = allowSelectors;
    }

    public EntitySelector build() {
        AxisAlignedBB box = null;
        if (this.dx != null || this.dy != null || this.dz != null) {
            box = createBox(this.dx == null ? 0.0D : this.dx, this.dy == null ? 0.0D : this.dy, this.dz == null ? 0.0D : this.dz);
        }
        Function<Vec3d, Vec3d> position = this.x == null && this.y == null && this.z == null ? pos -> pos
                : pos -> new Vec3d(this.x == null ? pos.x : this.x, this.y == null ? pos.y : this.y, this.z == null ? pos.z : this.z);
        Predicate<Entity> predicate = this.filter;
        if (!this.xRotation.isUnbounded()) predicate = predicate.and(rotationFilter(this.xRotation, entity -> entity.rotationPitch));
        if (!this.yRotation.isUnbounded()) predicate = predicate.and(rotationFilter(this.yRotation, entity -> entity.rotationYaw));
        if (!this.level.isUnbounded()) {
            MinMaxBounds levels = this.level;
            predicate = predicate.and(entity -> entity instanceof EntityPlayerMP && levels.test(((EntityPlayerMP) entity).experienceLevel));
        }
        return new EntitySelector(this.limit, this.includeNonPlayers, this.currentWorldOnly, predicate, this.distance, position, box, this.sorter,
                this.self, this.username, this.uuid, this.type, this.checkPermission);
    }

    private static AxisAlignedBB createBox(double dx, double dy, double dz) {
        return new AxisAlignedBB(dx < 0 ? dx : 0, dy < 0 ? dy : 0, dz < 0 ? dz : 0, (dx < 0 ? 0 : dx) + 1, (dy < 0 ? 0 : dy) + 1, (dz < 0 ? 0 : dz) + 1);
    }

    private static Predicate<Entity> rotationFilter(MinMaxBounds bounds, Function<Entity, Float> rotation) {
        double min = MathHelper.wrapDegrees(bounds.getMin() == null ? 0.0F : bounds.getMin().floatValue());
        double max = MathHelper.wrapDegrees(bounds.getMax() == null ? 359.0F : bounds.getMax().floatValue());
        return entity -> {
            double value = MathHelper.wrapDegrees(rotation.apply(entity));
            return min > max ? value >= min || value <= max : value >= min && value <= max;
        };
    }

    public EntitySelector parse() throws CommandSyntaxException {
        this.suggester = this::suggestNameOrSelector;
        if (this.reader.canRead() && this.reader.peek() == '@') {
            if (!this.allowSelectors) throw CommandExceptions.SELECTOR_NOT_ALLOWED.createWithContext(this.reader);
            this.reader.skip();
            this.parseSelector();
        } else {
            this.parseNameOrUuid();
        }
        return this.build();
    }

    private void parseNameOrUuid() throws CommandSyntaxException {
        int start = this.reader.getCursor();
        String text = this.reader.readString();
        try {
            this.uuid = UUID.fromString(text);
            this.includeNonPlayers = true;
        } catch (IllegalArgumentException e) {
            if (text.isEmpty() || text.length() > 16) {
                this.reader.setCursor(start);
                throw CommandExceptions.ENTITY_INVALID_NAME.createWithContext(this.reader);
            }
            this.includeNonPlayers = false;
            this.username = text;
        }
        this.limit = 1;
    }

    private void parseSelector() throws CommandSyntaxException {
        this.checkPermission = true;
        this.suggester = this::suggestSelectorType;
        if (!this.reader.canRead()) throw CommandExceptions.SELECTOR_MISSING.createWithContext(this.reader);
        int start = this.reader.getCursor();
        char c = this.reader.read();
        switch (c) {
            case 'p':
                this.limit = 1;
                this.includeNonPlayers = false;
                this.sorter = NEAREST;
                this.limitToType(EntityPlayerMP.class);
                break;
            case 'a':
                this.limit = Integer.MAX_VALUE;
                this.includeNonPlayers = false;
                this.sorter = ARBITRARY;
                this.limitToType(EntityPlayerMP.class);
                break;
            case 'r':
                this.limit = 1;
                this.includeNonPlayers = false;
                this.sorter = RANDOM_ORDER;
                this.limitToType(EntityPlayerMP.class);
                break;
            case 's':
                this.limit = 1;
                this.includeNonPlayers = true;
                this.self = true;
                break;
            case 'e':
                this.limit = Integer.MAX_VALUE;
                this.includeNonPlayers = true;
                this.sorter = ARBITRARY;
                this.filter = Entity::isEntityAlive;
                break;
            default:
                this.reader.setCursor(start);
                throw CommandExceptions.UNKNOWN_SELECTOR.createWithContext(this.reader, "@" + c);
        }
        this.suggester = this::suggestOpenOptions;
        if (this.reader.canRead() && this.reader.peek() == '[') {
            this.reader.skip();
            this.suggester = this::suggestOptionsOrEnd;
            this.parseOptions();
        }
    }

    private void parseOptions() throws CommandSyntaxException {
        this.suggester = this::suggestOptions;
        this.reader.skipWhitespace();
        while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            int start = this.reader.getCursor();
            String option = this.reader.readString();
            this.reader.skipWhitespace();
            if (!this.reader.canRead() || this.reader.peek() != '=') {
                this.reader.setCursor(start);
                throw CommandExceptions.OPTION_VALUELESS.createWithContext(this.reader, option);
            }
            this.reader.skip();
            this.reader.skipWhitespace();
            this.suggester = builder -> Suggestions.empty();
            this.handleOption(option, start);
            this.reader.skipWhitespace();
            this.suggester = this::suggestOptionsNextOrClose;
            if (!this.reader.canRead()) continue;
            if (this.reader.peek() == ',') {
                this.reader.skip();
                this.suggester = this::suggestOptions;
            } else if (this.reader.peek() != ']') {
                throw CommandExceptions.SELECTOR_OPTIONS_UNTERMINATED.createWithContext(this.reader);
            }
        }
        if (!this.reader.canRead()) throw CommandExceptions.SELECTOR_OPTIONS_UNTERMINATED.createWithContext(this.reader);
        this.reader.skip();
        this.suggester = builder -> Suggestions.empty();
    }

    private boolean readNegation() {
        this.reader.skipWhitespace();
        if (this.reader.canRead() && this.reader.peek() == '!') {
            this.reader.skip();
            this.reader.skipWhitespace();
            return true;
        }
        return false;
    }

    private void addFilter(Predicate<Entity> predicate) {
        this.filter = this.filter.and(predicate);
    }

    private void limitToType(Class<? extends Entity> type) {
        this.type = type;
    }

    private CommandSyntaxException inapplicable(int start, String option) {
        this.reader.setCursor(start);
        return CommandExceptions.OPTION_INAPPLICABLE.createWithContext(this.reader, option);
    }

    private void handleOption(String option, int start) throws CommandSyntaxException {
        switch (option) {
            case "name": {
                boolean negated = this.readNegation();
                if (this.hasName && !negated || this.excludesName && !negated) throw this.inapplicable(start, option);
                String name = this.reader.readString();
                if (negated) this.excludesName = true;
                else this.hasName = true;
                this.addFilter(entity -> entity.getName().equals(name) != negated);
                break;
            }
            case "distance": {
                int valueStart = this.reader.getCursor();
                MinMaxBounds bounds = MinMaxBounds.parseFloat(this.reader);
                if (bounds.getMin() != null && bounds.getMin() < 0 || bounds.getMax() != null && bounds.getMax() < 0) {
                    this.reader.setCursor(valueStart);
                    throw CommandExceptions.DISTANCE_NEGATIVE.createWithContext(this.reader);
                }
                this.distance = bounds;
                this.currentWorldOnly = true;
                break;
            }
            case "level": {
                int valueStart = this.reader.getCursor();
                MinMaxBounds bounds = MinMaxBounds.parseInt(this.reader);
                if (bounds.getMin() != null && bounds.getMin() < 0 || bounds.getMax() != null && bounds.getMax() < 0) {
                    this.reader.setCursor(valueStart);
                    throw CommandExceptions.LEVEL_NEGATIVE.createWithContext(this.reader);
                }
                this.level = bounds;
                this.includeNonPlayers = false;
                break;
            }
            case "x": this.x = this.reader.readDouble(); this.currentWorldOnly = true; break;
            case "y": this.y = this.reader.readDouble(); this.currentWorldOnly = true; break;
            case "z": this.z = this.reader.readDouble(); this.currentWorldOnly = true; break;
            case "dx": this.dx = this.reader.readDouble(); this.currentWorldOnly = true; break;
            case "dy": this.dy = this.reader.readDouble(); this.currentWorldOnly = true; break;
            case "dz": this.dz = this.reader.readDouble(); this.currentWorldOnly = true; break;
            case "x_rotation": this.xRotation = MinMaxBounds.parseWrapped(this.reader); break;
            case "y_rotation": this.yRotation = MinMaxBounds.parseWrapped(this.reader); break;
            case "limit": {
                if (this.self || this.hasLimit) throw this.inapplicable(start, option);
                int valueStart = this.reader.getCursor();
                int limit = this.reader.readInt();
                if (limit < 1) {
                    this.reader.setCursor(valueStart);
                    throw CommandExceptions.LIMIT_TOO_SMALL.createWithContext(this.reader);
                }
                this.limit = limit;
                this.hasLimit = true;
                break;
            }
            case "sort": {
                if (this.self || this.hasSort) throw this.inapplicable(start, option);
                int valueStart = this.reader.getCursor();
                this.suggester = builder -> suggestFrom(builder, "nearest", "furthest", "random", "arbitrary");
                String sort = this.reader.readUnquotedString();
                switch (sort) {
                    case "nearest": this.sorter = NEAREST; break;
                    case "furthest": this.sorter = FURTHEST; break;
                    case "random": this.sorter = RANDOM_ORDER; break;
                    case "arbitrary": this.sorter = ARBITRARY; break;
                    default:
                        this.reader.setCursor(valueStart);
                        throw CommandExceptions.SORT_IRREVERSIBLE.createWithContext(this.reader, sort);
                }
                this.hasSort = true;
                break;
            }
            case "gamemode": {
                this.suggester = builder -> suggestNegatable(builder, "survival", "creative", "adventure", "spectator");
                int valueStart = this.reader.getCursor();
                boolean negated = this.readNegation();
                if (this.hasGamemode && !negated || this.excludesGamemode && !negated) throw this.inapplicable(start, option);
                String name = this.reader.readUnquotedString();
                GameType mode = GameType.parseGameTypeWithDefault(name, GameType.NOT_SET);
                if (mode == GameType.NOT_SET) {
                    this.reader.setCursor(valueStart);
                    throw CommandExceptions.MODE_INVALID.createWithContext(this.reader, name);
                }
                this.includeNonPlayers = false;
                this.addFilter(entity -> entity instanceof EntityPlayerMP && (((EntityPlayerMP) entity).interactionManager.getGameType() == mode) != negated);
                if (negated) this.excludesGamemode = true;
                else this.hasGamemode = true;
                break;
            }
            case "team": {
                boolean negated = this.readNegation();
                if (this.hasTeam && !negated || this.excludesTeam && !negated) throw this.inapplicable(start, option);
                String team = this.reader.readUnquotedString();
                this.addFilter(entity -> {
                    Team entityTeam = entity.getTeam();
                    String name = entityTeam == null ? "" : entityTeam.getName();
                    return name.equals(team) != negated;
                });
                if (negated) this.excludesTeam = true;
                else this.hasTeam = true;
                break;
            }
            case "type": {
                this.suggester = this::suggestEntityTypes;
                int valueStart = this.reader.getCursor();
                boolean negated = this.readNegation();
                if (this.hasType && !negated || this.excludesType && !negated) throw this.inapplicable(start, option);
                if (this.reader.canRead() && this.reader.peek() == '#') {
                    this.reader.skip();
                    ResourceLocation tag = ResourceLocationArgument.read(this.reader);
                    this.addFilter(entity -> DataPackManager.entityTypeTagContains(tag, entity) != negated);
                } else {
                    ResourceLocation id = ResourceLocationArgument.read(this.reader);
                    ResourceLocation legacy = EntityTypes.toLegacy(id);
                    if (legacy == null) {
                        this.reader.setCursor(valueStart);
                        throw CommandExceptions.TYPE_INVALID.createWithContext(this.reader, id.toString());
                    }
                    if (EntityTypes.PLAYER.equals(legacy)) {
                        if (!negated) this.includeNonPlayers = false;
                        this.addFilter(entity -> entity instanceof EntityPlayer != negated);
                    } else {
                        Class<? extends Entity> type = EntityList.getClass(legacy);
                        if (!negated && type != null) this.limitToType(type);
                        this.addFilter(entity -> legacy.equals(EntityList.getKey(entity)) != negated);
                    }
                }
                if (negated) this.excludesType = true;
                else this.hasType = true;
                break;
            }
            case "tag": {
                boolean negated = this.readNegation();
                String tag = this.reader.readUnquotedString();
                this.addFilter(entity -> (tag.isEmpty() ? entity.getTags().isEmpty() : entity.getTags().contains(tag)) != negated);
                break;
            }
            case "nbt": {
                boolean negated = this.readNegation();
                NBTTagCompound wanted = new SnbtParser(this.reader).readStruct();
                this.addFilter(entity -> NBTUtil.areNBTEquals(wanted, CommandBase.entityToNBT(entity), true) != negated);
                break;
            }
            case "scores": {
                if (this.hasScores) throw this.inapplicable(start, option);
                this.parseScores();
                this.hasScores = true;
                break;
            }
            case "advancements": {
                if (this.hasAdvancements) throw this.inapplicable(start, option);
                this.parseAdvancements();
                this.hasAdvancements = true;
                break;
            }
            default:
                this.reader.setCursor(start);
                throw CommandExceptions.UNKNOWN_OPTION.createWithContext(this.reader, option);
        }
    }

    private void parseScores() throws CommandSyntaxException {
        this.reader.expect('{');
        this.reader.skipWhitespace();
        Map<String, MinMaxBounds> scores = new HashMap<>();
        while (this.reader.canRead() && this.reader.peek() != '}') {
            this.reader.skipWhitespace();
            String objective = this.reader.readUnquotedString();
            this.reader.skipWhitespace();
            this.reader.expect('=');
            this.reader.skipWhitespace();
            scores.put(objective, MinMaxBounds.parseInt(this.reader));
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == ',') this.reader.skip();
        }
        this.reader.expect('}');
        if (scores.isEmpty()) return;
        this.addFilter(entity -> {
            Scoreboard scoreboard = entity.world.getScoreboard();
            String holder = entity instanceof EntityPlayerMP ? entity.getName() : entity.getCachedUniqueIdString();
            for (Map.Entry<String, MinMaxBounds> entry : scores.entrySet()) {
                ScoreObjective objective = scoreboard.getObjective(entry.getKey());
                if (objective == null || !scoreboard.entityHasObjective(holder, objective)) return false;
                Score score = scoreboard.getOrCreateScore(holder, objective);
                if (!entry.getValue().test(score.getScorePoints())) return false;
            }
            return true;
        });
    }

    private void parseAdvancements() throws CommandSyntaxException {
        this.reader.expect('{');
        this.reader.skipWhitespace();
        Map<ResourceLocation, Predicate<AdvancementProgress>> checks = new HashMap<>();
        while (this.reader.canRead() && this.reader.peek() != '}') {
            this.reader.skipWhitespace();
            ResourceLocation id = ResourceLocationArgument.read(this.reader);
            this.reader.skipWhitespace();
            this.reader.expect('=');
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == '{') {
                Map<String, Boolean> criteria = new HashMap<>();
                this.reader.expect('{');
                this.reader.skipWhitespace();
                while (this.reader.canRead() && this.reader.peek() != '}') {
                    this.reader.skipWhitespace();
                    String criterion = this.reader.readUnquotedString();
                    this.reader.skipWhitespace();
                    this.reader.expect('=');
                    this.reader.skipWhitespace();
                    criteria.put(criterion, this.reader.readBoolean());
                    this.reader.skipWhitespace();
                    if (this.reader.canRead() && this.reader.peek() == ',') this.reader.skip();
                }
                this.reader.skipWhitespace();
                this.reader.expect('}');
                checks.put(id, progress -> {
                    for (Map.Entry<String, Boolean> entry : criteria.entrySet()) {
                        CriterionProgress criterion = progress.getCriterionProgress(entry.getKey());
                        if (criterion == null || criterion.isObtained() != entry.getValue()) return false;
                    }
                    return true;
                });
            } else {
                boolean done = this.reader.readBoolean();
                checks.put(id, progress -> progress.isDone() == done);
            }
            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == ',') this.reader.skip();
        }
        this.reader.expect('}');
        if (checks.isEmpty()) return;
        this.includeNonPlayers = false;
        this.addFilter(entity -> {
            if (!(entity instanceof EntityPlayerMP)) return false;
            EntityPlayerMP player = (EntityPlayerMP) entity;
            for (Map.Entry<ResourceLocation, Predicate<AdvancementProgress>> entry : checks.entrySet()) {
                Advancement advancement = player.server.getAdvancementManager().getAdvancement(entry.getKey());
                if (advancement == null || !entry.getValue().test(player.getAdvancements().getProgress(advancement))) return false;
            }
            return true;
        });
    }

    // ---- Suggestions ----

    /** Fills in suggestions for wherever parsing stopped; {@code names} adds player names. */
    public Suggestions fillSuggestions(SuggestionsBuilder builder, Consumer<SuggestionsBuilder> names) {
        this.nameSuggester = names;
        return this.suggester.apply(builder.createOffset(this.reader.getCursor()));
    }

    private Consumer<SuggestionsBuilder> nameSuggester = builder -> {};

    private Suggestions suggestNameOrSelector(SuggestionsBuilder builder) {
        this.nameSuggester.accept(builder);
        if (this.allowSelectors) {
            for (String selector : new String[] {"@p", "@a", "@r", "@s", "@e"}) {
                if (selector.startsWith(builder.getRemaining())) builder.suggest(selector, TextMessage.translate("argument.entity.selector." + selectorName(selector)));
            }
        }
        return builder.build();
    }

    private static String selectorName(String selector) {
        switch (selector.charAt(1)) {
            case 'p': return "nearestPlayer";
            case 'a': return "allPlayers";
            case 'r': return "randomPlayer";
            case 's': return "self";
            default: return "allEntities";
        }
    }

    private Suggestions suggestSelectorType(SuggestionsBuilder builder) {
        SuggestionsBuilder offset = builder.createOffset(builder.getStart() - 1);
        for (String selector : new String[] {"@p", "@a", "@r", "@s", "@e"}) offset.suggest(selector);
        return offset.build();
    }

    private Suggestions suggestOpenOptions(SuggestionsBuilder builder) {
        builder.suggest("[");
        return builder.build();
    }

    private Suggestions suggestOptionsOrEnd(SuggestionsBuilder builder) {
        builder.suggest("]");
        return this.suggestOptions(builder);
    }

    private Suggestions suggestOptions(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String option : OPTIONS) {
            if (option.startsWith(remaining)) builder.suggest(option + "=", TextMessage.translate("argument.entity.options." + option + ".description"));
        }
        return builder.build();
    }

    private Suggestions suggestOptionsNextOrClose(SuggestionsBuilder builder) {
        builder.suggest(",");
        builder.suggest("]");
        return builder.build();
    }

    private Suggestions suggestEntityTypes(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        boolean negated = remaining.startsWith("!");
        String typed = negated ? remaining.substring(1) : remaining;
        for (ResourceLocation id : EntityTypes.modernIds()) {
            String text = id.toString();
            if (text.startsWith(typed) || id.getPath().startsWith(typed)) builder.suggest((negated ? "!" : "") + text);
        }
        return builder.build();
    }

    private static Suggestions suggestFrom(SuggestionsBuilder builder, String... values) {
        for (String value : values) {
            if (value.startsWith(builder.getRemaining())) builder.suggest(value);
        }
        return builder.build();
    }

    private static Suggestions suggestNegatable(SuggestionsBuilder builder, String... values) {
        String remaining = builder.getRemaining();
        boolean negated = remaining.startsWith("!");
        for (String value : values) {
            String text = (negated ? "!" : "") + value;
            if (text.startsWith(remaining)) builder.suggest(text);
            if (!negated && ("!" + value).startsWith(remaining) && !remaining.isEmpty()) builder.suggest("!" + value);
        }
        return builder.build();
    }
}
