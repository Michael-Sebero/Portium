package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.BlockPosArgument;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.FunctionArgument;
import com.michaelsebero.portium.command.arguments.IdSuggestions;
import com.michaelsebero.portium.command.arguments.ItemArgument;
import com.michaelsebero.portium.command.arguments.MiscArguments;
import com.michaelsebero.portium.command.arguments.ResourceLocationArgument;
import com.michaelsebero.portium.command.arguments.Vec3Argument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.DoubleArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.IntegerArgumentType;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.ArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.compat.LegacyBiomes;
import com.michaelsebero.portium.datapack.CommandFunction;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import com.michaelsebero.portium.loot.LootOrigin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/**
 * The commands 1.14 to 1.16 added: {@code schedule} (1.14; {@code clear} 1.15), {@code loot} (1.14), {@code spectate}
 * (1.15), {@code attribute} and {@code locatebiome} (1.16).
 */
public final class Commands114 {

    private static final CommandExceptionType.Simple SCHEDULE_SAME_TICK = simple("commands.schedule.same_tick");
    private static final CommandExceptionType.Dynamic SCHEDULE_CLEAR_FAILED = dynamic("commands.schedule.cleared.failure");
    private static final CommandExceptionType.Dynamic FUNCTION_UNKNOWN = dynamic("arguments.function.unknown");
    private static final CommandExceptionType.Dynamic FUNCTION_TAG_UNKNOWN = dynamic("arguments.function.tag.unknown");
    private static final CommandExceptionType.Simple NO_HELD_ITEMS = simple("commands.drop.no_held_items");
    private static final CommandExceptionType.Dynamic NO_LOOT_TABLE = dynamic("commands.drop.no_loot_table");
    private static final CommandExceptionType.Dynamic NOT_SPECTATOR = dynamic("commands.spectate.not_spectator");
    private static final CommandExceptionType.Simple SPECTATE_SELF = simple("commands.spectate.self");
    private static final CommandExceptionType.Dynamic ATTRIBUTE_ENTITY = dynamic("commands.attribute.failed.entity");
    private static final CommandExceptionType.Dynamic2 NO_ATTRIBUTE = new CommandExceptionType.Dynamic2((a, b) -> TextMessage.translate(
            "commands.attribute.failed.no_attribute", a, b));
    private static final CommandExceptionType.Simple NO_MODIFIER = simple("commands.attribute.failed.no_modifier");
    private static final CommandExceptionType.Simple MODIFIER_PRESENT = simple("commands.attribute.failed.modifier_already_present");
    private static final CommandExceptionType.Dynamic BIOME_INVALID = dynamic("commands.locatebiome.invalid");
    private static final CommandExceptionType.Dynamic BIOME_NOT_FOUND = dynamic("commands.locatebiome.notFound");

    private static CommandExceptionType.Simple simple(String key) {
        return new CommandExceptionType.Simple(TextMessage.translate(key));
    }

    private static CommandExceptionType.Dynamic dynamic(String key) {
        return new CommandExceptionType.Dynamic(arg -> TextMessage.translate(key, arg));
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        registerSchedule(dispatcher);
        registerLoot(dispatcher);
        registerSpectate(dispatcher);
        registerAttribute(dispatcher);
        registerLocateBiome(dispatcher);
        MinecraftForge.EVENT_BUS.register(Commands114.class);
    }

    // ---- schedule ----

    /** A function or {@code #tag} by its id, as {@code /function} reads it, kept as the id (a schedule runs it later). */
    private static final class FunctionId implements ArgumentType<String> {

        private final FunctionArgument suggestions = FunctionArgument.function();

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            boolean tag = reader.canRead() && reader.peek() == '#';
            if (tag) reader.skip();
            return (tag ? "#" : "") + ResourceLocationArgument.read(reader);
        }

