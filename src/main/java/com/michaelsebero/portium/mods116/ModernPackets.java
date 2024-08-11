package com.michaelsebero.portium.mods116;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nullable;

/** Sends packets that 1.13 code built itself to where Forge 1.13's {@code PacketDistributor} would. */
public final class ModernPackets {

    public static void toPlayer(Packet<?> packet, EntityPlayerMP player) {
        if (player.connection != null) player.connection.sendPacket(packet);
    }

    public static void toServer(Packet<?> packet) {
        NetworkManager connection = FMLCommonHandler.instance().getClientToServerNetworkManager();
        if (connection != null) connection.sendPacket(packet);
    }

    public static void toAll(Packet<?> packet) {
        MinecraftServer server = Mods116.currentServer();
        if (server != null) server.getPlayerList().sendPacketToAllPlayers(packet);
    }

    public static void toDimension(Packet<?> packet, int dimension) {
        MinecraftServer server = Mods116.currentServer();
        if (server != null) server.getPlayerList().sendPacketToAllPlayersInDimension(packet, dimension);
    }

    public static void near(Packet<?> packet, int dimension, double x, double y, double z, double range, @Nullable EntityPlayerMP excluded) {
        MinecraftServer server = Mods116.currentServer();
        if (server != null) server.getPlayerList().sendToAllNearExcept(excluded, x, y, z, range, dimension, packet);
    }

    public static void tracking(Packet<?> packet, Entity entity, boolean self) {
        if (!(entity.world instanceof WorldServer)) return;
        if (self) {
            ((WorldServer) entity.world).getEntityTracker().sendToTrackingAndSelf(entity, packet);
        } else {
            ((WorldServer) entity.world).getEntityTracker().sendToTracking(entity, packet);
        }
    }

    public static void trackingChunk(Packet<?> packet, WorldServer world, int chunkX, int chunkZ) {
        PlayerChunkMapEntry entry = world.getPlayerChunkMap().getEntry(chunkX, chunkZ);
        if (entry != null) entry.sendPacket(packet);
    }

    /** To whichever player is connected through a network manager. */
    public static void toConnection(Packet<?> packet, NetworkManager manager) {
        manager.sendPacket(packet);
    }

    private ModernPackets() {}
}
