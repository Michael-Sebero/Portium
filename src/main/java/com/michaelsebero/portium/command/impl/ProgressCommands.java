package com.michaelsebero.portium.command.impl;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.CommandSource;
import com.michaelsebero.portium.command.TextMessage;
import com.michaelsebero.portium.command.arguments.EntityArgument;
import com.michaelsebero.portium.command.arguments.IdSuggestions;
import com.michaelsebero.portium.command.arguments.ResourceLocationArgument;
import com.michaelsebero.portium.command.brigadier.CommandDispatcher;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.arguments.StringArgumentType;
import com.michaelsebero.portium.command.brigadier.builder.LiteralArgumentBuilder;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.michaelsebero.portium.command.impl.Commands.argument;
import static com.michaelsebero.portium.command.impl.Commands.literal;

/** {@code advancement} and {@code recipe} (1.13). */
public final class ProgressCommands {

    private static final CommandExceptionType.Simple RECIPE_GIVE_FAILED = new CommandExceptionType.Simple(TextMessage.translate("commands.recipe.give.failed"));
    private static final CommandExceptionType.Simple RECIPE_TAKE_FAILED = new CommandExceptionType.Simple(TextMessage.translate("commands.recipe.take.failed"));

    private static final SuggestionProvider<CommandSource> ADVANCEMENTS = (context, builder) -> {
        List<ResourceLocation> ids = new ArrayList<>();
        for (Advancement advancement : context.getSource().getServer().getAdvancementManager().getAdvancements()) ids.add(advancement.getId());
        return IdSuggestions.suggest(ids, builder);
    };

    private static final SuggestionProvider<CommandSource> CRITERIA = (context, builder) ->
            IdSuggestions.suggest(getAdvancement(context).getCriteria().keySet(), builder);

