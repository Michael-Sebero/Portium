package net.minecraftforge.fml.network.event;

import com.michaelsebero.portium.mods116.ModernNetwork116;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Portium's replacement for Forge 1.16's: a channel whose payloads arrive as events (client payloads on the server,
 * server payloads on the client), through a Forge 1.12 channel. Payloads go out as packets from
 * {@link NetworkDirection#buildPacket}.
 */
public class EventNetworkChannel {

    private final ResourceLocation name;
    private final IEventBus bus = BusBuilder.builder().build();

    public EventNetworkChannel(ResourceLocation name) {
        this.name = name;
        ModernNetwork116.channel(name, this::receive);
    }

    private void receive(PacketBuffer payload, @Nullable Object sender, boolean onServer) {
        NetworkEvent.Context context = new NetworkEvent.Context(onServer ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT, sender);
        this.bus.post(onServer ? new NetworkEvent.ClientCustomPayloadEvent(payload, () -> context) : new NetworkEvent.ServerCustomPayloadEvent(payload, () -> context));
    }

    public ResourceLocation getChannelName() {
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
