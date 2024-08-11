package com.michaelsebero.portium.command.brigadier;

import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.context.CommandContextBuilder;
import com.michaelsebero.portium.command.brigadier.context.SuggestionContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.command.brigadier.tree.CommandNode;
import com.michaelsebero.portium.command.brigadier.tree.LiteralCommandNode;
import com.michaelsebero.portium.command.brigadier.tree.RootCommandNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and runs commands against a tree of nodes, the way 1.13's command system does. Parsing tries every relevant
 * branch and keeps the best one; running follows redirects, which may fork the command across many sources.
 */
public class CommandDispatcher<S> {

    public static final char ARGUMENT_SEPARATOR_CHAR = ' ';

    private final RootCommandNode<S> root;
    private ResultConsumer<S> consumer = (context, success, result) -> {};

    public CommandDispatcher() {
        this.root = new RootCommandNode<>();
    }

    public LiteralCommandNode<S> register(LiteralArgumentBuilder<S> command) {
        LiteralCommandNode<S> node = command.build();
        this.root.addChild(node);
        return node;
    }

    public void setConsumer(ResultConsumer<S> consumer) {
        this.consumer = consumer;
    }

    public RootCommandNode<S> getRoot() {
        return this.root;
    }

    public int execute(String input, S source) throws CommandSyntaxException {
        return this.execute(this.parse(new StringReader(input), source));
    }

    public int execute(ParseResults<S> parse) throws CommandSyntaxException {
        if (parse.getReader().canRead()) {
            if (parse.getExceptions().size() == 1) throw parse.getExceptions().values().iterator().next();
            if (parse.getContext().getRange().isEmpty()) throw BuiltInExceptions.DISPATCHER_UNKNOWN_COMMAND.createWithContext(parse.getReader());
            throw BuiltInExceptions.DISPATCHER_UNKNOWN_ARGUMENT.createWithContext(parse.getReader());
        }
        int result = 0;
        int successfulForks = 0;
        boolean forked = false;
        boolean foundCommand = false;
        String command = parse.getReader().getString();
        CommandContext<S> original = parse.getContext().build(command);
        List<CommandContext<S>> contexts = Collections.singletonList(original);
        List<CommandContext<S>> next = null;
        while (contexts != null) {
            for (CommandContext<S> context : contexts) {
                CommandContext<S> child = context.getChild();
                if (child != null) {
                    forked |= context.isForked();
                    if (!child.hasNodes()) continue;
                    foundCommand = true;
                    RedirectModifier<S> modifier = context.getRedirectModifier();
                    if (modifier == null) {
                        if (next == null) next = new ArrayList<>(1);
                        next.add(child.copyFor(context.getSource()));
                        continue;
                    }
                    try {
                        Collection<S> sources = modifier.apply(context);
                        if (sources.isEmpty()) continue;
                        if (next == null) next = new ArrayList<>(sources.size());
                        for (S source : sources) next.add(child.copyFor(source));
                    } catch (CommandSyntaxException e) {
                        this.consumer.onCommandComplete(context, false, 0);
                        if (!forked) throw e;
                    }
                } else if (context.getCommand() != null) {
                    foundCommand = true;
                    try {
                        int value = context.getCommand().run(context);
                        result += value;
                        this.consumer.onCommandComplete(context, true, value);
                        successfulForks++;
                    } catch (CommandSyntaxException e) {
                        this.consumer.onCommandComplete(context, false, 0);
                        if (!forked) throw e;
                    }
                }
            }
            contexts = next;
            next = null;
        }
        if (!foundCommand) {
            this.consumer.onCommandComplete(original, false, 0);
            throw BuiltInExceptions.DISPATCHER_UNKNOWN_COMMAND.createWithContext(parse.getReader());
        }
        return forked ? successfulForks : result;
    }

    public ParseResults<S> parse(String command, S source) {
        return this.parse(new StringReader(command), source);
    }

    public ParseResults<S> parse(StringReader command, S source) {
        CommandContextBuilder<S> context = new CommandContextBuilder<>(this, source, this.root, command.getCursor());
        return this.parseNodes(this.root, command, context);
    }

