package com.michaelsebero.portium.command;

import com.michaelsebero.portium.command.arguments.selector.EntitySelector;
import com.michaelsebero.portium.command.arguments.selector.EntitySelectorParser;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentKeybind;
import net.minecraft.util.text.TextComponentScore;
import net.minecraft.util.text.TextComponentSelector;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Fills in {@code selector} and {@code score} text with 1.13 selector syntax before the text is sent (1.12's own
 * resolver only understands 1.12 selectors).
 */
public final class ComponentResolver {

    public static ITextComponent resolve(CommandSource source, ITextComponent component, @Nullable Entity entity) throws CommandSyntaxException {
        return resolve(source, component, entity, 0);
    }

    private static ITextComponent resolve(CommandSource source, ITextComponent component, @Nullable Entity entity, int depth) throws CommandSyntaxException {
        if (depth > 100) return component;
        ITextComponent result;
        if (component instanceof TextComponentScore) {
            result = new TextComponentString(score(source, (TextComponentScore) component, entity));
        } else if (component instanceof TextComponentSelector) {
            result = selector(source, ((TextComponentSelector) component).getSelector());
        } else if (component instanceof TextComponentTranslation) {
            TextComponentTranslation translation = (TextComponentTranslation) component;
            Object[] args = translation.getFormatArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof ITextComponent) args[i] = resolve(source, (ITextComponent) args[i], entity, depth + 1);
            }
            result = new TextComponentTranslation(translation.getKey(), args);
        } else if (component instanceof TextComponentKeybind) {
            result = new TextComponentKeybind(((TextComponentKeybind) component).getKeybind());
        } else if (component instanceof TextComponentString) {
            result = new TextComponentString(((TextComponentString) component).getText());
        } else {
            result = new TextComponentString(component.getUnformattedComponentText());
        }
        result.setStyle(component.getStyle().createShallowCopy());
        for (ITextComponent sibling : component.getSiblings()) result.appendSibling(resolve(source, sibling, entity, depth + 1));
        return result;
    }

    private static String score(CommandSource source, TextComponentScore component, @Nullable Entity entity) throws CommandSyntaxException {
        String name = component.getName();
        if ("*".equals(name)) {
            if (entity == null) return "";
            name = scoreHolder(entity);
        } else if (name.startsWith("@")) {
            List<? extends Entity> entities = new EntitySelectorParser(new StringReader(name)).parse().select(source);
            if (entities.isEmpty()) return "";
            if (entities.size() != 1) throw CommandExceptions.ENTITY_TOO_MANY.create();
            name = scoreHolder(entities.get(0));
        }
        Scoreboard scoreboard = source.getServer().getWorld(0).getScoreboard();
        ScoreObjective objective = scoreboard.getObjective(component.getObjective());
        if (objective == null || !scoreboard.entityHasObjective(name, objective)) return "";
        return Integer.toString(scoreboard.getOrCreateScore(name, objective).getScorePoints());
    }

    private static ITextComponent selector(CommandSource source, String text) throws CommandSyntaxException {
        EntitySelector selector = new EntitySelectorParser(new StringReader(text)).parse();
        List<? extends Entity> entities = selector.select(source);
        TextComponentString joined = new TextComponentString("");
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) joined.appendText(", ");
            joined.appendSibling(entities.get(i).getDisplayName());
        }
        return joined;
    }

    /** The name an entity is listed under on the scoreboard: a player's name, or any other entity's UUID. */
    public static String scoreHolder(Entity entity) {
        return entity instanceof EntityPlayerMP ? entity.getName() : entity.getCachedUniqueIdString();
    }

    private ComponentResolver() {}
}
