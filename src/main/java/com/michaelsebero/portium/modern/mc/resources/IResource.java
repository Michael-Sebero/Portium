package com.michaelsebero.portium.modern.mc.resources;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.util.ResourceLocation;

import java.io.Closeable;
import java.io.InputStream;

/** 1.13's {@code net.minecraft.resources.IResource}: a resource from a pack (1.12's, wrapped; see {@link ModernResources}). */
public interface IResource extends Closeable {

    @Srg("func_199029_a")
    ResourceLocation getLocation();

    @Srg("func_199027_b")
    InputStream getInputStream();

    @Srg("func_199030_c")
    boolean hasMetadata();

    @Srg("func_199026_d")
    String getPackName();
}
