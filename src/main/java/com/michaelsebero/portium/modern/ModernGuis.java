package com.michaelsebero.portium.modern;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IInteractionObject;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Forge 1.13's {@code NetworkHooks.openGui}: the server opens a 1.13 mod's container and tells the client which GUI
 * to show, by id with the mod's extra data; the client side (in Portium's glue) asks the mod's GUI factory for it.
 */
public final class ModernGuis {

    /** The client's side: shows a mod's GUI for a container the server opened. */
    public interface ClientOpener {

        void open(ResourceLocation id, int windowId, PacketBuffer extra);
    }

    @Nullable
    private static ModernChannel channel;
    @Nullable
    private static ClientOpener opener;

    public static void init(ClientOpener clientOpener) {
        opener = clientOpener;
        if (channel == null) channel = new ModernChannel(new ResourceLocation("portium", "open_gui"), ModernGuis::receive);
    }

    public static void open(EntityPlayerMP player, IInteractionObject object, Consumer<PacketBuffer> extraData) {
        if (player.world.isRemote || channel == null) return;
        player.closeContainer();
        player.getNextWindowId();
        int windowId = ObfuscationReflectionHelper.getPrivateValue(EntityPlayerMP.class, player, "field_71139_cq");
        PacketBuffer extra = new PacketBuffer(Unpooled.buffer());
        extraData.accept(extra);
        Container container = object.createContainer(player.inventory, player);
        container.windowId = windowId;
        container.addListener(player);
        player.openContainer = container;
        PacketBuffer payload = new PacketBuffer(Unpooled.buffer());
        payload.writeResourceLocation(new ResourceLocation(object.getGuiID()));
        payload.writeVarInt(windowId);
        payload.writeVarInt(extra.readableBytes());
        payload.writeBytes(extra);
        channel.sendTo(payload, player);
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, container));
    }

    private static void receive(PacketBuffer payload, @Nullable EntityPlayerMP sender, boolean onServer) {
        if (onServer || opener == null) return;
        ResourceLocation id = payload.readResourceLocation();
        int windowId = payload.readVarInt();
        PacketBuffer extra = new PacketBuffer(payload.readBytes(payload.readVarInt()));
        opener.open(id, windowId, extra);
    }

    private ModernGuis() {}
}
