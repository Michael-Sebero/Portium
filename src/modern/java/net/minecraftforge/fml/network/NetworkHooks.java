package net.minecraftforge.fml.network;

import com.michaelsebero.portium.modern.ModernGuis;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerLoginServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.handshake.client.CPacketHandshake;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IInteractionObject;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Portium's replacement for Forge 1.13's: GUIs open through {@link ModernGuis}; connections are Forge 1.12's, which
 * handles logins and entity spawning itself.
 */
public class NetworkHooks {

    public static String getFMLVersion(String address) {
        return FMLNetworkConstants.NETVERSION;
    }

    public static ConnectionType getConnectionType(Supplier<NetworkManager> manager) {
        return ConnectionType.MODDED;
    }

    public static Packet<?> getEntitySpawningPacket(Entity entity) {
        return null;
    }

    public static boolean onCustomPayload(ICustomPacket<?> packet, NetworkManager manager) {
        return false;
    }

    public static void registerServerLoginChannel(NetworkManager manager, CPacketHandshake packet) {}

    public static synchronized void registerClientLoginChannel(NetworkManager manager) {}

    public static void handleClientLoginSuccess(NetworkManager manager) {}

    public static boolean tickNegotiation(NetHandlerLoginServer handler, NetworkManager manager, EntityPlayerMP player) {
        return true;
    }

    public static void openGui(EntityPlayerMP player, IInteractionObject object) {
        openGui(player, object, buffer -> {});
    }

    public static void openGui(EntityPlayerMP player, IInteractionObject object, BlockPos pos) {
        openGui(player, object, buffer -> buffer.func_179255_a(pos));
    }

    public static void openGui(EntityPlayerMP player, IInteractionObject object, Consumer<PacketBuffer> extraDataWriter) {
        ModernGuis.open(player, object, extraDataWriter);
    }
}
