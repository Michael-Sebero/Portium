package net.minecraftforge.fml.network;

import com.michaelsebero.portium.modern.ModernChannel;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Portium's replacement for Forge 1.13's: a channel whose payloads arrive as events, {@link
 * NetworkEvent.ClientCustomPayloadEvent} on the server and {@link NetworkEvent.ServerCustomPayloadEvent} on the client,
 * through a Forge 1.12 channel ({@link ModernChannel}). Payloads go out as packets from {@link NetworkDirection#buildPacket}.
 */
public class EventNetworkChannel {

    private final ResourceLocation name;
    private final IEventBus bus = BusBuilder.builder().build();
    private final ModernChannel channel;

    public EventNetworkChannel(ResourceLocation name) {
        this.name = name;
        this.channel = new ModernChannel(name, this::receive);
    }

    private void receive(PacketBuffer payload, @Nullable EntityPlayerMP sender, boolean onServer) {
        NetworkEvent.Context context = new NetworkEvent.Context(onServer ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT, sender);
        this.bus.post(onServer ? new NetworkEvent.ClientCustomPayloadEvent(payload, () -> context)
                : new NetworkEvent.ServerCustomPayloadEvent(payload, () -> context));
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public <T extends NetworkEvent> void addListener(Consumer<T> listener) {
        this.bus.addListener(listener);
    }

    public void registerObject(Object object) {
        this.bus.register(object);
    }

    public void unregisterObject(Object object) {
        this.bus.unregister(object);
    }
}
