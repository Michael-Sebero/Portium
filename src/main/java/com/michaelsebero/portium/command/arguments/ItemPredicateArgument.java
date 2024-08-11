package com.michaelsebero.portium.command.arguments;

import com.michaelsebero.portium.command.CommandExceptions;
import com.michaelsebero.portium.command.brigadier.CommandSyntaxException;
import com.michaelsebero.portium.command.brigadier.StringReader;
import com.michaelsebero.portium.command.brigadier.arguments.ArgumentType;
import com.michaelsebero.portium.command.brigadier.context.CommandContext;
import com.michaelsebero.portium.command.brigadier.suggestion.Suggestions;
import com.michaelsebero.portium.command.brigadier.suggestion.SuggestionsBuilder;
import com.michaelsebero.portium.command.nbt.SnbtParser;
import com.michaelsebero.portium.compat.LegacyItem;
import com.michaelsebero.portium.compat.LegacyItems;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.datapack.Tag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;

/** An item test for {@code clear}: a 1.13 item id or {@code #tag}, optionally with NBT the item must have. */
public class ItemPredicateArgument implements ArgumentType<ItemPredicateArgument.Result> {

    public interface Result {

        boolean test(ItemStack stack) throws CommandSyntaxException;
    }

    public static ItemPredicateArgument itemPredicate() {
        return new ItemPredicateArgument();
    }

    public static Result getItemPredicate(CommandContext<?> context, String name) {
        return context.getArgument(name, Result.class);
    }

    @Override
    public Result parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        boolean tag = reader.canRead() && reader.peek() == '#';
        if (tag) reader.skip();
        ResourceLocation id = ResourceLocationArgument.read(reader);
        NBTTagCompound nbt = reader.canRead() && reader.peek() == '{' ? new SnbtParser(reader).readStruct() : null;
        if (tag) {
            String input = reader.getString();
            return stack -> {
                Tag<LegacyItem> items = DataPackManager.get() == null ? null : DataPackManager.get().getTags().getItems().get(id);
                if (items == null) {
                    StringReader errorAt = new StringReader(input);
                    errorAt.setCursor(start);
                    throw CommandExceptions.ITEM_TAG_UNKNOWN.createWithContext(errorAt, id.toString());
                }
                return items.anyMatch(item -> item.test(stack)) && matchesNbt(stack, nbt);
            };
        }
        LegacyItem item = LegacyItems.resolve(id.toString());
        if (item == null) {
            reader.setCursor(start);
            throw CommandExceptions.ITEM_ID_INVALID.createWithContext(reader, id.toString());
        }
        return stack -> item.test(stack) && matchesNbt(stack, nbt);
    }

    private static boolean matchesNbt(ItemStack stack, NBTTagCompound nbt) {
        return nbt == null || stack.hasTagCompound() && NBTUtil.areNBTEquals(nbt, stack.getTagCompound(), true);
    }

    @Override
    public <S> Suggestions listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (DataPackManager.get() != null) IdSuggestions.suggest(DataPackManager.get().getTags().getItems().getIds(), builder, "#");
        return IdSuggestions.suggest(ItemArgument.modernIds(), builder);
    }
}
