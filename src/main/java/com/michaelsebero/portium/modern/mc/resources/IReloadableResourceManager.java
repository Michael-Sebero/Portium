package com.michaelsebero.portium.modern.mc.resources;

import com.michaelsebero.portium.modern.Srg;

/** 1.13's {@code net.minecraft.resources.IReloadableResourceManager}: reload listeners go to 1.12's manager. */
public interface IReloadableResourceManager extends IResourceManager {

    @Srg("func_199006_a")
    void addReloadListener(IResourceManagerReloadListener listener);
}
