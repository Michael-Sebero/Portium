package com.michaelsebero.portium.modern.mc.resources;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** 1.13's view of 1.12's client resource manager: what {@code Minecraft.getResourceManager()} gives 1.13 code. */
@SideOnly(Side.CLIENT)
public final class ModernResources implements IReloadableResourceManager {

    private static ModernResources instance;

    public static synchronized ModernResources get() {
        if (instance == null) instance = new ModernResources();
        return instance;
    }

    private ModernResources() {}

    private static net.minecraft.client.resources.IResourceManager legacy() {
        return Minecraft.getMinecraft().getResourceManager();
    }

    @Override
    public Set<String> getResourceNamespaces() {
        return legacy().getResourceDomains();
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        return wrap(legacy().getResource(location));
    }

    @Override
    public List<IResource> getAllResources(ResourceLocation location) throws IOException {
        List<IResource> all = new ArrayList<>();
        for (net.minecraft.client.resources.IResource resource : legacy().getAllResources(location)) all.add(wrap(resource));
        return all;
    }

    /**
     * Every file below {@code assets/<namespace>/<path>/} in the active packs whose name passes (Flywheel finds its
     * shaders so). 1.12's packs can't list themselves, so their files (jars and folders) are read directly.
     */
    @Override
    public Collection<ResourceLocation> getAllResourceLocations(String path, Predicate<String> filter) {
        String folder = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        java.util.Set<ResourceLocation> found = new java.util.TreeSet<>((a, b) -> a.toString().compareTo(b.toString()));
        for (java.io.File source : packFiles()) {
            try {
                if (source.isDirectory()) {
                    java.io.File[] namespaces = new java.io.File(source, "assets").listFiles(java.io.File::isDirectory);
                    if (namespaces == null) continue;
                    for (java.io.File namespace : namespaces) {
                        java.nio.file.Path root = namespace.toPath();
                        java.nio.file.Path start = root.resolve(folder);
                        if (!java.nio.file.Files.isDirectory(start)) continue;
                        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(start)) {
                            files.filter(java.nio.file.Files::isRegularFile).forEach(file -> {
                                String name = file.getFileName().toString();
                                if (!name.endsWith(".mcmeta") && filter.test(name)) {
                                    found.add(new ResourceLocation(namespace.getName(), root.relativize(file).toString().replace('\\', '/')));
                                }
                            });
                        }
                    }
                } else if (source.isFile()) {
                    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(source)) {
                        java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
                        while (entries.hasMoreElements()) {
                            java.util.zip.ZipEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (entry.isDirectory() || !name.startsWith("assets/") || name.endsWith(".mcmeta")) continue;
                            int slash = name.indexOf('/', 7);
                            if (slash < 0) continue;
                            String inNamespace = name.substring(slash + 1);
                            if (!inNamespace.startsWith(folder + "/")) continue;
                            if (filter.test(inNamespace.substring(inNamespace.lastIndexOf('/') + 1))) {
                                found.add(new ResourceLocation(name.substring(7, slash), inNamespace));
                            }
                        }
                    }
                }
            } catch (IOException | RuntimeException e) {
                // A pack that can't be read lists nothing.
            }
        }
        return new ArrayList<>(found);
    }

    /** The files (jars, zips, folders) behind the active resource packs. */
    private java.util.Set<java.io.File> packFiles() {
        java.util.Set<java.io.File> files = new java.util.LinkedHashSet<>();
        net.minecraft.client.resources.IResourceManager manager = legacy();
        if (!(manager instanceof net.minecraft.client.resources.SimpleReloadableResourceManager)) return files;
        try {
            java.util.Map<?, ?> domains = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(
                    net.minecraft.client.resources.SimpleReloadableResourceManager.class, (net.minecraft.client.resources.SimpleReloadableResourceManager) manager, "field_110548_a");
            for (Object fallback : new java.util.HashSet<>(domains.values())) {
                List<?> packs = net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(
                        net.minecraft.client.resources.FallbackResourceManager.class, (net.minecraft.client.resources.FallbackResourceManager) fallback, "field_110540_a");
                for (Object pack : packs) {
                    java.io.File file = fileOf(pack);
                    if (file != null) files.add(file);
                }
            }
        } catch (RuntimeException e) {
            // Nothing more to list.
        }
        return files;
    }

    @javax.annotation.Nullable
    private static java.io.File fileOf(Object pack) {
        for (Class<?> c = pack.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field field : c.getDeclaredFields()) {
                if (field.getType() != java.io.File.class || java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    java.io.File file = (java.io.File) field.get(pack);
                    if (file != null) return file;
                } catch (IllegalAccessException e) {
                    // the next
                }
            }
        }
        return null;
    }

    @Override
    public void addReloadListener(IResourceManagerReloadListener listener) {
        net.minecraft.client.resources.IResourceManager manager = legacy();
        if (manager instanceof net.minecraft.client.resources.IReloadableResourceManager) {
            ((net.minecraft.client.resources.IReloadableResourceManager) manager).registerReloadListener(reloaded -> listener.onResourceManagerReload(this));
        }
    }

    /**
     * As 1.14 and later add a listener: run at the next reload, not at once as 1.12 and 1.13 do (a mod's listener may
     * need what its constructor is still making). Mods' listeners added while they load run in the game's first load.
     */
    @SuppressWarnings("unchecked")
    public void addReloadListenerLater(IResourceManagerReloadListener listener) {
        net.minecraft.client.resources.IResourceManager manager = legacy();
        net.minecraft.client.resources.IResourceManagerReloadListener legacyListener = reloaded -> listener.onResourceManagerReload(this);
        if (manager instanceof net.minecraft.client.resources.SimpleReloadableResourceManager) {
            java.util.List<net.minecraft.client.resources.IResourceManagerReloadListener> listeners =
                    net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.client.resources.SimpleReloadableResourceManager.class,
                            (net.minecraft.client.resources.SimpleReloadableResourceManager) manager, "field_110546_b");
            listeners.add(legacyListener);
        } else if (manager instanceof net.minecraft.client.resources.IReloadableResourceManager) {
            ((net.minecraft.client.resources.IReloadableResourceManager) manager).registerReloadListener(legacyListener);
        }
    }

    private static IResource wrap(net.minecraft.client.resources.IResource resource) {
        return new IResource() {
            @Override
            public ResourceLocation getLocation() {
                return resource.getResourceLocation();
            }

            @Override
            public InputStream getInputStream() {
                return resource.getInputStream();
            }

            @Override
            public boolean hasMetadata() {
                return resource.hasMetadata();
            }

            @Override
            public String getPackName() {
                return resource.getResourcePackName();
            }

            @Override
            public void close() throws IOException {
                resource.close();
            }
        };
    }
}
