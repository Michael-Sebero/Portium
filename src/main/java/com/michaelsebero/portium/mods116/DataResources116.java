package com.michaelsebero.portium.mods116;

import com.michaelsebero.portium.datapack.DataPack;
import com.michaelsebero.portium.datapack.DataPackManager;
import com.michaelsebero.portium.modern.mc.resources.IReloadableResourceManager;
import com.michaelsebero.portium.modern.mc.resources.IResource;
import com.michaelsebero.portium.modern.mc.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * The server's data, as 1.16's resource manager: what 1.16 mods' reload listeners read their own data files from
 * (anything under {@code data/<namespace>/}), out of the data packs Portium has loaded, the mods' own among them.
 * One per reload: what it lists is remembered.
 */
public final class DataResources116 implements IReloadableResourceManager {

    private final DataPackManager packs;
    private final Map<String, Collection<ResourceLocation>> listings = new HashMap<>();
    /** Listeners 1.16 code added to this manager itself rather than through Forge's event (1.16's or 1.13's kind). */
    private final List<Object> listeners = new ArrayList<>();

    public DataResources116(DataPackManager packs) {
        this.packs = packs;
    }

    public List<Object> listeners() {
        return this.listeners;
    }

    public void addListener(Object listener) {
        this.listeners.add(listener);
    }

    @Override
    public Set<String> getResourceNamespaces() {
        Set<String> namespaces = new LinkedHashSet<>();
        for (String folder : new String[] {"recipes", "tags", "loot_tables", "advancements"}) {
            for (ResourceLocation id : this.list(folder)) namespaces.add(id.getNamespace());
        }
        return namespaces;
    }

    private static String path(ResourceLocation location) {
        return "data/" + location.getNamespace() + "/" + location.getPath();
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        List<DataPack> loaded = this.packs.loaded();
        for (int i = loaded.size() - 1; i >= 0; i--) {
            InputStream stream = loaded.get(i).open(path(location));
            if (stream != null) return resource(location, stream, loaded.get(i).getId());
        }
        throw new FileNotFoundException(location.toString());
    }

    @Override
    public List<IResource> getAllResources(ResourceLocation location) throws IOException {
        List<IResource> all = new ArrayList<>();
        for (DataPack pack : this.packs.loaded()) {
            InputStream stream = pack.open(path(location));
            if (stream != null) all.add(resource(location, stream, pack.getId()));
        }
        if (all.isEmpty()) throw new FileNotFoundException(location.toString());
        return all;
    }

    private Collection<ResourceLocation> list(String folder) {
        return this.listings.computeIfAbsent(folder, key -> this.packs.list(key, ""));
    }

    /** Every file below {@code data/<namespace>/<path>/} whose name passes, by its id from the namespace down. */
    @Override
    public Collection<ResourceLocation> getAllResourceLocations(String path, Predicate<String> filter) {
        Set<ResourceLocation> found = new TreeSet<>((a, b) -> a.toString().compareTo(b.toString()));
        for (ResourceLocation id : this.list(path)) {
            String file = id.getPath();
            if (filter.test(file.substring(file.lastIndexOf('/') + 1))) found.add(new ResourceLocation(id.getNamespace(), path + "/" + file));
        }
        return new ArrayList<>(found);
    }

    @Override
    public void addReloadListener(IResourceManagerReloadListener listener) {
        this.listeners.add(listener);
    }

    private static IResource resource(ResourceLocation location, InputStream stream, String pack) {
        return new IResource() {
            @Override
            public ResourceLocation getLocation() {
                return location;
            }

            @Override
            public InputStream getInputStream() {
                return stream;
            }

            @Override
            public boolean hasMetadata() {
                return false;
            }

            @Override
            public String getPackName() {
                return pack;
            }

            @Override
            public void close() throws IOException {
                stream.close();
            }
        };
    }
}