    private ParseResults<S> parseNodes(CommandNode<S> node, StringReader originalReader, CommandContextBuilder<S> contextSoFar) {
        S source = contextSoFar.getSource();
        Map<CommandNode<S>, CommandSyntaxException> errors = null;
        List<ParseResults<S>> potentials = null;
        int cursor = originalReader.getCursor();
        for (CommandNode<S> child : node.getRelevantNodes(originalReader)) {
            if (!child.canUse(source)) continue;
            CommandContextBuilder<S> context = contextSoFar.copy();
            StringReader reader = new StringReader(originalReader);
            try {
                try {
                    child.parse(reader, context);
                } catch (RuntimeException e) {
                    throw BuiltInExceptions.DISPATCHER_PARSE_EXCEPTION.createWithContext(reader, e.getMessage());
                }
                if (reader.canRead() && reader.peek() != ARGUMENT_SEPARATOR_CHAR) {
                    throw BuiltInExceptions.DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR.createWithContext(reader);
                }
            } catch (CommandSyntaxException e) {
                if (errors == null) errors = new LinkedHashMap<>();
                errors.put(child, e);
                reader.setCursor(cursor);
                continue;
            }
            context.withCommand(child.getCommand());
            if (reader.canRead(child.getRedirect() == null ? 2 : 1)) {
                reader.skip();
                if (child.getRedirect() != null) {
                    CommandContextBuilder<S> childContext = new CommandContextBuilder<>(this, source, child.getRedirect(), reader.getCursor());
                    ParseResults<S> parse = this.parseNodes(child.getRedirect(), reader, childContext);
                    context.withChild(parse.getContext());
                    return new ParseResults<>(context, parse.getReader(), parse.getExceptions());
                }
                if (potentials == null) potentials = new ArrayList<>(1);
                potentials.add(this.parseNodes(child, reader, context));
            } else {
                if (potentials == null) potentials = new ArrayList<>(1);
                potentials.add(new ParseResults<>(context, reader, Collections.emptyMap()));
            }
        }
        if (potentials != null) {
            if (potentials.size() > 1) {
                potentials.sort((a, b) -> {
                    if (!a.getReader().canRead() && b.getReader().canRead()) return -1;
                    if (a.getReader().canRead() && !b.getReader().canRead()) return 1;
                    if (a.getExceptions().isEmpty() && !b.getExceptions().isEmpty()) return -1;
                    if (!a.getExceptions().isEmpty() && b.getExceptions().isEmpty()) return 1;
                    return 0;
                });
            }
            return potentials.get(0);
        }
        return new ParseResults<>(contextSoFar, originalReader, errors == null ? Collections.emptyMap() : errors);
    }

    public Suggestions getCompletionSuggestions(ParseResults<S> parse) {
        return this.getCompletionSuggestions(parse, parse.getReader().getTotalLength());
    }

    /** Suggestions for the word at {@code cursor}, gathered from every child of the node before it. */
    public Suggestions getCompletionSuggestions(ParseResults<S> parse, int cursor) {
        CommandContextBuilder<S> context = parse.getContext();
        SuggestionContext<S> nodeBeforeCursor = context.findSuggestionContext(cursor);
        int start = Math.min(nodeBeforeCursor.startPos, cursor);
        String fullInput = parse.getReader().getString();
        String truncatedInput = fullInput.substring(0, cursor);
        List<Suggestions> all = new ArrayList<>();
        for (CommandNode<S> node : nodeBeforeCursor.parent.getChildren()) {
            try {
                all.add(node.listSuggestions(context.build(truncatedInput), new SuggestionsBuilder(truncatedInput, start)));
            } catch (CommandSyntaxException ignored) {
                // No suggestions from a branch that can't parse what's typed.
            }
        }
        return Suggestions.merge(fullInput, all);
    }

    /**
     * One compact usage line per child of {@code node}, as {@code /help} prints them: optional parts in square
     * brackets, required choices in parentheses separated by {@code |}.
     */
    public Map<CommandNode<S>, String> getSmartUsage(CommandNode<S> node, S source) {
        Map<CommandNode<S>, String> result = new LinkedHashMap<>();
        boolean optional = node.getCommand() != null;
        for (CommandNode<S> child : node.getChildren()) {
            String usage = this.getSmartUsage(child, source, optional, false);
            if (usage != null) result.put(child, usage);
        }
        return result;
    }

    private String getSmartUsage(CommandNode<S> node, S source, boolean optional, boolean deep) {
        if (!node.canUse(source)) return null;
        String self = optional ? "[" + node.getUsageText() + "]" : node.getUsageText();
        if (deep) return self;
        if (node.getRedirect() != null) {
            return self + ARGUMENT_SEPARATOR_CHAR + (node.getRedirect() == this.root ? "..." : "-> " + node.getRedirect().getUsageText());
        }
        boolean childOptional = node.getCommand() != null;
        List<CommandNode<S>> children = new ArrayList<>();
        for (CommandNode<S> child : node.getChildren()) {
            if (child.canUse(source)) children.add(child);
        }
        if (children.size() == 1) {
            String usage = this.getSmartUsage(children.get(0), source, childOptional, childOptional);
            return usage == null ? self : self + ARGUMENT_SEPARATOR_CHAR + usage;
        }
        if (children.size() > 1) {
            Set<String> usages = new LinkedHashSet<>();
            for (CommandNode<S> child : children) {
                String usage = this.getSmartUsage(child, source, childOptional, true);
                if (usage != null) usages.add(usage);
            }
            if (usages.size() == 1) {
                String usage = usages.iterator().next();
                return self + ARGUMENT_SEPARATOR_CHAR + (childOptional ? "[" + usage + "]" : usage);
            }
            if (usages.size() > 1) {
                StringBuilder choices = new StringBuilder(childOptional ? "[" : "(");
                for (int i = 0; i < children.size(); i++) {
                    if (i > 0) choices.append('|');
                    choices.append(children.get(i).getUsageText());
                }
                return self + ARGUMENT_SEPARATOR_CHAR + choices.append(childOptional ? "]" : ")");
            }
        }
        return self;
    }
}
