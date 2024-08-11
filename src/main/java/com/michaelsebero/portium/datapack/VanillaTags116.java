package com.michaelsebero.portium.datapack;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

/**
 * Minecraft 1.16.5's vanilla tags, read from its client jar (which Portium downloads for 1.16 mods), when 1.16 mods
 * are loaded: those mods name 1.16's tags (ores replace {@code minecraft:base_stone_overworld}), which 1.13's lack.
 * Only the tags: 1.16's recipes, loot tables and advancements name things 1.12 hasn't. Above Portium's vanilla pack,
 * so 1.16's tags add to 1.13's; entries naming blocks Portium hasn't are skipped as with any pack.
 */
public class VanillaTags116 extends ZipDataPack {

    public static final String ID = "vanilla-1.16.5-tags";

    public VanillaTags116(File jar) {
        super(ID, jar, "");
    }

    @Nullable
    @Override
    public InputStream open(String path) throws IOException {
        if (!path.equals("pack.mcmeta") && !(path.startsWith("data/minecraft/tags/"))) return null;
        return super.open(path);
    }

    @Override
    public Collection<ResourceLocation> list(String folder, String extension) {
        if (!folder.startsWith("tags/")) return Collections.emptyList();
        return super.list(folder, extension);
    }
}
