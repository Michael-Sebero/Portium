package com.michaelsebero.portium.mods116.host;

import com.michaelsebero.portium.mods116.ModernNetwork116;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Forge 1.16's {@code NetworkHooks.openGui}: the server makes a mod's menu and tells the client its type, window id,
 * title and the mod's extra data on Portium's channel; the client makes the same menu from them and shows the screen
 * the mod registered for its type (MenuClientGlue).
 */
public final class MenuGlue {

    @Nullable
    private static Object channel;

    static synchronized Object channel() {
        if (channel == null) channel = ModernNetwork116.channel(new ResourceLocation("portium", "open_menu"), MenuGlue::receive);
        return channel;
    }

    public static void open(ServerPlayerEntity player, INamedContainerProvider provider, Consumer<PacketBuffer> extraDataWriter) {
        if (player.field_70170_p.field_72995_K) return;
        Object out = channel();
        PacketBuffer extra = new PacketBuffer(Unpooled.buffer());
        extraDataWriter.accept(extra);
        int windowId = ModernNetwork116.nextWindowId(player);
        Container menu = provider.createMenu(windowId, player.field_71071_by, player);
        if (menu == null) return;
        ContainerType<?> type = menu.func_216957_a();
        PacketBuffer payload = new PacketBuffer(Unpooled.buffer());
        // The menu type is a registry entry at runtime (Portium makes Minecraft 1.16.5's registered types so).
        payload.func_192572_a(((net.minecraftforge.registries.IForgeRegistryEntry<?>) (Object) type).getRegistryName());
        payload.func_150787_b(windowId);
        payload.func_180714_a(ModernNetwork116.textToJson(provider.func_145748_c_()));
        payload.func_150787_b(extra.readableBytes());
        payload.writeBytes(extra);
        ModernNetwork116.toPlayer(out, payload, player);
        ModernNetwork116.opened(player, menu);
    }

    private static void receive(PacketBuffer payload, @Nullable Object sender, boolean onServer) {
        if (onServer) return;
        ResourceLocation id = payload.func_192575_l();
        int windowId = payload.func_150792_a();
        String title = payload.func_150789_c(262144);
        PacketBuffer extra = new PacketBuffer(payload.readBytes(payload.func_150792_a()));
        ModernNetwork116.schedule(false, () -> MenuClientGlue.open(id, windowId, title, extra));
    }

    private MenuGlue() {}
}
