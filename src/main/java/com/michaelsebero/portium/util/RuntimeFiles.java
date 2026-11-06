package com.michaelsebero.portium.util;

import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The jars Portium's 1.13 and 1.16.5 mod layers run on (Forge, its libraries, Minecraft itself), which Portium doesn't
 * ship. Portium has no network code of its own: AssetMover fetches each jar's files from the official servers (Forge's
 * and Maven Central's, or Mojang's for Minecraft) during mod construction, and Portium packs them back into the jar
 * under {@code portium/forge-<version>}, checked against a digest of the original's contents. Which files each jar
 * has, and the digest, come with Portium ({@code portium/runtime/<jar>.txt}, made by {@code tools/gen_runtime_lists.py}).
 */
public final class RuntimeFiles {

    private static final Logger LOGGER = LogManager.getLogger("Portium/Runtime");
    /** Where AssetMover puts the fetched files (inside its own {@code assetmover} folder). */
    private static final String STAGING = "portium-runtime/";
    private static final long TIMEOUT_MILLIS = 20L * 60L * 1000L;
    /** Jars already asked for this launch (asking twice would fetch twice). */
    private static final java.util.Set<String> REQUESTED = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /** One jar: its file name here, where AssetMover gets it, and the original's hash (jars downloaded by earlier Portium versions are kept). */
    public static final class Spec {

        final String fileName;
        @Nullable
        final String url;
        @Nullable
        final String minecraftVersion;
        final String originalSha1;
        final long originalSize;

        private Spec(String fileName, @Nullable String url, @Nullable String minecraftVersion, String originalSha1, long originalSize) {
            this.fileName = fileName;
            this.url = url;
            this.minecraftVersion = minecraftVersion;
            this.originalSha1 = originalSha1;
            this.originalSize = originalSize;
        }

        /** A jar from a Maven repository or other URL. */
        public static Spec url(String fileName, String url, String sha1, long size) {
            return new Spec(fileName, url, null, sha1, size);
        }

        /** Minecraft's client jar of a version, from Mojang through AssetMover's version lookup. */
        public static Spec minecraft(String fileName, String version, String sha1, long size) {
            return new Spec(fileName, null, version, sha1, size);
        }

        public String fileName() {
            return this.fileName;
        }
    }

    private RuntimeFiles() {}

    private static File assetMoverDir() {
        return new File("assetmover");
    }

    /** The file list and digest that come with Portium for a jar. */
    private static final class Listing {

        final String digest;
        final List<String> entries;

        Listing(String digest, List<String> entries) {
            this.digest = digest;
            this.entries = entries;
        }
    }

