package com.michaelsebero.portium.datapack;

import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.LegacyCommands;
import com.michaelsebero.portium.command.brigadier.BuiltInExceptions;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.ParseResults;
import com.michaelsebero.portium.command.brigadier.StringReader;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code .mcfunction}: commands parsed once when the pack loads and run in order, as in 1.13. Commands from other
 * mods (which only know 1.12's command system) are passed to it as written.
 */
public final class CommandFunction {

    private final ResourceLocation id;
    private final Entry[] entries;

    private CommandFunction(ResourceLocation id, Entry[] entries) {
        this.id = id;
        this.entries = entries;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    Entry[] getEntries() {
        return this.entries;
    }

    /** Parses the lines of a function file; throws with 1.13's message for the first bad line. */
    public static CommandFunction parse(ResourceLocation id, List<String> lines, CommandDispatcher<CommandSource> dispatcher, CommandSource source) {
        List<Entry> entries = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            int lineNumber = i + 1;
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            StringReader reader = new StringReader(line);
            if (reader.peek() == '/') {
                reader.skip();
                if (reader.canRead() && reader.peek() == '/') {
                    throw new IllegalArgumentException("Unknown or invalid command '" + line + "' on line " + lineNumber + " (if you intended to make a comment, use '#' not '//')");
                }
                String name = reader.readUnquotedString();
                throw new IllegalArgumentException("Unknown or invalid command '" + line + "' on line " + lineNumber + " (did you mean '" + name + "'? Do not use a preceding forwards slash.)");
            }
            String root = line.split(" ", 2)[0];
            if (dispatcher.getRoot().getChild(root) == null && LegacyCommands.isLegacyCommand(source.getServer(), root)) {
                entries.add(new LegacyEntry(line));
                continue;
            }
            try {
                ParseResults<CommandSource> parse = dispatcher.parse(reader, source);
                if (parse.getReader().canRead()) {
                    if (parse.getExceptions().size() == 1) throw parse.getExceptions().values().iterator().next();
                    if (parse.getContext().getRange().isEmpty()) throw BuiltInExceptions.DISPATCHER_UNKNOWN_COMMAND.createWithContext(parse.getReader());
                    throw BuiltInExceptions.DISPATCHER_UNKNOWN_ARGUMENT.createWithContext(parse.getReader());
                }
                entries.add(new CommandEntry(parse));
            } catch (CommandSyntaxException e) {
                throw new IllegalArgumentException("Whilst parsing command on line " + lineNumber + ": " + e.getMessage());
            }
        }
        return new CommandFunction(id, entries.toArray(new Entry[0]));
    }

    interface Entry {

        void execute(FunctionManager manager, CommandSource source, ArrayDeque<FunctionManager.QueuedCommand> queue, int maxChain) throws CommandSyntaxException;
    }

    private static final class CommandEntry implements Entry {

        private final ParseResults<CommandSource> parse;

        CommandEntry(ParseResults<CommandSource> parse) {
            this.parse = parse;
        }

        @Override
        public void execute(FunctionManager manager, CommandSource source, ArrayDeque<FunctionManager.QueuedCommand> queue, int maxChain) throws CommandSyntaxException {
            manager.getDispatcher().execute(new ParseResults<>(this.parse.getContext().withSource(source), this.parse.getReader(), this.parse.getExceptions()));
        }

        @Override
        public String toString() {
            return this.parse.getReader().getString();
        }
    }

    private static final class LegacyEntry implements Entry {

        private final String command;

        LegacyEntry(String command) {
            this.command = command;
        }

        @Override
        public void execute(FunctionManager manager, CommandSource source, ArrayDeque<FunctionManager.QueuedCommand> queue, int maxChain) {
            LegacyCommands.execute(source, this.command);
        }

        @Override
        public String toString() {
            return this.command;
        }
    }

    /** {@code function} inside a function: its commands are queued in place, within the chain limit. */
    static final class FunctionEntry implements Entry {

        private final CommandFunction function;

        FunctionEntry(CommandFunction function) {
            this.function = function;
        }

        @Override
        public void execute(FunctionManager manager, CommandSource source, ArrayDeque<FunctionManager.QueuedCommand> queue, int maxChain) {
            Entry[] entries = this.function.getEntries();
            int room = Math.min(entries.length, maxChain - queue.size());
            for (int i = room - 1; i >= 0; --i) queue.addFirst(new FunctionManager.QueuedCommand(manager, source, entries[i]));
        }

        @Override
        public String toString() {
            return "function " + this.function.getId();
        }
    }
}
