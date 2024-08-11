package com.michaelsebero.portium.modern;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A 1.13 mod's network channel on Forge 1.12's: payloads in, payloads out. Forge 1.13's {@code SimpleChannel} (in
 * Portium's glue) encodes its messages onto one of these.
 */
public final class ModernChannel {

    /** What arrives: the payload, the player who sent it (null on a client), and which side it arrived on. */
    public interface Receiver {

        void receive(PacketBuffer payload, @Nullable EntityPlayerMP sender, boolean onServer);
    }

    private final String name;
    private final FMLEventChannel channel;
    private final Receiver receiver;

    public ModernChannel(ResourceLocation id, Receiver receiver) {
        this.name = channelName(id);
        this.receiver = receiver;
        this.channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(this.name);
        this.channel.register(this);
    }

    /** Forge 1.12 names channels with at most 20 characters; longer 1.13 names get a short name made from them. */
    static String channelName(ResourceLocation id) {
        String name = id.toString();
        if (name.length() <= 20) return name;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(name.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder("m113:");
            for (int i = 0; hex.length() < 20; i++) hex.append(String.format("%02x", hash[i]));
            return hex.substring(0, 20);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @SubscribeEvent
    public void onServerPacket(FMLNetworkEvent.ServerCustomPacketEvent event) {
        EntityPlayerMP sender = ((NetHandlerPlayServer) event.getHandler()).player;
        this.receiver.receive(new PacketBuffer(event.getPacket().payload()), sender, true);
    }

    @SubscribeEvent
    public void onClientPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        this.receiver.receive(new PacketBuffer(event.getPacket().payload()), null, false);
    }

    private FMLProxyPacket packet(PacketBuffer payload) {
        return packet(this.name, payload);
    }

    private static FMLProxyPacket packet(String name, PacketBuffer payload) {
        PacketBuffer copy = new PacketBuffer(Unpooled.buffer(payload.readableBytes()));
        copy.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
        return new FMLProxyPacket(copy, name);
    }

    /** A packet carrying a payload on the channel of a 1.13 name, for 1.13 code that sends packets itself. */
    public static net.minecraft.network.Packet<?> vanillaPacket(ResourceLocation id, PacketBuffer payload) {
        return packet(channelName(id), payload);
    }

    public void sendToServer(PacketBuffer payload) {
        this.channel.sendToServer(packet(payload));
    }

    public void sendTo(PacketBuffer payload, EntityPlayerMP player) {
        this.channel.sendTo(packet(payload), player);
    }

    public void sendToAll(PacketBuffer payload) {
        this.channel.sendToAll(packet(payload));
    }

    public void sendToDimension(PacketBuffer payload, int dimension) {
        this.channel.sendToDimension(packet(payload), dimension);
    }

    public void sendToAllAround(PacketBuffer payload, int dimension, double x, double y, double z, double range, @Nullable EntityPlayerMP excluded) {
        net.minecraft.server.MinecraftServer server = ModernMods.currentServer();
        if (server == null) return;
        for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
            if (player == excluded || player.dimension != dimension) continue;
            double dx = x - player.posX;
            double dy = y - player.posY;
            double dz = z - player.posZ;
            if (dx * dx + dy * dy + dz * dz < range * range) sendTo(payload, player);
        }
    }

    /** To the players who see an entity, and the entity itself if it's a player and {@code self}. */
    public void sendToTracking(PacketBuffer payload, Entity entity, boolean self) {
        if (!(entity.world instanceof WorldServer)) return;
        for (net.minecraft.entity.player.EntityPlayer player : ((WorldServer) entity.world).getEntityTracker().getTrackingPlayers(entity)) {
            if (player instanceof EntityPlayerMP) sendTo(payload, (EntityPlayerMP) player);
        }
        if (self && entity instanceof EntityPlayerMP) sendTo(payload, (EntityPlayerMP) entity);
    }

    /** To the players who see a chunk. */
    public void sendToTrackingChunk(PacketBuffer payload, WorldServer world, int chunkX, int chunkZ) {
        net.minecraft.server.management.PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(chunkX, chunkZ);
        if (entry == null) return;
        for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, p -> entry.containsPlayer(p))) sendTo(payload, player);
    }
}
