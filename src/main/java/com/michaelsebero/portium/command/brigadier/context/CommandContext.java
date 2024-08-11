package com.michaelsebero.portium.command.brigadier.context;

import com.michaelsebero.portium.command.brigadier.Command;
import com.michaelsebero.portium.command.brigadier.RedirectModifier;
import com.michaelsebero.portium.command.brigadier.tree.CommandNode;

import java.util.List;
import java.util.Map;

/** A parsed command: its source, the arguments read, and the command (or child context, for redirects) to run. */
public class CommandContext<S> {

    private final S source;
    private final String input;
    private final Command<S> command;
    private final Map<String, ParsedArgument<S, ?>> arguments;
    private final CommandNode<S> rootNode;
    private final List<ParsedCommandNode<S>> nodes;
    private final StringRange range;
    private final CommandContext<S> child;
    private final RedirectModifier<S> modifier;
    private final boolean forks;

    public CommandContext(S source, String input, Map<String, ParsedArgument<S, ?>> arguments, Command<S> command, CommandNode<S> rootNode,
                          List<ParsedCommandNode<S>> nodes, StringRange range, CommandContext<S> child, RedirectModifier<S> modifier, boolean forks) {
        this.source = source;
        this.input = input;
        this.arguments = arguments;
        this.command = command;
        this.rootNode = rootNode;
        this.nodes = nodes;
        this.range = range;
        this.child = child;
        this.modifier = modifier;
        this.forks = forks;
    }

    public CommandContext<S> copyFor(S source) {
        if (this.source == source) return this;
        return new CommandContext<>(source, this.input, this.arguments, this.command, this.rootNode, this.nodes, this.range, this.child, this.modifier, this.forks);
    }

    public CommandContext<S> getChild() {
        return this.child;
    }

    public CommandContext<S> getLastChild() {
        CommandContext<S> result = this;
        while (result.getChild() != null) result = result.getChild();
        return result;
    }

    public Command<S> getCommand() {
        return this.command;
    }

    public S getSource() {
        return this.source;
    }

    @SuppressWarnings("unchecked")
    public <V> V getArgument(String name, Class<V> type) {
        ParsedArgument<S, ?> argument = this.arguments.get(name);
        if (argument == null) throw new IllegalArgumentException("No such argument '" + name + "' exists on this command");
        Object result = argument.getResult();
        if (PRIMITIVE_TO_WRAPPER.getOrDefault(type, type).isAssignableFrom(result.getClass())) return (V) result;
        throw new IllegalArgumentException("Argument '" + name + "' is defined as " + result.getClass().getSimpleName() + ", not " + type);
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new java.util.HashMap<>();

    static {
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
    }

    public boolean hasArgument(String name) {
        return this.arguments.containsKey(name);
    }

    public RedirectModifier<S> getRedirectModifier() {
        return this.modifier;
    }

    public StringRange getRange() {
        return this.range;
    }

    public String getInput() {
        return this.input;
    }

    public CommandNode<S> getRootNode() {
        return this.rootNode;
    }

    public List<ParsedCommandNode<S>> getNodes() {
        return this.nodes;
    }

    public boolean hasNodes() {
        return !this.nodes.isEmpty();
    }

    public boolean isForked() {
        return this.forks;
    }
}
