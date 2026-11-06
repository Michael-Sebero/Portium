package com.michaelsebero.portium.modern;

import com.michaelsebero.portium.util.RuntimeFiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Forge 1.13.2-25.0.223 and the libraries it and Minecraft 1.13.2 brought that 1.12.2 lacks, and Minecraft 1.13.2 itself with
 * MCPConfig's names for it (see {@link ModernNatives}). Portium doesn't ship them: the first time such mods are
 * installed, AssetMover fetches them from Forge's, Maven Central's and Mojang's servers during mod construction, and
 * Portium packs them into {@code portium/forge-1.13.2} in the game directory, checked against the originals' contents
 * ({@link com.michaelsebero.portium.util.RuntimeFiles}).
 */
final class ModernRuntime {

    static final String FORGE = "1.13.2-25.0.223";

    private static final List<RuntimeFiles.Spec> LIBRARIES = Arrays.asList(
            RuntimeFiles.Spec.url("forge-1.13.2-25.0.223-universal.jar", "https://maven.minecraftforge.net/net/minecraftforge/forge/1.13.2-25.0.223/forge-1.13.2-25.0.223-universal.jar", "55ec5d86e208a0f3b91e31c1c3f1ef960f7e41d1", 2385532L),
            RuntimeFiles.Spec.url("forge-1.13.2-25.0.223-launcher.jar", "https://maven.minecraftforge.net/net/minecraftforge/forge/1.13.2-25.0.223/forge-1.13.2-25.0.223-launcher.jar", "768790d34994d806935701002913cb7957993e5b", 159947L),
            RuntimeFiles.Spec.url("forgespi-0.13.0.jar", "https://maven.minecraftforge.net/net/minecraftforge/forgespi/0.13.0/forgespi-0.13.0.jar", "2623344ba8cdb3e2e0a16be59c1fc22ad25f2981", 16607L),
            RuntimeFiles.Spec.url("eventbus-0.9.3-service.jar", "https://maven.minecraftforge.net/net/minecraftforge/eventbus/0.9.3/eventbus-0.9.3-service.jar", "0a2ad95acb01a06ee6d263d3fe86750463ae5ec5", 39660L),
            RuntimeFiles.Spec.url("core-3.6.0.jar", "https://repo1.maven.org/maven2/com/electronwill/night-config/core/3.6.0/core-3.6.0.jar", "0412f2edf0ef6cc9178ab38751af4e507f413bef", 199763L),
            RuntimeFiles.Spec.url("toml-3.6.0.jar", "https://repo1.maven.org/maven2/com/electronwill/night-config/toml/3.6.0/toml-3.6.0.jar", "f0c2c48748bdfc8d1f3798ec478f003bccc7e0b1", 31256L),
            RuntimeFiles.Spec.url("typetools-0.6.0.jar", "https://repo1.maven.org/maven2/net/jodah/typetools/0.6.0/typetools-0.6.0.jar", "a1552ecd6f5b9444585a7c4b05f6312cf7d32fd3", 14734L),
            RuntimeFiles.Spec.url("brigadier-1.0.14.jar", "https://libraries.minecraft.net/com/mojang/brigadier/1.0.14/brigadier-1.0.14.jar", "a18df0522b577ca364b87019c1efa0b8faa12fe5", 73350L),
            RuntimeFiles.Spec.url("datafixerupper-1.0.19.jar", "https://libraries.minecraft.net/com/mojang/datafixerupper/1.0.19/datafixerupper-1.0.19.jar", "c0f74b1916bc141b40f7cb491b63e1898f2c99bb", 431014L));

    /** Minecraft 1.13.2's client jar, which has every class a server has too. */
    private static final RuntimeFiles.Spec MINECRAFT = RuntimeFiles.Spec.minecraft("minecraft-1.13.2-client.jar", "1.13.2", "30bfe37a8db404db11c7edf02cb5165817afb4d9", 16089640L);
    /** MCPConfig, whose joined.tsrg gives Minecraft 1.13.2's obfuscated classes and members their SRG names. */
    private static final RuntimeFiles.Spec MCP_CONFIG = RuntimeFiles.Spec.url("mcp_config-1.13.2-20190213.203750.zip", "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_config/1.13.2-20190213.203750/mcp_config-1.13.2-20190213.203750.zip", "b2e3d59e19521e1040e11d97db22d41bb9065a38", 466835L);

    private static File dir(File gameDir) {
        return new File(gameDir, "portium/forge-1.13.2");
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

    /** Minecraft 1.13.2's client jar and MCPConfig, fetching them if missing or damaged. */
    static File[] resolveMinecraft(File gameDir) throws IOException {
        RuntimeFiles.request(dir(gameDir), Arrays.asList(MINECRAFT, MCP_CONFIG));
        return new File[] {RuntimeFiles.await(dir(gameDir), MINECRAFT), RuntimeFiles.await(dir(gameDir), MCP_CONFIG)};
    }

    /** Minecraft 1.13.2's client jar alone (for its textures and data), fetching it if missing (only during mod construction). */
    static File minecraftJar(File gameDir) throws IOException {
        if (!RuntimeFiles.isReady(dir(gameDir), MINECRAFT)) RuntimeFiles.request(dir(gameDir), java.util.Collections.singletonList(MINECRAFT));
        return RuntimeFiles.await(dir(gameDir), MINECRAFT);
    }

    private ModernRuntime() {}
}
