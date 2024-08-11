package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.compat.LegacyBlock;
import com.michaelsebero.portium.compat.LegacyBlockStates;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import com.michaelsebero.portium.world.waterlog.Waterlogging;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * A block test: a 1.13 block name or {@code #tag}, optionally with properties and NBT the block must have, as used by
 * {@code execute if block}, {@code fill ... replace} and {@code clone ... filtered}. Tags are looked up when tested,
 * so a {@code /reload} takes effect.
 */
public class BlockPredicateArgument implements ArgumentType<BlockPredicateArgument.Result> {

    public interface Result {

        boolean test(World world, BlockPos pos) throws CommandSyntaxException;
    }

    public static BlockPredicateArgument blockPredicate() {
        return new BlockPredicateArgument();
    }

    public static Result getBlockPredicate(CommandContext<?> context, String name) {
        return context.getArgument(name, Result.class);
    }

    @Override
    public Result parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        BlockStateParser parser = new BlockStateParser(reader, true).parse(true);
        ResourceLocation id = parser.getId();
        Map<String, String> properties = parser.getProperties();
        NBTTagCompound nbt = parser.getNbt();
        if (parser.isTag()) {
            return (world, pos) -> {
                Tag<LegacyBlock> tag = DataPackManager.get() == null ? null : DataPackManager.get().getTags().getBlocks().get(id);
                if (tag == null) {
                    StringReader errorAt = new StringReader(reader.getString());
                    errorAt.setCursor(start);
                    throw CommandExceptions.BLOCK_TAG_UNKNOWN.createWithContext(errorAt, id.toString());
                }
                IBlockState state = world.getBlockState(pos);
                return tag.anyMatch(block -> block.test(state) && matchesProperties(state, block, properties)) && matchesWater(world, pos, state, properties)
                        && matchesNbt(world, pos, nbt);
            };
        }
        LegacyBlock block = LegacyBlockStates.resolve(id.toString());
        IBlockState wanted = LegacyBlockStates.convert(id.toString(), properties);
        return (world, pos) -> {
            IBlockState state = world.getBlockState(pos);
            return block != null && block.test(state) && matchesExplicit(state, wanted, properties) && matchesWater(world, pos, state, properties)
                    && matchesNbt(world, pos, nbt);
        };
    }

    /** For a named block: the properties written in the command must match (after translation to 1.12). */
    private static boolean matchesExplicit(IBlockState state, IBlockState wanted, Map<String, String> properties) {
        if (properties.isEmpty() || state.getBlock() != wanted.getBlock()) return properties.isEmpty();
        for (IProperty<?> property : wanted.getPropertyKeys()) {
            if (properties.containsKey(property.getName()) && !state.getValue(property).equals(wanted.getValue(property))) return false;
        }
        return true;
    }

    private static boolean matchesProperties(IBlockState state, LegacyBlock block, Map<String, String> properties) {
        if (properties.isEmpty()) return true;
        IBlockState wanted = LegacyBlockStates.copy(state, properties);
        return wanted == state && block.test(state);
    }

    /** {@code waterlogged} of the blocks whose water is kept beside their 1.12 state. */
    private static boolean matchesWater(World world, BlockPos pos, IBlockState state, Map<String, String> properties) {
        String wanted = properties.get("waterlogged");
        return wanted == null || !Waterlogging.canWaterlog(state) || Waterlogging.isWaterlogged(world, pos) == Boolean.parseBoolean(wanted);
    }

    private static boolean matchesNbt(World world, BlockPos pos, NBTTagCompound nbt) {
        if (nbt == null) return true;
        TileEntity tile = world.getTileEntity(pos);
        return tile != null && NBTUtil.areNBTEquals(nbt, tile.writeToNBT(new NBTTagCompound()), true);
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return IdSuggestions.blocks(builder, true);
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.asList("stone", "minecraft:stone", "stone[foo=bar]", "#stone", "#stone[foo=bar]{baz=nbt}");
    }
}
