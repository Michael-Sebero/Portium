package com.michaelsebero.portium.util;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The jars Portium's 1.13 and 1.16.5 mod layers run on (Forge, its libraries, Minecraft itself), which Portium doesn't
 * ship. Portium has no network code of its own: every jar is fetched by AssetMover 3 (see {@link AssetMoverBridge}),
 * which downloads all the missing ones in parallel from the official servers (Forge's and Maven Central's, or Mojang's
 * for Minecraft) and checks each against its SHA-1. Portium then checks the size once more and moves the file into the
 * layer's folder. A jar that fails a check is never kept.
 * <p>
 * A jar that has been verified once is remembered (by its size and modification time, in a small {@code .ok} file
 * beside it), so later launches don't read and hash it again. Jars that earlier Portium versions packed together from
 * AssetMover's files are still accepted: they carry a marker with the digest of their contents, which comes with
 * Portium ({@code portium/runtime/<jar>.txt}, made by {@code tools/gen_runtime_lists.py}).
 */
public final class RuntimeFiles {

    private static final Logger LOGGER = LogManager.getLogger("Portium/Runtime");
    /** Where AssetMover is asked to put each jar (inside its own {@code assetmover} folder), until it is checked and moved. */
    private static final String STAGING = "portium-runtime/";
    /** Jars verified during this run, by path, so none is hashed twice. The value is {@link #stamp}. */
    private static final ConcurrentHashMap<String, String> VERIFIED = new ConcurrentHashMap<>();

    /** One jar: its file name here, where AssetMover gets it, and the original's hash and size. */
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

        /** Minecraft's client jar of a version, from Mojang. */
        public static Spec minecraft(String fileName, String version, String sha1, long size) {
            return new Spec(fileName, null, version, sha1, size);
        }

        public String fileName() {
            return this.fileName;
        }

        /** Where AssetMover downloads the jar from (Mojang names a client jar's download after its SHA-1). */
        String downloadUrl() {
            return this.url != null ? this.url : "https://piston-data.mojang.com/v1/objects/" + this.originalSha1 + "/client.jar";
        }
    }

    private RuntimeFiles() {}

    /** AssetMover's folder, where it puts what it fetches. */
    private static File assetMoverDir() {
        return new File(Launch.minecraftHome != null ? Launch.minecraftHome : new File("."), "assetmover");
    }

    private static File staged(File dir, Spec spec) {
        return new File(assetMoverDir(), STAGING + dir.getName() + "/" + spec.fileName);
    }

    /** The digest of the contents of a jar that earlier Portium versions packed, from the file list that comes with Portium. */
    private static String packedDigest(Spec spec) throws IOException {
        String resource = "/portium/runtime/" + spec.fileName + ".txt";
        InputStream in = RuntimeFiles.class.getResourceAsStream(resource);
        if (in == null) throw new IOException("Portium lacks its file list " + resource);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || !header.startsWith("sha256 ")) throw new IOException("Bad file list " + resource);
            return header.substring(7).trim();
        }
    }

    private static File marker(File jar) {
        return new File(jar.getPath() + ".sha256");
    }

    private static File okMarker(File jar) {
        return new File(jar.getPath() + ".ok");
    }

    /** What a verified jar looks like on disk; a jar that changes size or time has to be checked again. */
    private static String stamp(File jar, Spec spec) {
        return jar.length() + ":" + jar.lastModified() + ":" + spec.originalSha1;
    }

    private static void remember(File jar, Spec spec) {
        String stamp = stamp(jar, spec);
        VERIFIED.put(jar.getAbsolutePath(), stamp);
        try {
            Files.write(okMarker(jar).toPath(), stamp.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Only costs a check on the next launch.
        }
    }

    /**
     * Whether the jar is here and whole: the original download, or one packed and checked by an earlier Portium version.
     * A jar that was verified before and hasn't changed since is accepted without reading it.
     */
    public static boolean isReady(File dir, Spec spec) {
        File jar = new File(dir, spec.fileName);
        if (!jar.isFile()) return false;
        try {
            String stamp = stamp(jar, spec);
            if (stamp.equals(VERIFIED.get(jar.getAbsolutePath()))) return true;
            File ok = okMarker(jar);
            if (ok.isFile() && stamp.equals(new String(Files.readAllBytes(ok.toPath()), StandardCharsets.UTF_8).trim())) {
                VERIFIED.put(jar.getAbsolutePath(), stamp);
                return true;
            }
            if (jar.length() == spec.originalSize && spec.originalSha1.equals(sha1(jar))) {
                remember(jar, spec);
                return true;
            }
            File marker = marker(jar);
            return marker.isFile() && new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim().equals(packedDigest(spec));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Has AssetMover fetch every jar that isn't here yet (or is damaged), all at once, with its bar on the loading
     * screen; each is checked before it is kept. Does nothing, and reads nothing, when all are ready.
     *
     * @return how many jars had to be fetched
     */
    public static int request(File dir, List<Spec> specs) throws IOException {
        List<Spec> missing = new ArrayList<>();
        for (Spec spec : specs) if (!isReady(dir, spec)) missing.add(spec);
        if (missing.isEmpty()) return 0;
        Files.createDirectories(dir.toPath());
        List<String[]> requests = new ArrayList<>();
        for (Spec spec : missing) {
            Files.deleteIfExists(staged(dir, spec).toPath());
            requests.add(new String[] {spec.downloadUrl(), STAGING + dir.getName() + "/" + spec.fileName, spec.originalSha1});
        }
        LOGGER.info("Asking AssetMover for {} file(s) of Portium's runtime", missing.size());
        AssetMoverBridge.fetch(requests);
        IOException failure = null;
        for (Spec spec : missing) {
            try {
                accept(dir, spec);
            } catch (IOException e) {
                if (failure == null) failure = e;
                LOGGER.error("{} wasn't fetched: {}", spec.fileName, e.getMessage());
            }
        }
        deleteTree(new File(assetMoverDir(), STAGING + dir.getName()).toPath());
        if (failure != null) throw failure;
        return missing.size();
    }

    /** Checks what AssetMover staged for a jar, and moves it into place. */
    private static void accept(File dir, Spec spec) throws IOException {
        File file = staged(dir, spec);
        if (!file.isFile()) throw new IOException("AssetMover didn't fetch " + spec.fileName + " (see its log). Check the connection and restart, or put "
                + spec.fileName + " into " + dir);
        if (file.length() != spec.originalSize || !spec.originalSha1.equals(sha1(file))) {
            throw new IOException(spec.fileName + " doesn't match the file Portium expects (" + file.length() + " bytes, expected " + spec.originalSize
                    + "). Restart to try again, or put the right " + spec.fileName + " into " + dir);
        }
        File jar = new File(dir, spec.fileName);
        Files.move(file.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        // A marker left by an earlier, packed copy of this jar says nothing about the one just fetched.
        Files.deleteIfExists(marker(jar).toPath());
        remember(jar, spec);
        LOGGER.info("{} is ready", spec.fileName);
    }

    /** The jar, fetched first (by AssetMover) if it isn't here yet or is damaged; what this returns is the file Portium expects. */
    public static synchronized File await(File dir, Spec spec) throws IOException {
        File jar = new File(dir, spec.fileName);
        if (isReady(dir, spec)) return jar;
        request(dir, java.util.Collections.singletonList(spec));
        return jar;
    }

    private static String sha1(File file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder(40);
        for (byte b : digest.digest()) hex.append(String.format("%02x", b));
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
