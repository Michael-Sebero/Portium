package net.minecraftforge.fml.network.simple;

import com.michaelsebero.portium.mods116.ModernNetwork116;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Portium's replacement for Forge 1.16's: indexed messages, each encoded after its index byte, sent and received
 * through a Forge 1.12 channel (ModernNetwork116). Handlers run on the network thread, as in 1.16, and hand work to
 * the game's thread with {@code enqueueWork}.
 */
public class SimpleChannel {

    private static final class Codec<MSG> {

        final int index;
        final Class<MSG> type;
        final BiConsumer<MSG, PacketBuffer> encoder;
        final Function<PacketBuffer, MSG> decoder;
        final BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler;

        Codec(int index, Class<MSG> type, BiConsumer<MSG, PacketBuffer> encoder, Function<PacketBuffer, MSG> decoder,
              BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
            this.index = index;
            this.type = type;
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }
    }

    private final ResourceLocation name;
    private final Object channel;
    private final Map<Integer, Codec<?>> byIndex = new HashMap<>();
    private final Map<Class<?>, Codec<?>> byType = new HashMap<>();

    public SimpleChannel(ResourceLocation name) {
        this.name = name;
        this.channel = ModernNetwork116.channel(name, this::receive);
    }

    public <MSG> int encodeMessage(MSG message, PacketBuffer buffer) {
        Codec<MSG> codec = codecOf(message);
        buffer.writeByte(codec.index);
        if (codec.encoder != null) codec.encoder.accept(message, buffer);
        return codec.index;
    }

    public <MSG> IndexedMessageCodec.MessageHandler<MSG> registerMessage(int index, Class<MSG> type, BiConsumer<MSG, PacketBuffer> encoder,
                                                                         Function<PacketBuffer, MSG> decoder,
                                                                         BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        Codec<MSG> codec = new Codec<>(index & 255, type, encoder, decoder, handler);
        this.byIndex.put(codec.index, codec);
        this.byType.put(type, codec);
        return new IndexedMessageCodec.MessageHandler<>(codec.index);
    }

    public <MSG> IndexedMessageCodec.MessageHandler<MSG> registerMessage(int index, Class<MSG> type, BiConsumer<MSG, PacketBuffer> encoder,
                                                                         Function<PacketBuffer, MSG> decoder,
                                                                         BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler,
                                                                         Optional<NetworkDirection> direction) {
        return registerMessage(index, type, encoder, decoder, handler);
    }

    public <MSG> void sendToServer(MSG message) {
        ModernNetwork116.toServer(this.channel, encode(message));
    }

    public <MSG> void sendTo(MSG message, NetworkManager manager, NetworkDirection direction) {
        if (direction == NetworkDirection.PLAY_TO_SERVER || direction == NetworkDirection.LOGIN_TO_SERVER) {
            sendToServer(message);
        } else {
            ModernNetwork116.toConnection(this.channel, encode(message), manager);
        }
    }

    public <MSG> void send(PacketDistributor.PacketTarget target, MSG message) {
        target.send(this.channel, encode(message));
    }

    @SuppressWarnings("unchecked")
    public <MSG> IPacket<?> toVanillaPacket(MSG message, NetworkDirection direction) {
        return (IPacket<?>) ModernNetwork116.vanillaPacket(this.name, encode(message));
    }

    public <MSG> void reply(MSG message, NetworkEvent.Context context) {
        if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER && context.getSender() != null) {
            ModernNetwork116.toPlayer(this.channel, encode(message), context.getSender());
        } else {
            sendToServer(message);
        }
    }

    /** Whether the other side has the channel: Forge 1.12 checked the mods when the client connected. */
    public boolean isRemotePresent(NetworkManager manager) {
        return true;
    }

    public <M> MessageBuilder<M> messageBuilder(Class<M> type, int index) {
        return new MessageBuilder<>(this, type, index);
    }

    public <M> MessageBuilder<M> messageBuilder(Class<M> type, int index, NetworkDirection direction) {
        return new MessageBuilder<>(this, type, index);
    }

    private <MSG> PacketBuffer encode(MSG message) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        encodeMessage(message, buffer);
        return buffer;
    }

    @SuppressWarnings("unchecked")
    private <MSG> Codec<MSG> codecOf(MSG message) {
        Codec<MSG> codec = (Codec<MSG>) this.byType.get(message.getClass());
        if (codec == null) throw new IllegalArgumentException("Message " + message.getClass().getName() + " isn't registered on channel " + this.name);
        return codec;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void receive(PacketBuffer payload, @Nullable Object sender, boolean onServer) {
        int index = payload.readUnsignedByte();
        Codec codec = this.byIndex.get(index);
        if (codec == null || codec.decoder == null) return;
        Object message = codec.decoder.apply(payload);
        NetworkEvent.Context context = new NetworkEvent.Context(onServer ? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT, sender);
        if (codec.handler != null) codec.handler.accept(message, (Supplier<NetworkEvent.Context>) () -> context);
    }

    /** Forge 1.16's builder for a message's codec and handler. */
    public static class MessageBuilder<MSG> {

        private final SimpleChannel channel;
        private final Class<MSG> type;
        private final int index;
        private BiConsumer<MSG, PacketBuffer> encoder;
        private Function<PacketBuffer, MSG> decoder;
        private BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer;

        MessageBuilder(SimpleChannel channel, Class<MSG> type, int index) {
            this.channel = channel;
            this.type = type;
            this.index = index;
        }

        public MessageBuilder<MSG> encoder(BiConsumer<MSG, PacketBuffer> encoder) {
            this.encoder = encoder;
            return this;
        }

        public MessageBuilder<MSG> decoder(Function<PacketBuffer, MSG> decoder) {
            this.decoder = decoder;
            return this;
        }

        public MessageBuilder<MSG> loginIndex(Function<MSG, Integer> getter, BiConsumer<MSG, Integer> setter) {
            return this;
        }

        public MessageBuilder<MSG> buildLoginPacketList(Function<Boolean, List<Pair<String, MSG>>> packets) {
            return this;
        }

        public MessageBuilder<MSG> markAsLoginPacket() {
            return this;
        }

        public MessageBuilder<MSG> consumer(BiConsumer<MSG, Supplier<NetworkEvent.Context>> consumer) {
            this.consumer = consumer;
            return this;
        }

        /** The handler that says itself whether it handled the message. */
        public MessageBuilder<MSG> consumer(ToBooleanBiFunction<MSG, Supplier<NetworkEvent.Context>> handler) {
            this.consumer = (message, context) -> {
                boolean handled = handler.applyAsBool(message, context);
                context.get().setPacketHandled(handled);
            };
            return this;
        }

        public void add() {
            this.channel.registerMessage(this.index, this.type, this.encoder, this.decoder, this.consumer);
        }

        public interface ToBooleanBiFunction<A, B> {

            boolean applyAsBool(A first, B second);
        }
    }
}
