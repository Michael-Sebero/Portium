package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.arguments.selector.EntityTypes;
import com.michaelsebero.portium.command.brigadier.CommandExceptionType;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Ids of registry entries: status effects, enchantments, summonable entities and dimensions. 1.13 names for things
 * Portium adds ({@code minecraft:slow_falling}) find Portium's entries.
 */
public final class RegistryArguments {

    public static ArgumentType<Potion> effect() {
        return registry(ForgeRegistries.POTIONS, CommandExceptions.EFFECT_UNKNOWN);
    }

    public static ArgumentType<Enchantment> enchantment() {
        return registry(ForgeRegistries.ENCHANTMENTS, CommandExceptions.ENCHANTMENT_UNKNOWN);
    }

    private static <T extends IForgeRegistryEntry<T>> ArgumentType<T> registry(IForgeRegistry<T> registry, CommandExceptionType.Dynamic unknown) {
        return new ArgumentType<T>() {
            @Override
            public T parse(StringReader reader) throws CommandSyntaxException {
                ResourceLocation id = ResourceLocationArgument.read(reader);
                T value = registry.getValue(id);
                if (value == null && "minecraft".equals(id.getNamespace())) value = registry.getValue(new ResourceLocation(Tags.MOD_ID, id.getPath()));
                if (value == null) throw unknown.create(id.toString());
                return value;
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                return IdSuggestions.suggest(modernize(registry.getKeys(), id -> id), builder);
            }
        };
    }

    private static <T> List<String> modernize(Iterable<T> values, Function<T, ResourceLocation> id) {
        List<String> names = new ArrayList<>();
        for (T value : values) {
            ResourceLocation key = id.apply(value);
            names.add(Tags.MOD_ID.equals(key.getNamespace()) ? "minecraft:" + key.getPath() : key.toString());
        }
        return names;
    }

    /** An entity type that {@code summon} can create (not players). Yields the 1.12 registry id. */
    public static ArgumentType<ResourceLocation> summonable() {
        return new ArgumentType<ResourceLocation>() {
            @Override
            public ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
                ResourceLocation id = ResourceLocationArgument.read(reader);
                ResourceLocation legacy = EntityTypes.toLegacy(id);
                if (legacy == null || EntityTypes.PLAYER.equals(legacy)) throw CommandExceptions.SUMMON_UNKNOWN.create(id.toString());
                return legacy;
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                List<ResourceLocation> ids = EntityTypes.modernIds();
                ids.remove(EntityTypes.PLAYER);
                return IdSuggestions.suggest(ids, builder);
            }
        };
    }

    /** {@code minecraft:overworld}, {@code minecraft:the_nether}, {@code minecraft:the_end}, or a modded dimension. */
    public static ArgumentType<DimensionType> dimension() {
        return new ArgumentType<DimensionType>() {
            @Override
            public DimensionType parse(StringReader reader) throws CommandSyntaxException {
                ResourceLocation id = ResourceLocationArgument.read(reader);
                for (DimensionType type : DimensionType.values()) {
                    if (type.getName().equals(id.getPath())) return type;
                }
                throw CommandExceptions.DIMENSION_INVALID.create(id.toString());
            }

            @Override
            public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
                List<String> names = new ArrayList<>();
                for (DimensionType type : DimensionType.values()) names.add("minecraft:" + type.getName());
                return IdSuggestions.suggest(names, builder);
            }
        };
    }

    private RegistryArguments() {}
}
