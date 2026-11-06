package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.recipe.CookingRecipes;
import com.michaelsebero.portium.recipe.StonecuttingRecipes;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The server's data pack recipes and the game recipes they replace: crafting (as 1.12 recipe JSON), and cooking
 * (campfire, smoker, blast furnace) and stonecutting, which the client's campfire and stonecutter screen ask about.
 */
public class RecipeSyncMessage implements IMessage {

    public Map<ResourceLocation, String> recipes = new LinkedHashMap<>();
    public Set<ResourceLocation> replaced = new HashSet<>();
    public List<CookingRecipes.Recipe> cooking = new ArrayList<>();
    public List<StonecuttingRecipes.Recipe> stonecutting = new ArrayList<>();

    public RecipeSyncMessage() {}

    public RecipeSyncMessage(Map<ResourceLocation, String> recipes, Set<ResourceLocation> replaced, List<CookingRecipes.Recipe> cooking,
                             List<StonecuttingRecipes.Recipe> stonecutting) {
        this.recipes = recipes;
        this.replaced = replaced;
        this.cooking = cooking;
        this.stonecutting = stonecutting;
    }

    private static void writeStacks(ByteBuf buf, List<ItemStack> stacks) {
        buf.writeInt(stacks.size());
        for (ItemStack stack : stacks) ByteBufUtils.writeItemStack(buf, stack);
    }

    private static List<ItemStack> readStacks(ByteBuf buf) {
        int count = buf.readInt();
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) stacks.add(ByteBufUtils.readItemStack(buf));
        return stacks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int recipeCount = buf.readInt();
        for (int i = 0; i < recipeCount; i++) this.recipes.put(new ResourceLocation(ByteBufUtils.readUTF8String(buf)), ByteBufUtils.readUTF8String(buf));
        int replacedCount = buf.readInt();
        for (int i = 0; i < replacedCount; i++) this.replaced.add(new ResourceLocation(ByteBufUtils.readUTF8String(buf)));
        int cookingCount = buf.readInt();
        for (int i = 0; i < cookingCount; i++) {
            ResourceLocation id = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
            CookingRecipes.Type type = CookingRecipes.Type.byId(ByteBufUtils.readUTF8String(buf));
            List<ItemStack> inputs = readStacks(buf);
            ItemStack result = ByteBufUtils.readItemStack(buf);
            float experience = buf.readFloat();
            int time = buf.readInt();
            if (type != null) this.cooking.add(new CookingRecipes.Recipe(id, type, inputs, result, experience, time));
        }
        int stonecuttingCount = buf.readInt();
        for (int i = 0; i < stonecuttingCount; i++) {
            ResourceLocation id = new ResourceLocation(ByteBufUtils.readUTF8String(buf));
            List<ItemStack> inputs = readStacks(buf);
            ItemStack result = ByteBufUtils.readItemStack(buf);
            this.stonecutting.add(new StonecuttingRecipes.Recipe(id, inputs, result, ByteBufUtils.readUTF8String(buf)));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.recipes.size());
        for (Map.Entry<ResourceLocation, String> entry : this.recipes.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey().toString());
            ByteBufUtils.writeUTF8String(buf, entry.getValue());
        }
        buf.writeInt(this.replaced.size());
        for (ResourceLocation id : this.replaced) ByteBufUtils.writeUTF8String(buf, id.toString());
        buf.writeInt(this.cooking.size());
        for (CookingRecipes.Recipe recipe : this.cooking) {
            ByteBufUtils.writeUTF8String(buf, recipe.id.toString());
            ByteBufUtils.writeUTF8String(buf, recipe.type.id);
            writeStacks(buf, recipe.getInputs());
            ByteBufUtils.writeItemStack(buf, recipe.getResult());
            buf.writeFloat(recipe.experience);
            buf.writeInt(recipe.cookingTime);
        }
        buf.writeInt(this.stonecutting.size());
        for (StonecuttingRecipes.Recipe recipe : this.stonecutting) {
            ByteBufUtils.writeUTF8String(buf, recipe.id.toString());
            writeStacks(buf, recipe.getInputs());
            ByteBufUtils.writeItemStack(buf, recipe.getResult());
            ByteBufUtils.writeUTF8String(buf, recipe.getSortKey());
        }
    }

    public static class Handler implements IMessageHandler<RecipeSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(RecipeSyncMessage message, MessageContext context) {
            Portium.proxy.receiveRecipes(message);
            return null;
        }
    }
}
