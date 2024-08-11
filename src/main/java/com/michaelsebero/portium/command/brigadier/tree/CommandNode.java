package com.michaelsebero.portium.command.brigadier.tree;

import com.michaelsebero.portium.command.brigadier.Command;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.RedirectModifier;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.builder.ArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.context.CommandContextBuilder;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/** A node in the command tree: a literal word or a typed argument, with children, a command, or a redirect. */
public abstract class CommandNode<S> implements Comparable<CommandNode<S>> {

    private Map<String, CommandNode<S>> children = new LinkedHashMap<>();
    private final Map<String, LiteralCommandNode<S>> literals = new LinkedHashMap<>();
    private final Map<String, ArgumentCommandNode<S, ?>> arguments = new LinkedHashMap<>();
    private final Predicate<S> requirement;
    private final CommandNode<S> redirect;
    private final RedirectModifier<S> modifier;
    private final boolean forks;
    private Command<S> command;

    protected CommandNode(Command<S> command, Predicate<S> requirement, CommandNode<S> redirect, RedirectModifier<S> modifier, boolean forks) {
        this.command = command;
        this.requirement = requirement;
        this.redirect = redirect;
        this.modifier = modifier;
        this.forks = forks;
    }

    public Command<S> getCommand() {
        return this.command;
    }

    public Collection<CommandNode<S>> getChildren() {
        return this.children.values();
    }

    public CommandNode<S> getChild(String name) {
        return this.children.get(name);
    }

    public CommandNode<S> getRedirect() {
        return this.redirect;
    }

    public RedirectModifier<S> getRedirectModifier() {
        return this.modifier;
    }

    public boolean canUse(S source) {
        return this.requirement.test(source);
    }

    public Predicate<S> getRequirement() {
        return this.requirement;
    }

    public boolean isFork() {
        return this.forks;
    }

    /** Adds a child, merging it into an existing child of the same name. */
    /** Removes a child by name, for a command replaced as a whole. */
    public void removeChild(String name) {
        this.children.remove(name);
        this.literals.remove(name);
        this.arguments.remove(name);
    }

    public void addChild(CommandNode<S> node) {
        if (node instanceof RootCommandNode) throw new UnsupportedOperationException("Cannot add a RootCommandNode as a child to any other CommandNode");
        CommandNode<S> existing = this.children.get(node.getName());
        if (existing != null) {
            if (node.getCommand() != null) existing.command = node.getCommand();
            for (CommandNode<S> grandchild : node.getChildren()) existing.addChild(grandchild);
            return;
        }
        this.children.put(node.getName(), node);
        if (node instanceof LiteralCommandNode) this.literals.put(node.getName(), (LiteralCommandNode<S>) node);
        else if (node instanceof ArgumentCommandNode) this.arguments.put(node.getName(), (ArgumentCommandNode<S, ?>) node);
        Map<String, CommandNode<S>> sorted = new LinkedHashMap<>();
        this.children.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        this.children = sorted;
    }

    /** The children worth trying for the next word: the literal it names if there is one, otherwise the arguments. */
    public Collection<? extends CommandNode<S>> getRelevantNodes(StringReader input) {
        if (this.literals.isEmpty()) return this.arguments.values();
        int cursor = input.getCursor();
        while (input.canRead() && input.peek() != ' ') input.skip();
        String text = input.getString().substring(cursor, input.getCursor());
        input.setCursor(cursor);
        LiteralCommandNode<S> literal = this.literals.get(text);
        return literal != null ? Collections.singleton(literal) : this.arguments.values();
    }

    @Override
    public int compareTo(CommandNode<S> o) {
        if (this instanceof LiteralCommandNode == o instanceof LiteralCommandNode) return this.getSortedKey().compareTo(o.getSortedKey());
        return o instanceof LiteralCommandNode ? 1 : -1;
    }

    protected abstract boolean isValidInput(String input);

    public abstract String getName();

    public abstract String getUsageText();

    public abstract void parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException;

    public abstract Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException;

    public abstract ArgumentBuilder<S, ?> createBuilder();

    protected abstract String getSortedKey();

    public abstract Collection<String> getExamples();
}
