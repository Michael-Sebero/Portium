package com.michaelsebero.portium.datapack;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A data pack in a zip file, or the data under a prefix inside a jar (for the pack bundled with Portium). Files are
 * read eagerly so the zip isn't held open across reloads.
 */
public class ZipDataPack extends DataPack {

    private final File file;
    private final String prefix;

    public ZipDataPack(String id, File file, String prefix) {
        super(id);
        this.file = file;
        this.prefix = prefix;
    }

    @Nullable
    @Override
    public InputStream open(String path) throws IOException {
        try (ZipFile zip = new ZipFile(this.file)) {
            ZipEntry entry = zip.getEntry(this.prefix + path);
            if (entry == null) return null;
            try (InputStream stream = zip.getInputStream(entry)) {
                return new ByteArrayInputStream(readAll(stream));
            }
        }
    }

    private static byte[] readAll(InputStream stream) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) > 0) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    @Override
    public Collection<ResourceLocation> list(String folder, String extension) {
        List<ResourceLocation> ids = new ArrayList<>();
        try (ZipFile zip = new ZipFile(this.file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(this.prefix)) continue;
                ResourceLocation id = idFromPath(entry.getName().substring(this.prefix.length()), folder, extension);
                if (id != null) ids.add(id);
            }
        } catch (IOException ignored) {
            // A broken zip simply contributes nothing.
        }
        return ids;
    }
}