        @Override
        public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return this.suggestions.listSuggestions(context, builder);
        }
    }

    private static void registerSchedule(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("schedule").requires(source -> source.hasPermissionLevel(2) && DataPackManager.get() != null)
                .then(literal("function").then(argument("function", new FunctionId()).then(argument("time", MiscArguments.time())
                        .executes(context -> schedule(context.getSource(), context.getArgument("function", String.class), context.getArgument("time", Integer.class), true))
                        .then(literal("append").executes(context -> schedule(context.getSource(), context.getArgument("function", String.class),
                                context.getArgument("time", Integer.class), false)))
                        .then(literal("replace").executes(context -> schedule(context.getSource(), context.getArgument("function", String.class),
                                context.getArgument("time", Integer.class), true))))))
                .then(literal("clear").then(argument("function", StringArgumentType.greedyString()).suggests(Commands114::scheduledIds)
                        .executes(context -> clearSchedule(context.getSource(), StringArgumentType.getString(context, "function"))))));
    }

    private static Suggestions scheduledIds(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        List<String> ids = new ArrayList<>();
        for (Scheduled entry : schedules(context.getSource().getServer()).entries) if (!ids.contains(entry.id)) ids.add(entry.id);
        return IdSuggestions.suggest(ids, builder);
    }

    private static int schedule(CommandSource source, String id, int time, boolean replace) throws CommandSyntaxException {
        if (time == 0) throw SCHEDULE_SAME_TICK.create();
        resolve(id);
        long at = source.getServer().getWorld(0).getTotalWorldTime() + time;
        Schedules schedules = schedules(source.getServer());
        if (replace) schedules.entries.removeIf(entry -> entry.id.equals(id));
        schedules.entries.add(new Scheduled(id, at));
        schedules.markDirty();
        boolean tag = id.startsWith("#");
        source.sendFeedback(new TextComponentTranslation(tag ? "commands.schedule.created.tag" : "commands.schedule.created.function", tag ? id.substring(1) : id,
                time, at), true);
        return (int) Math.floorMod(at, Integer.MAX_VALUE);
    }

    private static int clearSchedule(CommandSource source, String id) throws CommandSyntaxException {
        Schedules schedules = schedules(source.getServer());
        int before = schedules.entries.size();
        schedules.entries.removeIf(entry -> entry.id.equals(id));
        int removed = before - schedules.entries.size();
        if (removed == 0) throw SCHEDULE_CLEAR_FAILED.create(id);
        schedules.markDirty();
        source.sendFeedback(new TextComponentTranslation("commands.schedule.cleared.success", removed, id), true);
        return removed;
    }

    /** The functions an id names (one, or a tag's), from the loaded data packs. */
    private static List<CommandFunction> resolve(String id) throws CommandSyntaxException {
        DataPackManager manager = DataPackManager.get();
        if (manager == null) throw FUNCTION_UNKNOWN.create(id);
        List<CommandFunction> functions = new ArrayList<>();
        if (id.startsWith("#")) {
            Tag<ResourceLocation> tag = manager.getTags().getFunctions().get(new ResourceLocation(id.substring(1)));
            if (tag == null) throw FUNCTION_TAG_UNKNOWN.create(id.substring(1));
            for (ResourceLocation functionId : tag.getValues()) {
                CommandFunction function = manager.getFunctions().get(functionId);
                if (function != null) functions.add(function);
            }
        } else {
            CommandFunction function = manager.getFunctions().get(new ResourceLocation(id));
            if (function == null) throw FUNCTION_UNKNOWN.create(id);
            functions.add(function);
        }
        return functions;
    }

    private static final class Scheduled {
        final String id;
        final long at;

        Scheduled(String id, long at) {
            this.id = id;
            this.at = at;
        }
    }

    /** The world's scheduled functions, saved with it (1.14 keeps them in level.dat's {@code ScheduledEvents}). */
    public static final class Schedules extends WorldSavedData {

        static final String NAME = "portium_schedules";
        final List<Scheduled> entries = new ArrayList<>();

        public Schedules(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            this.entries.clear();
            NBTTagList list = nbt.getTagList("ScheduledEvents", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                this.entries.add(new Scheduled(entry.getString("Name"), entry.getLong("TriggerTime")));
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTTagList list = new NBTTagList();
            for (Scheduled scheduled : this.entries) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Name", scheduled.id);
                entry.setLong("TriggerTime", scheduled.at);
                list.appendTag(entry);
            }
            nbt.setTag("ScheduledEvents", list);
            return nbt;
        }
    }

    private static Schedules schedules(MinecraftServer server) {
        WorldServer overworld = server.getWorld(0);
        Schedules schedules = (Schedules) overworld.getMapStorage().getOrLoadData(Schedules.class, Schedules.NAME);
        if (schedules == null) {
            schedules = new Schedules(Schedules.NAME);
            overworld.getMapStorage().setData(Schedules.NAME, schedules);
        }
        return schedules;
    }

    /** Runs what's due, at the end of each server tick, as the server (1.14 runs them with the function source). */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        DataPackManager manager = DataPackManager.get();
        if (server == null || manager == null || server.getWorld(0) == null) return;
        Schedules schedules = schedules(server);
        if (schedules.entries.isEmpty()) return;
        long now = server.getWorld(0).getTotalWorldTime();
        List<Scheduled> due = new ArrayList<>();
        for (Iterator<Scheduled> it = schedules.entries.iterator(); it.hasNext(); ) {
            Scheduled entry = it.next();
            if (entry.at <= now) {
                due.add(entry);
                it.remove();
            }
        }
        if (due.isEmpty()) return;
        schedules.markDirty();
        for (Scheduled entry : due) {
            try {
                for (CommandFunction function : resolve(entry.id)) {
                    manager.getFunctions().execute(function, manager.getFunctions().getSource());
                }
            } catch (CommandSyntaxException e) {
                com.michaelsebero.portium.Portium.LOGGER.warn("Scheduled function {} no longer exists", entry.id);
            }
        }
    }

    // ---- loot ----

    @FunctionalInterface
    private interface LootSource {
        List<ItemStack> drop(CommandContext<CommandSource> context) throws CommandSyntaxException;
    }

    @FunctionalInterface
    private interface LootTarget {
        int put(CommandContext<CommandSource> context, List<ItemStack> items) throws CommandSyntaxException;
    }

    private static final SuggestionProvider<CommandSource> LOOT_TABLES = (context, builder) -> {
        List<ResourceLocation> ids = new ArrayList<>(net.minecraft.world.storage.loot.LootTableList.getAll());
        return IdSuggestions.suggest(ids, builder);
    };

    private static void registerLoot(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("loot").requires(source -> source.hasPermissionLevel(2))
                .then(literal("give").then(withSources(argument("players", EntityArgument.players()), (context, items) ->
                        give(context.getSource(), EntityArgument.getPlayers(context, "players"), items))))
                .then(literal("spawn").then(withSources(argument("targetPos", Vec3Argument.vec3()), (context, items) ->
                        spawn(context.getSource(), Vec3Argument.getVec3(context, "targetPos"), items))))
                .then(literal("insert").then(withSources(argument("targetPos", BlockPosArgument.blockPos()), (context, items) ->
                        insert(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "targetPos"), items))))
                .then(literal("replace")
                        .then(literal("block").then(argument("targetPos", BlockPosArgument.blockPos())
                                .then(withSources(argument("slot", MiscArguments.slot()), (context, items) -> replaceBlock(context, items, Integer.MAX_VALUE))
                                        .then(withSources(argument("count", IntegerArgumentType.integer(0)),
                                                (context, items) -> replaceBlock(context, items, IntegerArgumentType.getInteger(context, "count")))))))
                        .then(literal("entity").then(argument("entities", EntityArgument.entities())
                                .then(withSources(argument("slot", MiscArguments.slot()), (context, items) -> replaceEntity(context, items, Integer.MAX_VALUE))
                                        .then(withSources(argument("count", IntegerArgumentType.integer(0)),
                                                (context, items) -> replaceEntity(context, items, IntegerArgumentType.getInteger(context, "count")))))))));
    }

    /** The four loot sources ({@code loot}, {@code fish}, {@code kill}, {@code mine}) under a target's last node. */
    private static <B extends ArgumentBuilder<CommandSource, B>> B withSources(B node, LootTarget put) {
        node.then(literal("loot").then(argument("lootTable", ResourceLocationArgument.resourceLocation()).suggests(LOOT_TABLES)
                .executes(context -> put.put(context, lootFromTable(context)))));
        node.then(literal("fish").then(argument("lootTable", ResourceLocationArgument.resourceLocation()).suggests(LOOT_TABLES)
                .then(argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> put.put(context, fish(context, ItemStack.EMPTY)))
                        .then(argument("tool", ItemArgument.item()).executes(context -> put.put(context, fish(context, ItemArgument.getItem(context, "tool").createStack(1)))))
                        .then(literal("mainhand").executes(context -> put.put(context, fish(context, held(context.getSource(), true)))))
                        .then(literal("offhand").executes(context -> put.put(context, fish(context, held(context.getSource(), false))))))));
        node.then(literal("kill").then(argument("target", EntityArgument.entity()).executes(context -> put.put(context, kill(context)))));
        node.then(literal("mine").then(argument("pos", BlockPosArgument.blockPos())
                .executes(context -> put.put(context, mine(context, ItemStack.EMPTY)))
                .then(argument("tool", ItemArgument.item()).executes(context -> put.put(context, mine(context, ItemArgument.getItem(context, "tool").createStack(1)))))
                .then(literal("mainhand").executes(context -> put.put(context, mine(context, held(context.getSource(), true)))))
                .then(literal("offhand").executes(context -> put.put(context, mine(context, held(context.getSource(), false)))))));
        return node;
    }

    private static ItemStack held(CommandSource source, boolean mainhand) throws CommandSyntaxException {
        Entity entity = source.getEntity();
        if (!(entity instanceof EntityLivingBase)) throw NO_HELD_ITEMS.create();
        return mainhand ? ((EntityLivingBase) entity).getHeldItemMainhand() : ((EntityLivingBase) entity).getHeldItemOffhand();
    }

    private static List<ItemStack> generate(CommandSource source, ResourceLocation id, LootContext context, @Nullable BlockPos origin) {
        LootTable table = source.getWorld().getLootTableManager().getLootTableFromLocation(id);
        LootOrigin.set(origin);
        try {
            return table.generateLootForPools(source.getWorld().rand, context);
        } finally {
            LootOrigin.set(null);
        }
    }

    private static LootContext.Builder builder(CommandSource source) {
        LootContext.Builder builder = new LootContext.Builder(source.getWorld());
        if (source.getEntity() instanceof EntityPlayer) builder.withPlayer((EntityPlayer) source.getEntity()).withLuck(((EntityPlayer) source.getEntity()).getLuck());
        return builder;
    }

    private static List<ItemStack> lootFromTable(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "lootTable");
        return generate(source, id, builder(source).build(), new BlockPos(source.getPos()));
    }

    private static List<ItemStack> fish(CommandContext<CommandSource> context, ItemStack tool) {
        CommandSource source = context.getSource();
        ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "lootTable");
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        LootContext.Builder builder = builder(source);
        builder.withLuck(EnchantmentHelper.getEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, tool)
                + (source.getEntity() instanceof EntityPlayer ? ((EntityPlayer) source.getEntity()).getLuck() : 0.0F));
        return generate(source, id, builder.build(), pos);
    }

    private static Method lootTableMethod;

    private static List<ItemStack> kill(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        Entity entity = EntityArgument.getEntity(context, "target");
        ResourceLocation table = null;
        if (entity instanceof EntityLiving) {
            try {
                if (lootTableMethod == null) lootTableMethod = ObfuscationReflectionHelper.findMethod(EntityLiving.class, "func_184647_J", ResourceLocation.class);
                table = (ResourceLocation) lootTableMethod.invoke(entity);
            } catch (ReflectiveOperationException | RuntimeException e) {
                table = null;
            }
        }
        if (table == null) throw NO_LOOT_TABLE.create(entity.getDisplayName());
        LootContext.Builder builder = builder(source).withLootedEntity(entity);
        builder.withDamageSource(source.getEntity() instanceof EntityPlayer ? DamageSource.causePlayerDamage((EntityPlayer) source.getEntity()) : DamageSource.GENERIC);
        return generate(source, table, builder.build(), new BlockPos(entity));
    }

    /** What the block would drop mined with the tool (1.12's blocks drop by code, not by loot table). */
    private static List<ItemStack> mine(CommandContext<CommandSource> context, ItemStack tool) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        IBlockState state = source.getWorld().getBlockState(pos).getActualState(source.getWorld(), pos);
        NonNullList<ItemStack> drops = NonNullList.create();
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, tool);
        state.getBlock().getDrops(drops, source.getWorld(), pos, state, fortune);
        return new ArrayList<>(drops);
    }

    private static void feedback(CommandSource source, List<ItemStack> items) {
        if (items.size() == 1) {
            ItemStack stack = items.get(0);
            source.sendFeedback(new TextComponentTranslation("commands.drop.success.single", stack.getCount(), stack.getTextComponent()), false);
        } else {
            source.sendFeedback(new TextComponentTranslation("commands.drop.success.multiple", items.size()), false);
        }
    }

    private static int give(CommandSource source, Collection<EntityPlayerMP> players, List<ItemStack> items) {
        for (EntityPlayerMP player : players) {
            for (ItemStack stack : items) {
                ItemStack copy = stack.copy();
                if (!player.inventory.addItemStackToInventory(copy)) player.dropItem(copy, false);
            }
            player.inventoryContainer.detectAndSendChanges();
        }
        feedback(source, items);
        return items.size();
    }

    private static int spawn(CommandSource source, Vec3d pos, List<ItemStack> items) {
        for (ItemStack stack : items) {
            EntityItem item = new EntityItem(source.getWorld(), pos.x, pos.y, pos.z, stack.copy());
            item.setDefaultPickupDelay();
            source.getWorld().spawnEntity(item);
        }
        feedback(source, items);
        return items.size();
    }

    private static int insert(CommandSource source, BlockPos pos, List<ItemStack> items) throws CommandSyntaxException {
        TileEntity tile = source.getWorld().getTileEntity(pos);
        if (!(tile instanceof IInventory)) throw new CommandExceptionType.Simple(TextMessage.translate("commands.replaceitem.block.failed")).create();
        List<ItemStack> inserted = new ArrayList<>();
        for (ItemStack stack : items) {
            ItemStack left = TileEntityHopper.putStackInInventoryAllSlots(null, (IInventory) tile, stack.copy(), null);
            if (left.getCount() < stack.getCount()) {
                ItemStack put = stack.copy();
                put.setCount(stack.getCount() - left.getCount());
                inserted.add(put);
            }
        }
        feedback(source, inserted);
        return inserted.size();
    }

    private static int replaceBlock(CommandContext<CommandSource> context, List<ItemStack> items, int count) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "targetPos");
        int slot = context.getArgument("slot", Integer.class);
        TileEntity tile = source.getWorld().getTileEntity(pos);
        if (!(tile instanceof IInventory)) throw new CommandExceptionType.Simple(TextMessage.translate("commands.replaceitem.block.failed")).create();
        IInventory inventory = (IInventory) tile;
        List<ItemStack> placed = new ArrayList<>();
        for (int i = 0; i < count && slot + i < inventory.getSizeInventory(); i++) {
            ItemStack stack = i < items.size() ? items.get(i).copy() : ItemStack.EMPTY;
            inventory.setInventorySlotContents(slot + i, stack);
            if (!stack.isEmpty()) placed.add(stack);
            if (i >= items.size() - 1 && count == Integer.MAX_VALUE) break;
        }
        feedback(source, placed);
        return placed.size();
    }

    private static int replaceEntity(CommandContext<CommandSource> context, List<ItemStack> items, int count) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        int slot = context.getArgument("slot", Integer.class);
        List<ItemStack> placed = new ArrayList<>();
        for (Entity entity : EntityArgument.getEntities(context, "entities")) {
            for (int i = 0; i < count && (i < items.size() || count != Integer.MAX_VALUE); i++) {
                ItemStack stack = i < items.size() ? items.get(i).copy() : ItemStack.EMPTY;
                if (entity.replaceItemInInventory(slot + i, stack) && !stack.isEmpty()) placed.add(stack);
            }
            if (entity instanceof EntityPlayerMP) ((EntityPlayerMP) entity).inventoryContainer.detectAndSendChanges();
        }
        feedback(source, placed);
        return placed.size();
    }

    // ---- spectate ----

    private static void registerSpectate(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("spectate").requires(source -> source.hasPermissionLevel(2))
                .executes(context -> spectate(context.getSource(), null, context.getSource().asPlayer()))
                .then(argument("target", EntityArgument.entity())
                        .executes(context -> spectate(context.getSource(), EntityArgument.getEntity(context, "target"), context.getSource().asPlayer()))
                        .then(argument("player", EntityArgument.player()).executes(context -> spectate(context.getSource(),
                                EntityArgument.getEntity(context, "target"), EntityArgument.getPlayer(context, "player"))))));
    }

    private static int spectate(CommandSource source, @Nullable Entity target, EntityPlayerMP player) throws CommandSyntaxException {
        if (player == target) throw SPECTATE_SELF.create();
        if (player.interactionManager.getGameType() != GameType.SPECTATOR) throw NOT_SPECTATOR.create(player.getDisplayName());
        player.setSpectatingEntity(target);
        if (target != null) source.sendFeedback(new TextComponentTranslation("commands.spectate.success.started", target.getDisplayName()), false);
        else source.sendFeedback(new TextComponentTranslation("commands.spectate.success.stopped"), false);
        return 1;
    }

    // ---- attribute ----

    /** 1.16's attribute ids by 1.12's names. */
    private static final Map<String, String> ATTRIBUTES = new HashMap<>();

    static {
        String[] pairs = {"generic.max_health", "generic.maxHealth", "generic.follow_range", "generic.followRange", "generic.knockback_resistance",
                "generic.knockbackResistance", "generic.movement_speed", "generic.movementSpeed", "generic.flying_speed", "generic.flyingSpeed",
                "generic.attack_damage", "generic.attackDamage", "generic.attack_speed", "generic.attackSpeed", "generic.armor", "generic.armor",
                "generic.armor_toughness", "generic.armorToughness", "generic.luck", "generic.luck", "horse.jump_strength", "horse.jumpStrength",
                "zombie.spawn_reinforcements", "zombie.spawnReinforcements"};
        for (int i = 0; i < pairs.length; i += 2) ATTRIBUTES.put("minecraft:" + pairs[i], pairs[i + 1]);
    }

    private static final SuggestionProvider<CommandSource> ATTRIBUTE_IDS = (context, builder) -> IdSuggestions.suggest(new java.util.TreeSet<>(ATTRIBUTES.keySet()), builder);

    private static void registerAttribute(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("attribute").requires(source -> source.hasPermissionLevel(2))
                .then(argument("target", EntityArgument.entity()).then(argument("attribute", ResourceLocationArgument.resourceLocation()).suggests(ATTRIBUTE_IDS)
                        .then(literal("get")
                                .executes(context -> getValue(context, 1.0D))
                                .then(argument("scale", DoubleArgumentType.doubleArg()).executes(context -> getValue(context, DoubleArgumentType.getDouble(context, "scale")))))
                        .then(literal("base")
                                .then(literal("set").then(argument("value", DoubleArgumentType.doubleArg()).executes(context -> setBase(context))))
                                .then(literal("get")
                                        .executes(context -> getBase(context, 1.0D))
                                        .then(argument("scale", DoubleArgumentType.doubleArg()).executes(context -> getBase(context, DoubleArgumentType.getDouble(context, "scale"))))))
                        .then(literal("modifier")
                                .then(literal("add").then(argument("uuid", StringArgumentType.word()).then(argument("name", StringArgumentType.string())
                                        .then(argument("value", DoubleArgumentType.doubleArg())
                                                .then(literal("add").executes(context -> addModifier(context, 0)))
                                                .then(literal("multiply_base").executes(context -> addModifier(context, 1)))
                                                .then(literal("multiply").executes(context -> addModifier(context, 2)))))))
                                .then(literal("remove").then(argument("uuid", StringArgumentType.word()).executes(context -> removeModifier(context))))
                                .then(literal("value").then(literal("get").then(argument("uuid", StringArgumentType.word())
                                        .executes(context -> getModifier(context, 1.0D))
                                        .then(argument("scale", DoubleArgumentType.doubleArg()).executes(context -> getModifier(context,
                                                DoubleArgumentType.getDouble(context, "scale")))))))))));
    }

    private static ITextComponent attributeName(ResourceLocation id) {
        return new TextComponentTranslation("attribute.name." + id.getPath());
    }

    private static IAttributeInstance attribute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        if (!(entity instanceof EntityLivingBase)) throw ATTRIBUTE_ENTITY.create(entity.getName());
        ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "attribute");
        String legacy = ATTRIBUTES.getOrDefault(id.toString(), id.getPath());
        IAttributeInstance instance = ((EntityLivingBase) entity).getAttributeMap().getAttributeInstanceByName(legacy);
        if (instance == null) throw NO_ATTRIBUTE.create(entity.getName(), attributeName(id));
        return instance;
    }

    private static UUID uuid(CommandContext<CommandSource> context) throws CommandSyntaxException {
        try {
            return UUID.fromString(StringArgumentType.getString(context, "uuid"));
        } catch (IllegalArgumentException e) {
            throw new CommandExceptionType.Simple(TextMessage.translate("argument.uuid.invalid")).create();
        }
    }

    private static int getValue(CommandContext<CommandSource> context, double scale) throws CommandSyntaxException {
        IAttributeInstance instance = attribute(context);
        double value = instance.getAttributeValue();
        context.getSource().sendFeedback(new TextComponentTranslation("commands.attribute.value.get.success",
                attributeName(ResourceLocationArgument.getResourceLocation(context, "attribute")), EntityArgument.getEntity(context, "target").getName(), value), false);
        return (int) (value * scale);
    }

    private static int getBase(CommandContext<CommandSource> context, double scale) throws CommandSyntaxException {
        IAttributeInstance instance = attribute(context);
        double value = instance.getBaseValue();
        context.getSource().sendFeedback(new TextComponentTranslation("commands.attribute.base_value.get.success",
                attributeName(ResourceLocationArgument.getResourceLocation(context, "attribute")), EntityArgument.getEntity(context, "target").getName(), value), false);
        return (int) (value * scale);
    }

    private static int setBase(CommandContext<CommandSource> context) throws CommandSyntaxException {
        IAttributeInstance instance = attribute(context);
        double value = DoubleArgumentType.getDouble(context, "value");
        instance.setBaseValue(value);
        context.getSource().sendFeedback(new TextComponentTranslation("commands.attribute.base_value.set.success",
                attributeName(ResourceLocationArgument.getResourceLocation(context, "attribute")), EntityArgument.getEntity(context, "target").getName(), value), false);
        return 1;
    }

    private static int addModifier(CommandContext<CommandSource> context, int operation) throws CommandSyntaxException {
        IAttributeInstance instance = attribute(context);
        UUID uuid = uuid(context);
        if (instance.getModifier(uuid) != null) throw MODIFIER_PRESENT.create();
        AttributeModifier modifier = new AttributeModifier(uuid, StringArgumentType.getString(context, "name"), DoubleArgumentType.getDouble(context, "value"), operation);
        instance.applyModifier(modifier);
        context.getSource().sendFeedback(new TextComponentTranslation("commands.attribute.modifier.add.success", uuid.toString(),
                attributeName(ResourceLocationArgument.getResourceLocation(context, "attribute")), EntityArgument.getEntity(context, "target").getName()), false);
        return 1;
    }

    private static int removeModifier(CommandContext<CommandSource> context) throws CommandSyntaxException {
        IAttributeInstance instance = attribute(context);
        UUID uuid = uuid(context);
        if (instance.getModifier(uuid) == null) throw NO_MODIFIER.create();
        instance.removeModifier(uuid);
        context.getSource().sendFeedback(new TextComponentTranslation("commands.attribute.modifier.remove.success", uuid.toString(),
                attributeName(ResourceLocationArgument.getResourceLocation(context, "attribute")), EntityArgument.getEntity(context, "target").getName()), false);
        return 1;
    }

    private static int getModifier(CommandContext<CommandSource> context, double scale) throws CommandSyntaxException {
        IAttributeInstance instance = attribute(context);
        AttributeModifier modifier = instance.getModifier(uuid(context));
        if (modifier == null) throw NO_MODIFIER.create();
        context.getSource().sendFeedback(new TextComponentTranslation("commands.attribute.modifier.value.get.success", modifier.getID().toString(),
                attributeName(ResourceLocationArgument.getResourceLocation(context, "attribute")), EntityArgument.getEntity(context, "target").getName(),
                modifier.getAmount()), false);
        return (int) (modifier.getAmount() * scale);
    }

    // ---- locatebiome ----

    private static final SuggestionProvider<CommandSource> BIOMES = (context, builder) -> {
        List<String> ids = new ArrayList<>();
        for (ResourceLocation id : ForgeRegistries.BIOMES.getKeys()) ids.add(LegacyBiomes.toModern(id));
        return IdSuggestions.suggest(new java.util.TreeSet<>(ids), builder);
    };

    private static void registerLocateBiome(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("locatebiome").requires(source -> source.hasPermissionLevel(2))
                .then(argument("biome", ResourceLocationArgument.resourceLocation()).suggests(BIOMES)
                        .executes(context -> locateBiome(context.getSource(), ResourceLocationArgument.getResourceLocation(context, "biome")))));
    }

    /**
     * 1.16's search: outward from the source, up to 6400 blocks, for the nearest spot of the biome; read a tile of
     * 256 blocks at a time at the generator's quarter resolution, ring by ring, the nearest match of the first ring with one.
     */
    private static int locateBiome(CommandSource source, ResourceLocation id) throws CommandSyntaxException {
        Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(LegacyBiomes.toLegacy(id.toString())));
        if (biome == null || !ForgeRegistries.BIOMES.containsKey(biome.getRegistryName())) throw BIOME_INVALID.create(id.toString());
        BlockPos origin = new BlockPos(source.getPos());
        BiomeProvider provider = source.getWorld().getBiomeProvider();
        int tile = 64;
        int centerX = Math.floorDiv(origin.getX() >> 2, tile);
        int centerZ = Math.floorDiv(origin.getZ() >> 2, tile);
        BlockPos found = null;
        double foundSq = Double.MAX_VALUE;
        for (int ring = 0; ring <= 6400 / 256 + 1 && found == null; ring++) {
            for (int tx = -ring; tx <= ring; tx++) {
                for (int tz = -ring; tz <= ring; tz++) {
                    if (Math.abs(tx) != ring && Math.abs(tz) != ring) continue;
                    int fromX = (centerX + tx) * tile;
                    int fromZ = (centerZ + tz) * tile;
                    Biome[] biomes = provider.getBiomesForGeneration(null, fromX, fromZ, tile, tile);
                    for (int i = 0; i < tile * tile; i++) {
                        if (biomes[i] != biome) continue;
                        int x = (fromX + i % tile) << 2;
                        int z = (fromZ + i / tile) << 2;
                        double sq = (double) (x - origin.getX()) * (x - origin.getX()) + (double) (z - origin.getZ()) * (z - origin.getZ());
                        if (sq < foundSq && sq <= 6400.0D * 6400.0D) {
                            foundSq = sq;
                            found = new BlockPos(x, 0, z);
                        }
                    }
                }
            }
        }
        if (found == null) throw BIOME_NOT_FOUND.create(id.toString());
        int distance = MathHelper.floor(Math.sqrt(foundSq));
        ITextComponent coordinates = new TextComponentTranslation("chat.coordinates", found.getX(), "~", found.getZ());
        ITextComponent link = new TextComponentString("[").appendSibling(coordinates).appendText("]");
        link.getStyle().setColor(TextFormatting.GREEN)
                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + found.getX() + " ~ " + found.getZ()))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentTranslation("chat.coordinates.tooltip")));
        source.sendFeedback(new TextComponentTranslation("commands.locatebiome.success", new TextComponentTranslation("biome." + id.getNamespace() + "." + id.getPath()),
                link, distance), false);
        return distance;
    }

    private Commands114() {}
}