    private static final SuggestionProvider<CommandSource> RECIPES = (context, builder) -> IdSuggestions.suggest(CraftingManager.REGISTRY.getKeys(), builder);

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("advancement").requires(source -> source.hasPermissionLevel(2))
                .then(action("grant", Action.GRANT))
                .then(action("revoke", Action.REVOKE)));

        dispatcher.register(literal("recipe").requires(source -> source.hasPermissionLevel(2))
                .then(literal("give").then(argument("targets", EntityArgument.players())
                        .then(argument("recipe", ResourceLocationArgument.resourceLocation()).suggests(RECIPES).executes(context ->
                                giveRecipes(context.getSource(), EntityArgument.getPlayers(context, "targets"), singleRecipe(context))))
                        .then(literal("*").executes(context -> giveRecipes(context.getSource(), EntityArgument.getPlayers(context, "targets"), allRecipes())))))
                .then(literal("take").then(argument("targets", EntityArgument.players())
                        .then(argument("recipe", ResourceLocationArgument.resourceLocation()).suggests(RECIPES).executes(context ->
                                takeRecipes(context.getSource(), EntityArgument.getPlayers(context, "targets"), singleRecipe(context))))
                        .then(literal("*").executes(context -> takeRecipes(context.getSource(), EntityArgument.getPlayers(context, "targets"), allRecipes()))))));
    }

    // ---- advancement ----

    private enum Action {
        GRANT, REVOKE;

        String prefix() {
            return "commands.advancement." + this.name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Completes (or clears) every criterion, returning whether anything changed. */
        boolean apply(EntityPlayerMP player, Advancement advancement) {
            PlayerAdvancements progress = player.getAdvancements();
            AdvancementProgress state = progress.getProgress(advancement);
            if (this == GRANT ? state.isDone() : !state.hasProgress()) return false;
            for (String criterion : this == GRANT ? state.getRemaningCriteria() : state.getCompletedCriteria()) this.apply(player, advancement, criterion);
            return true;
        }

        boolean apply(EntityPlayerMP player, Advancement advancement, String criterion) {
            return this == GRANT ? player.getAdvancements().grantCriterion(advancement, criterion) : player.getAdvancements().revokeCriterion(advancement, criterion);
        }
    }

    /** Which advancements around the named one {@code only}, {@code from}, {@code through} and {@code until} cover. */
    private enum Mode {
        ONLY(false, false), THROUGH(true, true), FROM(false, true), UNTIL(true, false);

        final boolean parents;
        final boolean children;

        Mode(boolean parents, boolean children) {
            this.parents = parents;
            this.children = children;
        }
    }

    private static LiteralArgumentBuilder<CommandSource> action(String name, Action action) {
        LiteralArgumentBuilder<CommandSource> builder = literal(name);
        builder.then(argument("targets", EntityArgument.players())
                .then(literal("only").then(argument("advancement", ResourceLocationArgument.resourceLocation()).suggests(ADVANCEMENTS)
                        .executes(context -> applyAll(context.getSource(), EntityArgument.getPlayers(context, "targets"), action, matching(getAdvancement(context), Mode.ONLY)))
                        .then(argument("criterion", StringArgumentType.greedyString()).suggests(CRITERIA).executes(context ->
                                applyCriterion(context.getSource(), EntityArgument.getPlayers(context, "targets"), action, getAdvancement(context),
                                        StringArgumentType.getString(context, "criterion"))))))
                .then(mode("from", action, Mode.FROM))
                .then(mode("until", action, Mode.UNTIL))
                .then(mode("through", action, Mode.THROUGH))
                .then(literal("everything").executes(context -> {
                    List<Advancement> all = new ArrayList<>();
                    for (Advancement advancement : context.getSource().getServer().getAdvancementManager().getAdvancements()) all.add(advancement);
                    return applyAll(context.getSource(), EntityArgument.getPlayers(context, "targets"), action, all);
                })));
        return builder;
    }

    private static LiteralArgumentBuilder<CommandSource> mode(String name, Action action, Mode mode) {
        return literal(name).then(argument("advancement", ResourceLocationArgument.resourceLocation()).suggests(ADVANCEMENTS).executes(context ->
                applyAll(context.getSource(), EntityArgument.getPlayers(context, "targets"), action, matching(getAdvancement(context), mode))));
    }

    private static Advancement getAdvancement(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "advancement");
        Advancement advancement = context.getSource().getServer().getAdvancementManager().getAdvancement(id);
        if (advancement == null) throw CommandExceptions.ADVANCEMENT_UNKNOWN.create(id);
        return advancement;
    }

    private static List<Advancement> matching(Advancement advancement, Mode mode) {
        List<Advancement> result = new ArrayList<>();
        if (mode.parents) {
            for (Advancement parent = advancement.getParent(); parent != null; parent = parent.getParent()) result.add(parent);
        }
        result.add(advancement);
        if (mode.children) addChildren(advancement, result);
        return result;
    }

    private static void addChildren(Advancement advancement, List<Advancement> result) {
        for (Advancement child : advancement.getChildren()) {
            result.add(child);
            addChildren(child, result);
        }
    }

    private static int applyAll(CommandSource source, Collection<EntityPlayerMP> targets, Action action, List<Advancement> advancements) throws CommandSyntaxException {
        int changed = 0;
        for (EntityPlayerMP player : targets) {
            for (Advancement advancement : advancements) {
                if (action.apply(player, advancement)) changed++;
            }
        }
        String key = action.prefix() + (advancements.size() == 1 ? ".one" : ".many") + (targets.size() == 1 ? ".to.one" : ".to.many");
        Object what = advancements.size() == 1 ? advancements.get(0).getDisplayText() : advancements.size();
        Object who = targets.size() == 1 ? targets.iterator().next().getDisplayName() : targets.size();
        if (changed == 0) throw CommandExceptions.failure(key + ".failure", what, who);
        source.sendFeedback(new TextComponentTranslation(key + ".success", what, who), true);
        return changed;
    }

    private static int applyCriterion(CommandSource source, Collection<EntityPlayerMP> targets, Action action, Advancement advancement, String criterion)
            throws CommandSyntaxException {
        if (!advancement.getCriteria().containsKey(criterion)) {
            throw CommandExceptions.failure("commands.advancement.criterionNotFound", advancement.getDisplayText(), criterion);
        }
        int changed = 0;
        for (EntityPlayerMP player : targets) {
            if (action.apply(player, advancement, criterion)) changed++;
        }
        String key = action.prefix() + ".criterion" + (targets.size() == 1 ? ".to.one" : ".to.many");
        Object who = targets.size() == 1 ? targets.iterator().next().getDisplayName() : targets.size();
        if (changed == 0) throw CommandExceptions.failure(key + ".failure", criterion, advancement.getDisplayText(), who);
        source.sendFeedback(new TextComponentTranslation(key + ".success", criterion, advancement.getDisplayText(), who), true);
        return changed;
    }

    // ---- recipe ----

    private static List<IRecipe> singleRecipe(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getResourceLocation(context, "recipe");
        IRecipe recipe = CraftingManager.getRecipe(id);
        if (recipe == null) throw CommandExceptions.RECIPE_UNKNOWN.create(id);
        List<IRecipe> recipes = new ArrayList<>();
        recipes.add(recipe);
        return recipes;
    }

    private static List<IRecipe> allRecipes() {
        List<IRecipe> recipes = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.REGISTRY) recipes.add(recipe);
        return recipes;
    }

    private static int giveRecipes(CommandSource source, Collection<EntityPlayerMP> targets, List<IRecipe> recipes) throws CommandSyntaxException {
        int changed = 0;
        for (EntityPlayerMP player : targets) {
            List<IRecipe> locked = new ArrayList<>();
            for (IRecipe recipe : recipes) {
                if (!recipe.isDynamic() && !player.getRecipeBook().isUnlocked(recipe)) locked.add(recipe);
            }
            player.unlockRecipes(locked);
            changed += locked.size();
        }
        if (changed == 0) throw RECIPE_GIVE_FAILED.create();
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.recipe.give.success.single", recipes.size(), targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.recipe.give.success.multiple", recipes.size(), targets.size()), true);
        return changed;
    }

    private static int takeRecipes(CommandSource source, Collection<EntityPlayerMP> targets, List<IRecipe> recipes) throws CommandSyntaxException {
        int changed = 0;
        for (EntityPlayerMP player : targets) {
            List<IRecipe> unlocked = new ArrayList<>();
            for (IRecipe recipe : recipes) {
                if (player.getRecipeBook().isUnlocked(recipe)) unlocked.add(recipe);
            }
            player.resetRecipes(unlocked);
            changed += unlocked.size();
        }
        if (changed == 0) throw RECIPE_TAKE_FAILED.create();
        if (targets.size() == 1) source.sendFeedback(new TextComponentTranslation("commands.recipe.take.success.single", recipes.size(), targets.iterator().next().getDisplayName()), true);
        else source.sendFeedback(new TextComponentTranslation("commands.recipe.take.success.multiple", recipes.size(), targets.size()), true);
        return changed;
    }

    private ProgressCommands() {}
}
