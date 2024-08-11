package net.minecraftforge.fml.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Portium's replacement for Forge 1.16's: menus open through Portium's own channel (MenuGlue); connections, logins
 * and entity spawning are Forge 1.12's, which handles them itself.
 */
public class NetworkHooks {

    public static String getFMLVersion(String address) {
        return "FML2";
    }

    public static ConnectionType getConnectionType(Supplier<NetworkManager> manager) {
        return ConnectionType.MODDED;
    }

    /** 1.12's FML spawns a mod's entities with their spawn data itself, so no packet of 1.16's is needed. */
    public static IPacket<?> getEntitySpawningPacket(Entity entity) {
        return null;
    }

    public static boolean isVanillaConnection(NetworkManager manager) {
        return false;
    }

    public static void openGui(ServerPlayerEntity player, INamedContainerProvider provider) {
        openGui(player, provider, buffer -> {});
    }

    public static void openGui(ServerPlayerEntity player, INamedContainerProvider provider, BlockPos pos) {
        openGui(player, provider, buffer -> buffer.func_179255_a(pos));
    }

    public static void openGui(ServerPlayerEntity player, INamedContainerProvider provider, Consumer<PacketBuffer> extraDataWriter) {
        com.michaelsebero.portium.mods116.host.MenuGlue.open(player, provider, extraDataWriter);
    }
}
