package net.minecraftforge.fml.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Portium's replacement for Forge 1.13's: channels are made on Forge 1.12's networking (see {@link SimpleChannel}).
 * Versions aren't checked; Forge 1.12 checks the mods themselves when a client connects.
 */
public class NetworkRegistry {

    public static String ABSENT = "ABSENT \\uD83E\\uDD14";
    public static String ACCEPTVANILLA = "ALLOWVANILLA \\uD83D\\uDC93\\uD83D\\uDC93\\uD83D\\uDC93";
    private static final Map<ResourceLocation, SimpleChannel> CHANNELS = new HashMap<>();
    private static final Map<ResourceLocation, EventNetworkChannel> EVENT_CHANNELS = new HashMap<>();

    public static List<String> getServerNonVanillaNetworkMods() {
        return new ArrayList<>();
    }

    public static List<String> getClientNonVanillaNetworkMods() {
        return new ArrayList<>();
    }

    public static boolean acceptsVanillaClientConnections() {
        return true;
    }

    public static boolean canConnectToVanillaServer() {
        return true;
    }

    public static synchronized SimpleChannel newSimpleChannel(ResourceLocation name, Supplier<String> version, Predicate<String> clientAccepted,
                                                              Predicate<String> serverAccepted) {
        return CHANNELS.computeIfAbsent(name, SimpleChannel::new);
    }

    public static synchronized EventNetworkChannel newEventChannel(ResourceLocation name, Supplier<String> version, Predicate<String> clientAccepted,
                                                                   Predicate<String> serverAccepted) {
        return EVENT_CHANNELS.computeIfAbsent(name, EventNetworkChannel::new);
    }

    public static boolean checkListPingCompatibilityForClient(Map<ResourceLocation, Pair<String, Boolean>> channels) {
        return true;
    }

    public boolean isLocked() {
        return false;
    }

    public static void lock() {}

    /** Forge 1.13's channel builder. */
    public static class ChannelBuilder {

        private ResourceLocation name;

        public static ChannelBuilder named(ResourceLocation name) {
            ChannelBuilder builder = new ChannelBuilder();
            builder.name = name;
            return builder;
        }

        public ChannelBuilder networkProtocolVersion(Supplier<String> version) {
            return this;
        }

        public ChannelBuilder clientAcceptedVersions(Predicate<String> accepted) {
            return this;
        }

        public ChannelBuilder serverAcceptedVersions(Predicate<String> accepted) {
            return this;
        }

        public SimpleChannel simpleChannel() {
            return newSimpleChannel(this.name, null, null, null);
        }

        public EventNetworkChannel eventNetworkChannel() {
            return newEventChannel(this.name, null, null, null);
        }
    }
}
