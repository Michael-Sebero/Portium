package net.minecraftforge.fml.network;

import com.google.common.util.concurrent.ListenableFuture;
import com.michaelsebero.portium.modern.ModernMods;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.DefaultAttributeMap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Portium's replacement for Forge 1.13's: a message arriving, and its context. */
public class NetworkEvent extends Event {

    private final PacketBuffer payload;
    private final Supplier<Context> source;

    public NetworkEvent(PacketBuffer payload, Supplier<Context> source) {
        this.payload = payload;
        this.source = source;
    }

    public PacketBuffer getPayload() {
        return this.payload;
    }

    public Supplier<Context> getSource() {
        return this.source;
    }

    public int getLoginIndex() {
        return -1;
    }

    /** A payload from the server, on the client (an {@link EventNetworkChannel}'s). */
    public static class ServerCustomPayloadEvent extends NetworkEvent {

        public ServerCustomPayloadEvent(PacketBuffer payload, Supplier<Context> source) {
            super(payload, source);
        }
    }

    /** A payload from a client, on the server (an {@link EventNetworkChannel}'s). */
    public static class ClientCustomPayloadEvent extends NetworkEvent {

        public ClientCustomPayloadEvent(PacketBuffer payload, Supplier<Context> source) {
            super(payload, source);
        }
    }

    /** The other side registering or dropping a channel: Forge 1.12 doesn't say, so this isn't fired. */
    public static class ChannelRegistrationChangeEvent extends NetworkEvent {

        private final RegistrationChangeType changeType;

        public ChannelRegistrationChangeEvent(PacketBuffer payload, Supplier<Context> source, RegistrationChangeType changeType) {
            super(payload, source);
            this.changeType = changeType;
        }

        public RegistrationChangeType getRegistrationChangeType() {
            return this.changeType;
        }
    }

    public enum RegistrationChangeType {
        REGISTER, UNREGISTER
    }

    /** Where a message came from: its direction and sender, and how to run work on the game's thread. */
    public static class Context {

        private final NetworkDirection direction;
        @Nullable
        private final EntityPlayerMP sender;
        private final DefaultAttributeMap attributes = new DefaultAttributeMap();
        private boolean handled;

        public Context(NetworkDirection direction, @Nullable EntityPlayerMP sender) {
            this.direction = direction;
            this.sender = sender;
        }

        public NetworkDirection getDirection() {
            return this.direction;
        }

        @Nullable
        public PacketDispatcher getPacketDispatcher() {
            return null;
        }

        public <T> Attribute<T> attr(AttributeKey<T> key) {
            return this.attributes.attr(key);
        }

        public void setPacketHandled(boolean handled) {
            this.handled = handled;
        }

        public boolean getPacketHandled() {
            return this.handled;
        }

        /** Runs work on the thread of the side the message arrived on. */
        @SuppressWarnings("unchecked")
        public <V> ListenableFuture<V> enqueueWork(Runnable work) {
            if (this.direction.getReceptionSide() == net.minecraftforge.fml.LogicalSide.SERVER) {
                MinecraftServer server = ModernMods.currentServer();
                if (server != null) return (ListenableFuture<V>) server.func_152344_a(work);
            }
            return (ListenableFuture<V>) com.michaelsebero.portium.modern.host.ModernClient.schedule(work);
        }

        @Nullable
        public EntityPlayerMP getSender() {
            return this.sender;
        }
    }
}
