package com.michaelsebero.portium.util;

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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * The jars Portium's 1.13 and 1.16.5 mod layers run on (Forge, its libraries, Minecraft itself), which Portium doesn't
 * ship. The first time a layer has mods, each missing jar is downloaded from its official server (Forge's and Maven
 * Central's, or Mojang's for Minecraft) straight into the layer's folder, and checked against the SHA-1 and size of the
 * original before it is used. A jar that fails the check is never kept.
 * <p>
 * The jars are fetched here and not through AssetMover because they are needed at once, during mod construction.
 * AssetMover 2.5's {@code fromUrlFile} fails on every URL (it hands the URL's path to {@code Files.createTempFile} as a
 * file name prefix), and AssetMover 3 only queues its 2.x requests until construction is over, and never runs them on a
 * dedicated server, so waiting here for what they would fetch could never end.
 * <p>
 * Jars that earlier Portium versions packed together from AssetMover's files are still accepted: they carry a marker
 * with the digest of their contents, which comes with Portium ({@code portium/runtime/<jar>.txt}, made by
 * {@code tools/gen_runtime_lists.py}).
 */
public final class RuntimeFiles {

    private static final Logger LOGGER = LogManager.getLogger("Portium/Runtime");
    /** Mojang names a client jar's download after its SHA-1. */
    private static final String MOJANG_CLIENT_URL = "https://piston-data.mojang.com/v1/objects/%s/client.jar";
    private static final String USER_AGENT = "Portium (Minecraft 1.12.2 mod)";
    private static final int ATTEMPTS = 3;
    private static final int CONNECT_TIMEOUT_MILLIS = 15 * 1000;
    private static final int READ_TIMEOUT_MILLIS = 30 * 1000;
    private static final long RETRY_DELAY_MILLIS = 2000L;

    /** One jar: its file name here, where it comes from, and the original's hash and size. */
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

        /** Where the jar is downloaded from. */
        String downloadUrl() {
            return this.url != null ? this.url : String.format(MOJANG_CLIENT_URL, this.originalSha1);
        }
    }

    /** A download that arrived whole but isn't the file Portium expects, which asking again won't change. */
    private static final class ChecksumException extends IOException {

        private static final long serialVersionUID = 1L;

        ChecksumException(String message) {
            super(message);
        }
    }

    private RuntimeFiles() {}

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

    /** Whether the jar is here and whole: the original download, or one packed and checked by an earlier Portium version. */
    public static boolean isReady(File dir, Spec spec) {
        File jar = new File(dir, spec.fileName);
        if (!jar.isFile()) return false;
        try {
            if (jar.length() == spec.originalSize && spec.originalSha1.equals(hash(jar, "SHA-1"))) return true;
            File marker = marker(jar);
            return marker.isFile() && new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim().equals(packedDigest(spec));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Counts the jars that aren't here yet, so a caller can say up front what it needs. They are downloaded by
     * {@link #await}, one at a time, as each is asked for, so the loading screen names the jar that is coming.
     *
     * @return how many of the jars will have to be downloaded
     */
    public static int request(File dir, List<Spec> specs) throws IOException {
        int missing = 0;
        for (Spec spec : specs) if (!isReady(dir, spec)) missing++;
        return missing;
    }

    /**
     * The jar, downloaded first (showing progress) if it isn't here yet or is damaged. It is checked before it is
     * kept, so what this returns is the file Portium expects.
     */
    public static synchronized File await(File dir, Spec spec) throws IOException {
        File jar = new File(dir, spec.fileName);
        if (isReady(dir, spec)) return jar;
        download(spec, jar);
        return jar;
    }

    private static void download(Spec spec, File jar) throws IOException {
        Files.createDirectories(jar.getParentFile().toPath());
        File partial = new File(jar.getPath() + ".part");
        String address = spec.downloadUrl();
        if (!address.startsWith("https://")) throw new IOException("Refusing to download " + spec.fileName + " from " + address + ", which isn't https");
        IOException failure = null;
        try (Progress progress = Progress.start("Portium: downloading " + spec.fileName + " (first launch only)", 20)) {
            for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
                try {
                    LOGGER.info("Downloading {} from {}", spec.fileName, address);
                    fetch(address, partial, spec, progress);
                    Files.move(partial.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    // A marker left by an earlier, packed copy of this jar says nothing about the one just downloaded.
                    Files.deleteIfExists(marker(jar).toPath());
                    LOGGER.info("{} is ready", spec.fileName);
                    return;
                } catch (ChecksumException e) {
                    deletePartial(partial);
                    throw new IOException(e.getMessage() + " Restart to try again, or put the right " + spec.fileName + " into " + jar.getParent(), e);
                } catch (IOException e) {
                    failure = e;
                    deletePartial(partial);
                    LOGGER.warn("Couldn't download {} (attempt {} of {}): {}", spec.fileName, attempt, ATTEMPTS, e.toString());
                    if (attempt < ATTEMPTS) pause(RETRY_DELAY_MILLIS * attempt, spec);
                }
            }
        }
        throw new IOException("Couldn't download " + spec.fileName + " from " + address + " (" + failure + "). Check the connection and restart, "
                + "or put " + spec.fileName + " into " + jar.getParent(), failure);
    }

    /** Downloads {@code address} into {@code to}, which must come out the size and SHA-1 of the spec's original. */
    private static void fetch(String address, File to, Spec spec, Progress progress) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("the server answered " + status + " " + connection.getResponseMessage());
            MessageDigest sha1 = messageDigest("SHA-1");
            long received = 0L;
            try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(to)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    sha1.update(buffer, 0, read);
                    received += read;
                    progress.reach(Math.min(1.0D, (double) received / Math.max(1L, spec.originalSize)), (received / 1024L) + " of " + (spec.originalSize / 1024L) + " KB");
                }
            }
            if (received != spec.originalSize) throw new IOException("got " + received + " bytes, not the " + spec.originalSize + " expected");
            String actual = hex(sha1.digest());
            if (!actual.equals(spec.originalSha1)) {
                throw new ChecksumException(spec.fileName + " from " + address + " doesn't match the file Portium expects (SHA-1 " + actual + ", not " + spec.originalSha1 + ").");
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void deletePartial(File partial) {
        try {
            Files.deleteIfExists(partial.toPath());
        } catch (IOException e) {
            LOGGER.warn("Couldn't remove {}", partial, e);
        }
    }

    private static void pause(long millis, Spec spec) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + spec.fileName, e);
        }
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
}
