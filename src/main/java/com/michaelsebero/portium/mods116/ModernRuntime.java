package com.michaelsebero.portium.mods116;

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

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    static final String FORGE = "1.16.5-36.2.39";
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
            new Library(FORGE_MAVEN, "net/minecraftforge/forge/" + FORGE + "/forge-" + FORGE + "-universal.jar", "807540b167214a81684716fa0d6e92566ae14a04", 2504067),
            new Library(FORGE_MAVEN, "net/minecraftforge/forge/" + FORGE + "/forge-" + FORGE + "-launcher.jar", "0094f0546dba2d5bcee90e6a0a4f98d3adb40baf", 212574),
            new Library(FORGE_MAVEN, "net/minecraftforge/forgespi/3.2.0/forgespi-3.2.0.jar", "0c6ca4e4e4a0343701407c760e642537b613b543", 20172),
            new Library(FORGE_MAVEN, "net/minecraftforge/eventbus/4.0.0/eventbus-4.0.0.jar", "260e34800723e4c098c4e247cf2b900535e01b6d", 42873),
            new Library(FORGE_MAVEN, "net/minecraftforge/unsafe/0.2.0/unsafe-0.2.0.jar", "54d7a0a5e8fdb71b973025caa46f341ae5904f39", 2834),
            new Library(FORGE_MAVEN, "cpw/mods/modlauncher/8.1.3/modlauncher-8.1.3.jar", "6d0bfd78e025a87d43985e30907aa9ecbc51b2d6", 126747),
            new Library(FORGE_MAVEN, "net/jodah/typetools/0.8.3/typetools-0.8.3.jar", "98f84f353457629e81cc6827224871b1a8faa7af", 15425),
            new Library(CENTRAL, "com/electronwill/night-config/core/3.6.3/core-3.6.3.jar", "c601bfeaeb2c0abe7aaa901b0bbe6d1beff49281", 199091),
            new Library(CENTRAL, "com/electronwill/night-config/toml/3.6.3/toml-3.6.3.jar", "2b05b4d606c517da4d1a7b6d2b97d751c92887d7", 31288),
            // Mojang's libraries 1.16 brought or moved on from 1.12's (MIT licensed): commands, codecs and data fixers.
            new Library(MOJANG_LIBRARIES, "com/mojang/brigadier/1.0.17/brigadier-1.0.17.jar", "c6b7dc51dd44379cc751b7504816006e9be4b1e6", 77392),
            new Library(MOJANG_LIBRARIES, "com/mojang/datafixerupper/4.0.26/datafixerupper-4.0.26.jar", "ebd6690f33871ccee9b6132c6480668ee2e35020", 673183),
            // The collections library at the version 1.16 uses (Apache 2.0); 1.12's is older and lacks methods 1.16 calls.
            new Library(CENTRAL, "it/unimi/dsi/fastutil/8.2.1/fastutil-8.2.1.jar", "5ad88f325e424f8dbc2be5459e21ea5cab3864e9", 18800417)
    };

    /** Minecraft 1.16.5's client jar, which has every class a server has too. */
    private static final Library MINECRAFT = new Library(MOJANG, "v1/objects/37fd3c903861eeff3bc24b71eed48f828b5269c8/client.jar",
            "37fd3c903861eeff3bc24b71eed48f828b5269c8", 17547153, "minecraft-1.16.5-client.jar");
    /** MCPConfig, whose joined.tsrg gives Minecraft 1.16.5's obfuscated classes and members their SRG names. */
    private static final Library MCP_CONFIG = new Library(FORGE_MAVEN, "de/oceanlabs/mcp/mcp_config/1.16.5-20210115.111550/mcp_config-1.16.5-20210115.111550.zip",
            "a81464c686be42933aacac7898f8c0d50edccf51", 777652);

    /** The runtime's jars, downloading any that are missing or damaged. */
    static List<File> resolve(File gameDir) throws IOException {
        List<File> files = new ArrayList<>();
        for (Library library : LIBRARIES) files.add(resolve(gameDir, library));
        return files;
    }

    /** How many of the runtime's files (Minecraft's included) aren't downloaded yet, by a quick look at their sizes. */
    static int missing(File gameDir) {
        int count = 0;
        List<Library> all = new ArrayList<>(java.util.Arrays.asList(LIBRARIES));
        all.add(MINECRAFT);
        all.add(MCP_CONFIG);
        for (Library library : all) {
            File file = new File(new File(gameDir, "portium/forge-1.16.5"), library.fileName());
            if (!file.isFile() || file.length() != library.size) count++;
        }
        return count;
    }

    /** Minecraft 1.16.5's client jar alone (for its textures), downloading it if missing or damaged. */
    static File minecraftJar(File gameDir) throws IOException {
        return resolve(gameDir, MINECRAFT);
    }

    /** Minecraft 1.16.5's client jar and MCPConfig, downloading them if missing or damaged. */
    static File[] resolveMinecraft(File gameDir) throws IOException {
        return new File[] {resolve(gameDir, MINECRAFT), resolve(gameDir, MCP_CONFIG)};
    }

    private static File resolve(File gameDir, Library library) throws IOException {
        File file = new File(new File(gameDir, "portium/forge-1.16.5"), library.fileName());
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
