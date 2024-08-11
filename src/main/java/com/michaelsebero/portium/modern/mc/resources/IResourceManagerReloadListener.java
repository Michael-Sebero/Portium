package com.michaelsebero.portium.modern.mc.resources;

import com.michaelsebero.portium.modern.Srg;

/** 1.13's {@code net.minecraft.resources.IResourceManagerReloadListener}. */
public interface IResourceManagerReloadListener {

    @Srg("func_195410_a")
    void onResourceManagerReload(IResourceManager manager);
}
