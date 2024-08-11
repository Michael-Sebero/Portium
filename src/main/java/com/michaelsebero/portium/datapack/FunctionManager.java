package com.michaelsebero.portium.datapack;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.EntityAnchor;
import com.michaelsebero.portium.command.Vec2;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Runs data pack functions as 1.13 does: commands go through a queue so nested functions run in place, at most
 * {@code maxCommandChainLength} commands per call; {@code #minecraft:tick} runs every tick and {@code #minecraft:load}
 * once after each load.
 */
public class FunctionManager {

    public static final ResourceLocation TICK_TAG = new ResourceLocation("tick");
    public static final ResourceLocation LOAD_TAG = new ResourceLocation("load");

    private final MinecraftServer server;
    private final CommandDispatcher<CommandSource> dispatcher;
    private Map<ResourceLocation, CommandFunction> functions = new HashMap<>();
    private final ArrayDeque<QueuedCommand> queue = new ArrayDeque<>();
    private boolean executing;
    private List<CommandFunction> tickFunctions = Collections.emptyList();
    private List<CommandFunction> loadFunctions = Collections.emptyList();
    private boolean justLoaded;

    public FunctionManager(MinecraftServer server, CommandDispatcher<CommandSource> dispatcher) {
        this.server = server;
        this.dispatcher = dispatcher;
    }

    public CommandDispatcher<CommandSource> getDispatcher() {
        return this.dispatcher;
    }

    public CommandFunction get(ResourceLocation id) {
        return this.functions.get(id);
    }

    public Collection<ResourceLocation> getIds() {
        return this.functions.keySet();
    }

    /** The source functions run as: the server, at the world spawn, with permission level 2 and no feedback. */
    public CommandSource getSource() {
        WorldServer overworld = this.server.getWorld(0);
        Vec3d spawn = new Vec3d(overworld.getSpawnPoint());
        return new CommandSource(this.server, spawn, Vec2.ZERO, overworld, 2, "Server", new TextComponentString("Server"), this.server, null,
                true, (context, success, result) -> {}, EntityAnchor.FEET);
    }

    public void load(List<DataPack> packs, TagManager tags) {
        Map<ResourceLocation, CommandFunction> loaded = new HashMap<>();
        CommandSource source = this.getSource();
        for (DataPack pack : packs) {
            for (ResourceLocation id : pack.list("functions", ".mcfunction")) {
                try (Reader reader = pack.openData(id, "functions", ".mcfunction")) {
                    if (reader == null) continue;
                    List<String> lines = new BufferedReader(reader).lines().collect(Collectors.toList());
                    loaded.put(id, CommandFunction.parse(id, lines, this.dispatcher, source));
                } catch (IOException | IllegalArgumentException e) {
                    Portium.LOGGER.error("Failed to load function {}", id, e);
                }
            }
        }
        this.functions = loaded;
        this.tickFunctions = this.resolveTag(tags, TICK_TAG);
        this.loadFunctions = this.resolveTag(tags, LOAD_TAG);
        this.justLoaded = true;
        Portium.LOGGER.info("Loaded {} data pack functions", loaded.size());
    }

    private List<CommandFunction> resolveTag(TagManager tags, ResourceLocation id) {
        Tag<ResourceLocation> tag = tags.getFunctions().get(id);
        if (tag == null) return Collections.emptyList();
        List<CommandFunction> result = new ArrayList<>();
        for (ResourceLocation function : tag.getValues()) {
            CommandFunction found = this.functions.get(function);
            if (found == null) Portium.LOGGER.error("The function tag {} lists the missing function {}", id, function);
            else result.add(found);
        }
        return result;
    }

    public void tick() {
        if (this.justLoaded) {
            this.justLoaded = false;
            this.runAll(this.loadFunctions);
        }
        this.runAll(this.tickFunctions);
    }

    private void runAll(List<CommandFunction> functions) {
        for (CommandFunction function : functions) this.execute(function, this.getSource());
    }

    public int getMaxCommandChainLength() {
        return Math.max(1, this.server.worlds[0].getGameRules().getInt("maxCommandChainLength"));
    }

    /** Runs a function, returning how many commands ran; from inside another function it is queued in place. */
    public int execute(CommandFunction function, CommandSource source) {
        int maxChain = this.getMaxCommandChainLength();
        if (this.executing) {
            if (this.queue.size() < maxChain) this.queue.addFirst(new QueuedCommand(this, source, new CommandFunction.FunctionEntry(function)));
            return 0;
        }
        try {
            this.executing = true;
            int count = 0;
            CommandFunction.Entry[] entries = function.getEntries();
            for (int i = entries.length - 1; i >= 0; --i) this.queue.push(new QueuedCommand(this, source, entries[i]));
            while (!this.queue.isEmpty()) {
                this.queue.removeFirst().execute(this.queue, maxChain);
                if (++count >= maxChain) return count;
            }
            return count;
        } finally {
            this.queue.clear();
            this.executing = false;
        }
    }

    static final class QueuedCommand {

        private final FunctionManager manager;
        private final CommandSource source;
        private final CommandFunction.Entry entry;

        QueuedCommand(FunctionManager manager, CommandSource source, CommandFunction.Entry entry) {
            this.manager = manager;
            this.source = source;
            this.entry = entry;
        }

        void execute(ArrayDeque<QueuedCommand> queue, int maxChain) {
            try {
                this.entry.execute(this.manager, this.source, queue, maxChain);
            } catch (CommandSyntaxException ignored) {
                // A failing command just doesn't run, as in 1.13.
            }
        }

        @Override
        public String toString() {
            return this.entry.toString();
        }
    }
}
