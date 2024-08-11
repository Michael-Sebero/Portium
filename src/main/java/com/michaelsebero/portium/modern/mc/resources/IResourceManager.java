package com.michaelsebero.portium.modern.mc.resources;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** 1.13's {@code net.minecraft.resources.IResourceManager} (1.12's client one, wrapped; see {@link ModernResources}). */
public interface IResourceManager {

    @Srg("func_199001_a")
    Set<String> getResourceNamespaces();

    @Srg("func_199002_a")
    IResource getResource(ResourceLocation location) throws IOException;

    @Srg("func_199004_b")
    List<IResource> getAllResources(ResourceLocation location) throws IOException;

    @Srg("func_199003_a")
    Collection<ResourceLocation> getAllResourceLocations(String path, Predicate<String> filter);
}
