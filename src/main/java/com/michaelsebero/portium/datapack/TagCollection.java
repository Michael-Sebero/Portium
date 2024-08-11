package com.michaelsebero.portium.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.michaelsebero.portium.Portium;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The tags of one type, merged across packs as in 1.13: each pack adds to a tag unless it sets {@code replace}.
 * Values are ids or {@code #other_tag} references; ids 1.12 has no equivalent for are skipped with a warning.
 */
public final class TagCollection<T> {

    private final String folder;
    private final Function<ResourceLocation, T> resolver;
    private Map<ResourceLocation, Tag<T>> tags = new HashMap<>();

    public TagCollection(String folder, Function<ResourceLocation, T> resolver) {
        this.folder = folder;
        this.resolver = resolver;
    }

    @Nullable
    public Tag<T> get(ResourceLocation id) {
        return this.tags.get(id);
    }

    public Collection<ResourceLocation> getIds() {
        return this.tags.keySet();
    }

    public void load(List<DataPack> packs) {
        Map<ResourceLocation, List<String>> raw = new LinkedHashMap<>();
        for (DataPack pack : packs) {
            for (ResourceLocation id : pack.list("tags/" + this.folder, ".json")) {
                try (Reader reader = pack.openData(id, "tags/" + this.folder, ".json")) {
                    if (reader == null) continue;
                    JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
                    List<String> entries = raw.computeIfAbsent(id, key -> new ArrayList<>());
                    if (json.has("replace") && json.get("replace").getAsBoolean()) entries.clear();
                    JsonArray values = json.getAsJsonArray("values");
                    if (values == null) continue;
                    for (JsonElement value : values) {
                        // 1.16.2 allows {"id": ..., "required": false}.
                        entries.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
                    }
                } catch (IOException | RuntimeException e) {
                    Portium.LOGGER.error("Couldn't read {} tag {} from data pack {}", this.folder, id, pack.getId(), e);
                }
            }
        }
        Map<ResourceLocation, Tag<T>> resolved = new HashMap<>();
        for (ResourceLocation id : raw.keySet()) this.resolve(id, raw, resolved, new HashSet<>());
        this.tags = resolved;
    }

    private List<T> resolve(ResourceLocation id, Map<ResourceLocation, List<String>> raw, Map<ResourceLocation, Tag<T>> resolved, Set<ResourceLocation> visiting) {
        Tag<T> done = resolved.get(id);
        if (done != null) return done.getValues();
        if (!visiting.add(id)) {
            Portium.LOGGER.error("The {} tag {} includes itself", this.folder, id);
            return new ArrayList<>();
        }
        List<T> values = new ArrayList<>();
        for (String entry : raw.getOrDefault(id, new ArrayList<>())) {
            if (entry.startsWith("#")) {
                ResourceLocation reference = new ResourceLocation(entry.substring(1));
                if (raw.containsKey(reference)) values.addAll(this.resolve(reference, raw, resolved, visiting));
                else Portium.LOGGER.warn("The {} tag {} refers to the missing tag {}", this.folder, id, reference);
                continue;
            }
            T value = this.resolver.apply(new ResourceLocation(entry));
            if (value == null) Portium.LOGGER.warn("The {} tag {} lists {}, which doesn't exist here", this.folder, id, entry);
            else if (!values.contains(value)) values.add(value);
        }
        visiting.remove(id);
        resolved.put(id, new Tag<>(id, values));
        return values;
    }
}
