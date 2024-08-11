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

    /** 1.12 can't list a pack's files. */
    @Override
    public Collection<ResourceLocation> getAllResourceLocations(String path, Predicate<String> filter) {
        return Collections.emptyList();
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
