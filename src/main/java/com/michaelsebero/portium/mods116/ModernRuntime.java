package com.michaelsebero.portium.mods116;

import com.michaelsebero.portium.util.RuntimeFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Forge 1.16.5-36.2.39 and the libraries it and Minecraft 1.16.5 brought that 1.12.2 lacks, and Minecraft 1.16.5 itself with
 * MCPConfig's names for it (see {@link ModernNatives}). Portium doesn't ship them: the first time such mods are
 * installed, AssetMover fetches them from Forge's, Maven Central's and Mojang's servers during mod construction, and
 * Portium packs them into {@code portium/forge-1.16.5} in the game directory, checked against the originals' contents
 * ({@link com.michaelsebero.portium.util.RuntimeFiles}).
 */
final class ModernRuntime {

    static final String FORGE = "1.16.5-36.2.39";

    private static final List<RuntimeFiles.Spec> LIBRARIES = Arrays.asList(
            RuntimeFiles.Spec.url("forge-1.16.5-36.2.39-universal.jar", "https://maven.minecraftforge.net/net/minecraftforge/forge/1.16.5-36.2.39/forge-1.16.5-36.2.39-universal.jar", "807540b167214a81684716fa0d6e92566ae14a04", 2504067L),
            RuntimeFiles.Spec.url("forge-1.16.5-36.2.39-launcher.jar", "https://maven.minecraftforge.net/net/minecraftforge/forge/1.16.5-36.2.39/forge-1.16.5-36.2.39-launcher.jar", "0094f0546dba2d5bcee90e6a0a4f98d3adb40baf", 212574L),
            RuntimeFiles.Spec.url("forgespi-3.2.0.jar", "https://maven.minecraftforge.net/net/minecraftforge/forgespi/3.2.0/forgespi-3.2.0.jar", "0c6ca4e4e4a0343701407c760e642537b613b543", 20172L),
            RuntimeFiles.Spec.url("eventbus-4.0.0.jar", "https://maven.minecraftforge.net/net/minecraftforge/eventbus/4.0.0/eventbus-4.0.0.jar", "260e34800723e4c098c4e247cf2b900535e01b6d", 42873L),
            RuntimeFiles.Spec.url("unsafe-0.2.0.jar", "https://maven.minecraftforge.net/net/minecraftforge/unsafe/0.2.0/unsafe-0.2.0.jar", "54d7a0a5e8fdb71b973025caa46f341ae5904f39", 2834L),
            RuntimeFiles.Spec.url("modlauncher-8.1.3.jar", "https://maven.minecraftforge.net/cpw/mods/modlauncher/8.1.3/modlauncher-8.1.3.jar", "6d0bfd78e025a87d43985e30907aa9ecbc51b2d6", 126747L),
            RuntimeFiles.Spec.url("typetools-0.8.3.jar", "https://maven.minecraftforge.net/net/jodah/typetools/0.8.3/typetools-0.8.3.jar", "98f84f353457629e81cc6827224871b1a8faa7af", 15425L),
            RuntimeFiles.Spec.url("core-3.6.3.jar", "https://repo1.maven.org/maven2/com/electronwill/night-config/core/3.6.3/core-3.6.3.jar", "c601bfeaeb2c0abe7aaa901b0bbe6d1beff49281", 199091L),
            RuntimeFiles.Spec.url("toml-3.6.3.jar", "https://repo1.maven.org/maven2/com/electronwill/night-config/toml/3.6.3/toml-3.6.3.jar", "2b05b4d606c517da4d1a7b6d2b97d751c92887d7", 31288L),
            RuntimeFiles.Spec.url("brigadier-1.0.17.jar", "https://libraries.minecraft.net/com/mojang/brigadier/1.0.17/brigadier-1.0.17.jar", "c6b7dc51dd44379cc751b7504816006e9be4b1e6", 77392L),
            RuntimeFiles.Spec.url("datafixerupper-4.0.26.jar", "https://libraries.minecraft.net/com/mojang/datafixerupper/4.0.26/datafixerupper-4.0.26.jar", "ebd6690f33871ccee9b6132c6480668ee2e35020", 673183L),
            RuntimeFiles.Spec.url("fastutil-8.2.1.jar", "https://repo1.maven.org/maven2/it/unimi/dsi/fastutil/8.2.1/fastutil-8.2.1.jar", "5ad88f325e424f8dbc2be5459e21ea5cab3864e9", 18800417L));

    /** Minecraft 1.16.5's client jar, which has every class a server has too. */
    private static final RuntimeFiles.Spec MINECRAFT = RuntimeFiles.Spec.minecraft("minecraft-1.16.5-client.jar", "1.16.5", "37fd3c903861eeff3bc24b71eed48f828b5269c8", 17547153L);
    /** MCPConfig, whose joined.tsrg gives Minecraft 1.16.5's obfuscated classes and members their SRG names. */
    private static final RuntimeFiles.Spec MCP_CONFIG = RuntimeFiles.Spec.url("mcp_config-1.16.5-20210115.111550.zip", "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.16.5-20210115.111550/mcp_config-1.16.5-20210115.111550.zip", "a81464c686be42933aacac7898f8c0d50edccf51", 777652L);

    private static File dir(File gameDir) {
        return new File(gameDir, "portium/forge-1.16.5");
    }

    private static List<RuntimeFiles.Spec> all() {
        List<RuntimeFiles.Spec> all = new ArrayList<>(LIBRARIES);
        all.add(MINECRAFT);
        all.add(MCP_CONFIG);
        return all;
    }

    /** How many of the runtime's jars (Minecraft's included) aren't here yet. */
    static int missing(File gameDir) {
        int count = 0;
        for (RuntimeFiles.Spec spec : all()) if (!RuntimeFiles.isReady(dir(gameDir), spec)) count++;
        return count;
    }

    /** The runtime's jars, fetching any that are missing or damaged (during mod construction). */
    static List<File> resolve(File gameDir) throws IOException {
        RuntimeFiles.request(dir(gameDir), all());
        List<File> files = new ArrayList<>();
        for (RuntimeFiles.Spec spec : LIBRARIES) files.add(RuntimeFiles.await(dir(gameDir), spec));
        return files;
    }

    /** Minecraft 1.16.5's client jar and MCPConfig, fetching them if missing or damaged. */
    static File[] resolveMinecraft(File gameDir) throws IOException {
        RuntimeFiles.request(dir(gameDir), Arrays.asList(MINECRAFT, MCP_CONFIG));
        return new File[] {RuntimeFiles.await(dir(gameDir), MINECRAFT), RuntimeFiles.await(dir(gameDir), MCP_CONFIG)};
    }

    /** Minecraft 1.16.5's client jar alone (for its textures and data), fetching it if missing (only during mod construction). */
    static File minecraftJar(File gameDir) throws IOException {
        if (!RuntimeFiles.isReady(dir(gameDir), MINECRAFT)) RuntimeFiles.request(dir(gameDir), java.util.Collections.singletonList(MINECRAFT));
        return RuntimeFiles.await(dir(gameDir), MINECRAFT);
    }

    private ModernRuntime() {}
}
