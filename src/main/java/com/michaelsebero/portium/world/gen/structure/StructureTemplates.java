package com.michaelsebero.portium.world.gen.structure;

import com.cleanroommc.assetmover.AssetMoverAPI;
import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.Tags;
import com.michaelsebero.portium.datapack.DataPackManager;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads structure templates by id ({@code portium:shipwreck/with_mast}): from an enabled data pack's
 * {@code minecraft} structures if one has it (as data packs override 1.13's), then the mod's own resources, then
 * the copies AssetMover extracted from the official 1.13.2 jar into {@code assetmover/}.
 */
public final class StructureTemplates {

    /** The version whose templates are used: the last 1.13 release, for 1.13's structures exactly. */
    private static final String TEMPLATE_VERSION = "1.13.2";
    private static final File ASSET_MOVER_DIR = new File("assetmover");
    private static final Map<ResourceLocation, Optional<StructureTemplate>> CACHE = new ConcurrentHashMap<>();

    /**
     * Asks AssetMover to extract 1.13.2's shipwreck and ocean ruin templates. Must run during mod construction. Does
     * nothing without AssetMover, in which case those structures don't generate.
     */
    public static void fetch() {
        if (!Loader.isModLoaded("assetmover")) {
            Portium.LOGGER.warn("AssetMover is not installed, so shipwrecks and ocean ruins won't generate.");
            return;
        }
        Map<String, String> files = new LinkedHashMap<>();
        for (String name : ShipwreckStructure.TEMPLATES) template(files, "shipwreck/" + name);
        for (String name : OceanRuinStructure.TEMPLATES) template(files, "underwater_ruin/" + name);
        AssetMoverAPI.fromMinecraft(TEMPLATE_VERSION, files);
    }

    private static void template(Map<String, String> files, String path) {
        files.put("data/minecraft/structures/" + path + ".nbt", "assets/" + Tags.MOD_ID + "/structures/" + path + ".nbt");
    }

    @Nullable
    public static StructureTemplate get(ResourceLocation id) {
        return CACHE.computeIfAbsent(id, StructureTemplates::load).orElse(null);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static Optional<StructureTemplate> load(ResourceLocation id) {
        String path = "assets/" + id.getNamespace() + "/structures/" + id.getPath() + ".nbt";
        try (InputStream stream = open(id, path)) {
            if (stream == null) {
                Portium.LOGGER.warn("Structure template {} is missing (expected at {}); is AssetMover installed?", id, path);
                return Optional.empty();
            }
            return Optional.of(StructureTemplate.read(CompressedStreamTools.readCompressed(stream)));
        } catch (IOException | RuntimeException e) {
            Portium.LOGGER.error("Couldn't read structure template {}", id, e);
            return Optional.empty();
        }
    }

    @Nullable
    private static InputStream open(ResourceLocation id, String path) throws IOException {
        DataPackManager packs = DataPackManager.get();
        if (packs != null) {
            InputStream pack = packs.open(new ResourceLocation("minecraft", id.getPath()), "structures", ".nbt");
            if (pack != null) return pack;
        }
        InputStream bundled = StructureTemplates.class.getResourceAsStream("/" + path);
        if (bundled != null) return bundled;
        File extracted = new File(ASSET_MOVER_DIR, path);
        return extracted.isFile() ? new FileInputStream(extracted) : null;
    }

    private StructureTemplates() {}
}
