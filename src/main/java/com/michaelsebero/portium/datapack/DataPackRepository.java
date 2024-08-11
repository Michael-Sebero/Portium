package com.michaelsebero.portium.datapack;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.modern.ModernMods;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Finds the data packs a world can use: the built-in {@code vanilla} pack (1.13's vanilla tags), 1.13 mods' own
 * ({@code mod/<jar>}), every folder or zip in the game's own {@code datapacks} folder, which every world uses
 * ({@code global/<name>}), and every folder or zip in {@code <world>/datapacks}, named {@code file/<name>} as in 1.13.
 */
public class DataPackRepository {

    public static final String VANILLA = "vanilla";
    private static final String BUILTIN_PATH = "datapacks/vanilla/";

    /** The game's own folder, beside {@code mods} and {@code config}: packs here load in every world. */
    public static final File GLOBAL_FOLDER = new File("datapacks");

    private final File folder;
    private final Map<String, DataPack> packs = new LinkedHashMap<>();

    public DataPackRepository(File folder) {
        this.folder = folder;
    }

    /** Makes the global folder, so players find it before they have a pack to put there. */
    public static void createGlobalFolder() {
        if (PortiumConfig.compatibility.dataPacks && PortiumConfig.compatibility.globalDataPacks && !GLOBAL_FOLDER.isDirectory() && !GLOBAL_FOLDER.mkdirs()) {
            Portium.LOGGER.warn("Couldn't create the global data pack folder {}", GLOBAL_FOLDER.getAbsolutePath());
        }
    }

    public File getFolder() {
        return this.folder;
    }

    public Map<String, DataPack> getPacks() {
        return this.packs;
    }

    public void scan() {
        this.packs.clear();
        DataPack builtin = PortiumConfig.compatibility.vanillaDataPack ? builtin() : null;
        if (builtin != null) this.packs.put(VANILLA, builtin);
        // 1.16's vanilla tags, which 1.16 mods name, when there are any (from the 1.16.5 jar they run on).
        if (!com.michaelsebero.portium.mods116.Mods116.dataPackJars().isEmpty()) {
            try {
                File jar = com.michaelsebero.portium.mods116.Mods116.minecraftJar();
                if (jar.isFile()) this.packs.put(VanillaTags116.ID, new VanillaTags116(jar));
            } catch (java.io.IOException e) {
                Portium.LOGGER.warn("Couldn't read Minecraft 1.16.5's tags for the 1.16 mods", e);
            }
        }
        // 1.13 mods' data, as Forge 1.13 makes every mod a pack; their pack.mcmeta, if any, describes resources.
        for (Map.Entry<String, File> mod : ModernMods.dataPackJars().entrySet()) {
            DataPack pack = new ZipDataPack(mod.getKey(), mod.getValue(), "");
            pack.readMetadata();
            this.packs.put(mod.getKey(), pack);
        }
        // 1.16 mods' data likewise (Forge 1.16's own beside Forge 1.13's).
        for (Map.Entry<String, File> mod : com.michaelsebero.portium.mods116.Mods116.dataPackJars().entrySet()) {
            String id = this.packs.containsKey(mod.getKey()) ? mod.getKey() + "-1.16.5" : mod.getKey();
            DataPack pack = new ZipDataPack(id, mod.getValue(), "");
            pack.readMetadata();
            this.packs.put(id, pack);
        }
        if (PortiumConfig.compatibility.globalDataPacks) {
            createGlobalFolder();
            this.scanFolder(GLOBAL_FOLDER, "global/");
        }
        this.scanFolder(this.folder, "file/");
    }

    private void scanFolder(File folder, String prefix) {
        if (!folder.isDirectory() && !folder.mkdirs()) return;
        File[] files = folder.listFiles();
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            String id = prefix + file.getName();
            DataPack pack;
            if (file.isDirectory()) pack = new FolderDataPack(id, file);
            else if (file.getName().endsWith(".zip")) pack = new ZipDataPack(id, file, "");
            else continue;
            if (pack.readMetadata()) this.packs.put(id, pack);
            else Portium.LOGGER.warn("Ignoring {} in datapacks: it has no valid pack.mcmeta", file.getName());
        }
    }

    /** The pack bundled in Portium's jar (or resources folder, in development). */
    private static DataPack builtin() {
        URL marker = DataPackRepository.class.getResource("/" + BUILTIN_PATH + "pack.mcmeta");
        if (marker == null) {
            Portium.LOGGER.error("Portium's built-in data pack is missing");
            return null;
        }
        try {
            DataPack bundled;
            if ("jar".equals(marker.getProtocol())) {
                String path = marker.getPath();
                File jar = new File(new URL(path.substring(0, path.indexOf("!/"))).toURI());
                bundled = new ZipDataPack(VANILLA, jar, BUILTIN_PATH);
            } else {
                bundled = new FolderDataPack(VANILLA, new File(marker.toURI()).getParentFile());
            }
            DataPack pack = new VanillaDataPack(bundled);
            pack.readMetadata();
            return pack;
        } catch (IOException | URISyntaxException | RuntimeException e) {
            Portium.LOGGER.error("Couldn't open Portium's built-in data pack", e);
            return null;
        }
    }
}
