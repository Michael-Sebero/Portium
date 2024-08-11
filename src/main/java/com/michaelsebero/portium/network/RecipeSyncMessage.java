package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Portium;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** The server's data pack crafting recipes (as 1.12 recipe JSON) and the game recipes they replace. */
public class RecipeSyncMessage implements IMessage {

    public Map<ResourceLocation, String> recipes = new LinkedHashMap<>();
    public Set<ResourceLocation> replaced = new HashSet<>();

    public RecipeSyncMessage() {}

    public RecipeSyncMessage(Map<ResourceLocation, String> recipes, Set<ResourceLocation> replaced) {
        this.recipes = recipes;
        this.replaced = replaced;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int recipeCount = buf.readInt();
        for (int i = 0; i < recipeCount; i++) this.recipes.put(new ResourceLocation(ByteBufUtils.readUTF8String(buf)), ByteBufUtils.readUTF8String(buf));
        int replacedCount = buf.readInt();
        for (int i = 0; i < replacedCount; i++) this.replaced.add(new ResourceLocation(ByteBufUtils.readUTF8String(buf)));
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
    }

    public static class Handler implements IMessageHandler<RecipeSyncMessage, IMessage> {

        @Override
        public IMessage onMessage(RecipeSyncMessage message, MessageContext context) {
            Portium.proxy.receiveRecipes(message);
            return null;
        }
    }
}