    private static Listing listing(Spec spec) throws IOException {
        String resource = "/portium/runtime/" + spec.fileName + ".txt";
        InputStream in = RuntimeFiles.class.getResourceAsStream(resource);
        if (in == null) throw new IOException("Portium lacks its file list " + resource);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.startsWith("sha256 ")) throw new IOException("Bad file list " + resource);
            List<String> entries = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) if (!line.isEmpty()) entries.add(line);
            return new Listing(header.substring(7).trim(), entries);
        }
    }

    private static File marker(File jar) {
        return new File(jar.getPath() + ".sha256");
    }

    /** Whether the jar is here and whole: the original download, or one packed and checked before. */
    public static boolean isReady(File dir, Spec spec) {
        File jar = new File(dir, spec.fileName);
        if (!jar.isFile()) return false;
        try {
            if (jar.length() == spec.originalSize && spec.originalSha1.equals(hash(jar, "SHA-1"))) return true;
            File marker = marker(jar);
            return marker.isFile() && new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim().equals(listing(spec).digest);
        } catch (IOException e) {
            return false;
        }
    }

    /** Whether AssetMover is there to fetch anything. */
    public static boolean canFetch() {
        return Loader.isModLoaded("assetmover");
    }

    /**
     * Asks AssetMover for the files of every jar that isn't here yet. Only during mod construction (AssetMover's
     * rule); the files arrive in the background, and {@link #await} waits for them.
     *
     * @return how many jars are being fetched
     */
    public static int request(File dir, List<Spec> specs) throws IOException {
        int requested = 0;
        for (Spec spec : specs) {
            if (REQUESTED.contains(spec.fileName) || isReady(dir, spec)) continue;
            if (!canFetch()) throw new IOException("AssetMover isn't installed, so " + spec.fileName + " can't be fetched. Install AssetMover, "
                    + "or put " + spec.fileName + " into " + dir);
            Map<String, String> files = new LinkedHashMap<>();
            for (String entry : listing(spec).entries) files.put(entry, STAGING + spec.fileName + "/" + entry);
            LOGGER.info("Asking AssetMover for {} ({} files)", spec.fileName, files.size());
            if (spec.minecraftVersion != null) {
                com.cleanroommc.assetmover.AssetMoverAPI.fromMinecraft(spec.minecraftVersion, files);
            } else {
                com.cleanroommc.assetmover.AssetMoverAPI.fromUrlFile(spec.url, files);
            }
            REQUESTED.add(spec.fileName);
            requested++;
        }
        return requested;
    }

    /**
     * The jar, once AssetMover has fetched its files: packed from them and checked. Waits (showing progress) while
     * they're still coming.
     */
    public static File await(File dir, Spec spec) throws IOException {
        File jar = new File(dir, spec.fileName);
        if (isReady(dir, spec)) return jar;
        Listing listing = listing(spec);
        File staged = new File(assetMoverDir(), STAGING + spec.fileName);
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        try (Progress progress = Progress.start("Portium: fetching " + spec.fileName + " (first launch only)", 20)) {
            while (true) {
                int present = 0;
                for (String entry : listing.entries) if (new File(staged, entry).isFile()) present++;
                progress.reach((double) present / listing.entries.size(), present + " of " + listing.entries.size() + " files");
                // AssetMover copies the files one by one; the digest says when the last one is whole.
                if (present == listing.entries.size() && listing.digest.equals(digest(staged, listing.entries))) break;
                if (System.currentTimeMillis() > deadline) {
                    throw new IOException(spec.fileName + " wasn't fetched (" + present + " of " + listing.entries.size()
                            + " files; see AssetMover's log). Check the connection and restart.");
                }
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for " + spec.fileName, e);
                }
            }
        }
        pack(staged, listing.entries, jar);
        Files.write(marker(jar).toPath(), listing.digest.getBytes(StandardCharsets.UTF_8));
        deleteTree(staged.toPath());
        LOGGER.info("{} is ready", spec.fileName);
        return jar;
    }

    private static void pack(File staged, List<String> entries, File jar) throws IOException {
        Files.createDirectories(jar.getParentFile().toPath());
        File partial = new File(jar.getPath() + ".part");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(partial))) {
            byte[] buffer = new byte[16384];
            for (String entry : entries) {
                out.putNextEntry(new ZipEntry(entry));
                try (InputStream in = new FileInputStream(new File(staged, entry))) {
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                }
                out.closeEntry();
            }
        }
        Files.move(partial.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /** The digest {@code tools/gen_runtime_lists.py} writes: entries by name (as UTF-8 bytes), each name, 0, length, data. */
    private static String digest(File staged, List<String> entries) throws IOException {
        List<String> sorted = new ArrayList<>(entries);
        sorted.sort((a, b) -> compareBytes(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)));
        MessageDigest digest = messageDigest("SHA-256");
        byte[] length = new byte[8];
        for (String entry : sorted) {
            byte[] data = Files.readAllBytes(new File(staged, entry).toPath());
            digest.update(entry.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            long size = data.length;
            for (int i = 7; i >= 0; i--) {
                length[i] = (byte) size;
                size >>>= 8;
            }
            digest.update(length);
            digest.update(data);
        }
        return hex(digest.digest());
    }

    private static int compareBytes(byte[] a, byte[] b) {
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            int difference = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (difference != 0) return difference;
        }
        return a.length - b.length;
    }

    private static String hash(File file, String algorithm) throws IOException {
        MessageDigest digest = messageDigest(algorithm);
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return hex(digest.digest());
    }

    private static MessageDigest messageDigest(String algorithm) throws IOException {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path directory, IOException e) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Couldn't clear {}", root, e);
        }
    }
}
