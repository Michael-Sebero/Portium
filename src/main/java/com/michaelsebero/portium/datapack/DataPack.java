package com.michaelsebero.portium.datapack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * A data pack: a folder or zip holding {@code pack.mcmeta} and a {@code data/<namespace>/<type>/...} tree, as in
 * 1.13. Packs later in the enabled list override earlier ones.
 */
public abstract class DataPack implements Closeable {

    /** 1.13-1.14 packs are format 4, 1.15 is 5 and 1.16 is 6; all are read. */
    public static final int CURRENT_FORMAT = 4;

    private final String id;
    private ITextComponent description = new TextComponentString("");
    private int format = CURRENT_FORMAT;

    protected DataPack(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public ITextComponent getDescription() {
        return this.description;
    }

    public int getFormat() {
        return this.format;
    }

    /** Reads {@code pack.mcmeta}; false if it's missing or broken, in which case this isn't a pack. */
    public boolean readMetadata() {
        try (InputStream stream = this.open("pack.mcmeta")) {
            if (stream == null) return false;
            JsonObject pack = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("pack");
            if (pack == null) return false;
            this.format = pack.has("pack_format") ? pack.get("pack_format").getAsInt() : CURRENT_FORMAT;
            if (pack.has("description")) {
                ITextComponent text = ITextComponent.Serializer.fromJsonLenient(pack.get("description").toString());
                if (text != null) this.description = text;
            }
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Opens a file by its path inside the pack, or returns null if the pack doesn't have it. */
    @Nullable
    public abstract InputStream open(String path) throws IOException;

    /** Every file under {@code data/<namespace>/<folder>/} with the extension, as namespaced ids without it. */
    public abstract Collection<ResourceLocation> list(String folder, String extension);

    @Nullable
    public Reader openData(ResourceLocation id, String folder, String extension) throws IOException {
        InputStream stream = this.open("data/" + id.getNamespace() + "/" + folder + "/" + id.getPath() + extension);
        return stream == null ? null : new InputStreamReader(stream, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {}

    /** Strips {@code data/<namespace>/<folder>/} and the extension from a path, giving the resource id. */
    @Nullable
    protected static ResourceLocation idFromPath(String path, String folder, String extension) {
        if (!path.startsWith("data/") || !path.endsWith(extension)) return null;
        String rest = path.substring("data/".length());
        int slash = rest.indexOf('/');
        if (slash < 0) return null;
        String namespace = rest.substring(0, slash);
        String inside = rest.substring(slash + 1);
        if (!inside.startsWith(folder + "/")) return null;
        String name = inside.substring(folder.length() + 1, inside.length() - extension.length());
        if (name.isEmpty()) return null;
        return new ResourceLocation(namespace, name);
    }
}
