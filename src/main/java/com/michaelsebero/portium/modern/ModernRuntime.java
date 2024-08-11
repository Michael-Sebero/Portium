package com.michaelsebero.portium.modern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Forge 1.13.2 and the libraries it and Minecraft 1.13.2 brought that 1.12.2 lacks, and Minecraft 1.13.2 itself with MCPConfig's names for
 * it (see {@link ModernNatives}). Portium doesn't ship them: the first time 1.13 mods are installed, they're
 * downloaded from Forge's, Maven Central's and Mojang's servers into {@code portium/forge-1.13.2} in the game
 * directory, and checked against known hashes.
 */
final class ModernRuntime {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");
    static final String FORGE = "1.13.2-25.0.223";
    private static final String FORGE_MAVEN = "https://maven.minecraftforge.net/";
    private static final String CENTRAL = "https://repo1.maven.org/maven2/";
    private static final String MOJANG = "https://piston-data.mojang.com/";
    private static final String MOJANG_LIBRARIES = "https://libraries.minecraft.net/";

    private static final class Library {

        final String repository;
        final String path;
        final String sha1;
        final long size;
        final String fileName;

        Library(String repository, String path, String sha1, long size) {
            this(repository, path, sha1, size, path.substring(path.lastIndexOf('/') + 1));
        }

        Library(String repository, String path, String sha1, long size, String fileName) {
            this.repository = repository;
            this.path = path;
            this.sha1 = sha1;
            this.size = size;
            this.fileName = fileName;
        }

        String fileName() {
            return this.fileName;
        }
    }

    private static final Library[] LIBRARIES = {
            new Library(FORGE_MAVEN, "net/minecraftforge/forge/" + FORGE + "/forge-" + FORGE + "-universal.jar", "55ec5d86e208a0f3b91e31c1c3f1ef960f7e41d1", 2385532),
            new Library(FORGE_MAVEN, "net/minecraftforge/forge/" + FORGE + "/forge-" + FORGE + "-launcher.jar", "768790d34994d806935701002913cb7957993e5b", 159947),
            new Library(FORGE_MAVEN, "net/minecraftforge/forgespi/0.13.0/forgespi-0.13.0.jar", "2623344ba8cdb3e2e0a16be59c1fc22ad25f2981", 16607),
            new Library(FORGE_MAVEN, "net/minecraftforge/eventbus/0.9.3/eventbus-0.9.3-service.jar", "0a2ad95acb01a06ee6d263d3fe86750463ae5ec5", 39660),
            new Library(CENTRAL, "com/electronwill/night-config/core/3.6.0/core-3.6.0.jar", "0412f2edf0ef6cc9178ab38751af4e507f413bef", 199763),
            new Library(CENTRAL, "com/electronwill/night-config/toml/3.6.0/toml-3.6.0.jar", "f0c2c48748bdfc8d1f3798ec478f003bccc7e0b1", 31256),
            new Library(CENTRAL, "net/jodah/typetools/0.6.0/typetools-0.6.0.jar", "a1552ecd6f5b9444585a7c4b05f6312cf7d32fd3", 14734),
            // Mojang's libraries 1.13 brought (MIT licensed): commands, and data fixers.
            new Library(MOJANG_LIBRARIES, "com/mojang/brigadier/1.0.14/brigadier-1.0.14.jar", "a18df0522b577ca364b87019c1efa0b8faa12fe5", 73350),
            new Library(MOJANG_LIBRARIES, "com/mojang/datafixerupper/1.0.19/datafixerupper-1.0.19.jar", "c0f74b1916bc141b40f7cb491b63e1898f2c99bb", 431014)
    };

    /** Minecraft 1.13.2's client jar, which has every class a server has too. */
    private static final Library MINECRAFT = new Library(MOJANG, "v1/objects/30bfe37a8db404db11c7edf02cb5165817afb4d9/client.jar",
            "30bfe37a8db404db11c7edf02cb5165817afb4d9", 16089640, "minecraft-1.13.2-client.jar");
    /** MCPConfig, whose joined.tsrg gives Minecraft 1.13.2's obfuscated classes and members their SRG names. */
    private static final Library MCP_CONFIG = new Library(FORGE_MAVEN, "de/oceanlabs/mcp/mcp_config/1.13.2-20190213.203750/mcp_config-1.13.2-20190213.203750.zip",
            "b2e3d59e19521e1040e11d97db22d41bb9065a38", 466835);

    /** The runtime's jars, downloading any that are missing or damaged. */
    static List<File> resolve(File gameDir) throws IOException {
        List<File> files = new ArrayList<>();
        for (Library library : LIBRARIES) files.add(resolve(gameDir, library));
        return files;
    }

    /** Minecraft 1.13.2's client jar and MCPConfig, downloading them if missing or damaged. */
    static File[] resolveMinecraft(File gameDir) throws IOException {
        return new File[] {resolve(gameDir, MINECRAFT), resolve(gameDir, MCP_CONFIG)};
    }

    private static File resolve(File gameDir, Library library) throws IOException {
        File file = new File(new File(gameDir, "portium/forge-1.13.2"), library.fileName());
        if (!file.isFile() || file.length() != library.size || !library.sha1.equals(sha1(file))) download(library, file);
        return file;
    }

    private static void download(Library library, File file) throws IOException {
        URL url = new URL(library.repository + library.path);
        LOGGER.info("Downloading {} ({} KB) from {}", library.fileName(), library.size / 1024, url);
        Files.createDirectories(file.getParentFile().toPath());
        File partial = new File(file.getPath() + ".part");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("User-Agent", "Portium");
        String megabytes = String.format(java.util.Locale.ROOT, "%.1f MB", library.size / 1048576.0);
        try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(partial);
             com.michaelsebero.portium.util.Progress progress = com.michaelsebero.portium.util.Progress.start(
                     "Portium: downloading " + library.fileName() + " (" + megabytes + ", first launch only)", 20)) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > library.size) throw new IOException(url + " is larger than expected");
                out.write(buffer, 0, read);
                progress.reach((double) total / library.size, String.format(java.util.Locale.ROOT, "%d%% of %s", total * 100 / library.size, megabytes));
            }
        } finally {
            connection.disconnect();
        }
        String hash = sha1(partial);
        if (!library.sha1.equals(hash)) {
            Files.deleteIfExists(partial.toPath());
            throw new IOException(url + " has hash " + hash + ", expected " + library.sha1);
        }
        Files.move(partial.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sha1(File file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder(40);
        for (byte b : digest.digest()) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private ModernRuntime() {}
}
