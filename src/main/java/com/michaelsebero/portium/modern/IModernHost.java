package com.michaelsebero.portium.modern;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * What 1.12 code asks of Forge 1.13.2's side. Implemented by Portium's glue ({@code modern.host}), which is loaded by
 * the 1.13 mods' class loader; arguments are Minecraft or Portium types both sides share.
 */
public interface IModernHost {

    /** Reads the 1.13 mods in these jars and constructs them. Returns their ids. */
    List<String> construct(List<File> jars);

    /** The loaded mods for 1.12's mod list: id, name, version, description, authors and url, by those keys. */
    List<Map<String, String>> describeMods();

    /** Forge 1.13's registry creation stage. */
    void createRegistries();

    /**
     * Forge 1.13's registry event for one of 1.12's registries, as 1.12 fires its own; then fills 1.13 mods' object
     * holders from it.
     */
    void register(ResourceLocation name, IForgeRegistry<?> registry);

    /** Common, then client or server setup. */
    void setup();

    /** Mods send, then process, their inter-mod messages. */
    void interModComms();

    void complete();

    /** Loads the mods' server configs from the server's world. */
    void serverAboutToStart(MinecraftServer server);

    /** Whether a data pack recipe's Forge 1.13 conditions hold, by Forge 1.13's and mods' condition serializers. */
    boolean conditionsMet(com.google.gson.JsonObject recipe);

    /** A server is starting: 1.13 mods register their commands. */
    void serverStarting(net.minecraft.server.MinecraftServer server);

    void serverStarted(net.minecraft.server.MinecraftServer server);

    void serverStopping(net.minecraft.server.MinecraftServer server);

    void serverStopped(net.minecraft.server.MinecraftServer server);
}
