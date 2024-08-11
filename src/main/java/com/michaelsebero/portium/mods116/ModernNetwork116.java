package com.michaelsebero.portium.mods116;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Forge 1.16's networking for 1.16 mods, on Forge 1.12's: Portium's {@code SimpleChannel}, {@code PacketDistributor}
 * and {@code NetworkHooks} (in the 1.16 glue, replacing Forge 1.16's, whose own network stack doesn't run here) send
 * through {@link ModernChannel}s. The glue can't name 1.12's classes, so players, entities, chunks, containers and
 * channels are taken as objects, and dimensions by 1.16's names.
 */
public final class ModernNetwork116 {

    /** What arrives on a channel: the payload, the player who sent it (none on a client), and the side. */
    public interface Receiver {

        void receive(PacketBuffer payload, @Nullable Object sender, boolean onServer);
    }

    /** A channel of Forge 1.12's for a 1.16 mod's channel name. */
    public static Object channel(ResourceLocation id, Receiver receiver) {
        return new ModernChannel(id, receiver::receive);
    }

    public static void toServer(Object channel, PacketBuffer payload) {
        ((ModernChannel) channel).sendToServer(payload);
    }

    public static void toPlayer(Object channel, PacketBuffer payload, Object player) {
        ((ModernChannel) channel).sendTo(payload, (EntityPlayerMP) player);
    }

    public static void toAll(Object channel, PacketBuffer payload) {
        ((ModernChannel) channel).sendToAll(payload);
    }

    public static void toDimension(Object channel, PacketBuffer payload, String dimension) {
        ((ModernChannel) channel).sendToDimension(payload, dimensionId(dimension));
    }

    public static void near(Object channel, PacketBuffer payload, String dimension, double x, double y, double z, double range, @Nullable Object excluded) {
        ((ModernChannel) channel).sendToAllAround(payload, dimensionId(dimension), x, y, z, range, (EntityPlayerMP) excluded);
    }

    public static void tracking(Object channel, PacketBuffer payload, Object entity, boolean self) {
        ((ModernChannel) channel).sendToTracking(payload, (Entity) entity, self);
    }

    public static void trackingChunk(Object channel, PacketBuffer payload, Object chunk) {
        Chunk at = (Chunk) chunk;
        if (at.getWorld() instanceof WorldServer) ((ModernChannel) channel).sendToTrackingChunk(payload, (WorldServer) at.getWorld(), at.x, at.z);
    }

    /** To the player on that connection (1.16 mods name connections; 1.12 sends to players). */
    public static void toConnection(Object channel, PacketBuffer payload, Object connection) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player.connection != null && player.connection.netManager == connection) ((ModernChannel) channel).sendTo(new PacketBuffer(payload.copy()), player);
        }
    }

    public static Object vanillaPacket(ResourceLocation id, PacketBuffer payload) {
        return ModernChannel.vanillaPacket(id, payload);
    }

    /** 1.12's number for a dimension 1.16 names: vanilla's three, then 1.12's dimension types by name. */
    public static int dimensionId(String name) {
        switch (name) {
            case "minecraft:overworld":
                return 0;
            case "minecraft:the_nether":
                return -1;
            case "minecraft:the_end":
                return 1;
            default:
                String path = name.substring(name.indexOf(':') + 1);
                for (DimensionType type : DimensionType.values()) {
                    if (type.getName().equals(path)) return type.getId();
                }
                return 0;
        }
    }

    /**
     * Runs work on the game's thread of a side (the server, or the client), as Forge 1.16's network context does;
     * the future completes when it has run.
     */
    public static CompletableFuture<Void> schedule(boolean onServer, Runnable work) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Runnable task = () -> {
            try {
                work.run();
                done.complete(null);
            } catch (Throwable e) {
                done.completeExceptionally(e);
                throw e;
            }
        };
        if (onServer) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server == null) {
                done.cancel(false);
            } else {
                server.addScheduledTask(task);
            }
        } else {
            ClientTasks.schedule(task);
        }
        return done;
    }

    // Menus: the server's half of NetworkHooks.openGui.

    /** Closes the player's open menu and gives the next window id, as 1.16 does before opening another. */
    public static int nextWindowId(Object player) {
        EntityPlayerMP server = (EntityPlayerMP) player;
        server.closeContainer();
        server.getNextWindowId();
        return ObfuscationReflectionHelper.getPrivateValue(EntityPlayerMP.class, server, "field_71139_cq");
    }

    /** The menu made for the player is open: 1.12 tracks it and tells listeners, as Forge 1.16 does. */
    public static void opened(Object player, Object container) {
        EntityPlayerMP server = (EntityPlayerMP) player;
        Container menu = (Container) container;
        server.openContainer = menu;
        menu.addListener(server);
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(server, menu));
    }

    /** The client's menu for a window: 1.12's player keeps it. */
    public static void openedOnClient(Object player, Object container) {
        ((net.minecraft.entity.player.EntityPlayer) player).openContainer = (Container) container;
    }

    /** A menu's title as JSON, to send with it. */
    public static String textToJson(Object text) {
        return net.minecraft.util.text.ITextComponent.Serializer.componentToJson((net.minecraft.util.text.ITextComponent) text);
    }

    /** A title sent as JSON, as a text component (1.16's text components are 1.12's here). */
    public static Object textFromJson(String json) {
        net.minecraft.util.text.ITextComponent text = net.minecraft.util.text.ITextComponent.Serializer.jsonToComponent(json);
        return text == null ? new net.minecraft.util.text.TextComponentString("") : text;
    }

    /** Whether a connection (by its manager) is to this server's integrated client: its packets stay in memory. */
    public static boolean isLocal(@Nullable NetworkManager manager) {
        return manager != null && manager.isLocalChannel();
    }

    /** The client's thread; loaded only when used, which is only on a client. */
    private static final class ClientTasks {

        static void schedule(Runnable task) {
            net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(task);
        }

        private ClientTasks() {}
    }

    private ModernNetwork116() {}
}
