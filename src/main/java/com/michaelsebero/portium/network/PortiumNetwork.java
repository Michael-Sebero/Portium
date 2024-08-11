package com.michaelsebero.portium.network;

import com.michaelsebero.portium.Tags;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Portium's packets, for things 1.12's own packets cannot carry. */
public final class PortiumNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    public static void init() {
        CHANNEL.registerMessage(ParticleMessage.Handler.class, ParticleMessage.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(RecipeSyncMessage.Handler.class, RecipeSyncMessage.class, 1, Side.CLIENT);
        CHANNEL.registerMessage(WaterlogChunkMessage.Handler.class, WaterlogChunkMessage.class, 2, Side.CLIENT);
        CHANNEL.registerMessage(WaterlogUpdateMessage.Handler.class, WaterlogUpdateMessage.class, 3, Side.CLIENT);
        CHANNEL.registerMessage(ServerFeaturesMessage.Handler.class, ServerFeaturesMessage.class, 4, Side.CLIENT);
        CHANNEL.registerMessage(CommandSuggestRequest.Handler.class, CommandSuggestRequest.class, 5, Side.SERVER);
        CHANNEL.registerMessage(CommandSuggestResponse.Handler.class, CommandSuggestResponse.class, 6, Side.CLIENT);
        CHANNEL.registerMessage(ModernStateChunkMessage.Handler.class, ModernStateChunkMessage.class, 7, Side.CLIENT);
        CHANNEL.registerMessage(ModernStateUpdateMessage.Handler.class, ModernStateUpdateMessage.class, 8, Side.CLIENT);
    }

    private PortiumNetwork() {}
}
