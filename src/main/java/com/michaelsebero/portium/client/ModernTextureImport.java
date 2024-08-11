package com.michaelsebero.portium.client;

import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.mods116.Mods116;
import com.michaelsebero.portium.util.Progress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Minecraft 1.16.5's textures and models as a resource pack of the player's: copied once from its client jar (which
 * Portium downloads from Mojang, and ships nothing of) into {@code resourcepacks/Portium - Minecraft 1.16.5}, and
 * switched on below any other pack the first time it's made. With it, {@link ModernPackSupport} points 1.12's blocks
 * at 1.16's models, so the game looks as 1.16.5 does, and modern packs (textures only, or with models) work on top.
 * <p>
 * Textures whose layout changed since 1.12 while their path didn't (chests, villagers) are left out: 1.12's models
 * would draw them scrambled. A stamp in the pack says what made it; a pack from an older import is made again.
 */
@SideOnly(Side.CLIENT)
public final class ModernTextureImport {

    private static final Logger LOGGER = LogManager.getLogger("Portium");
    public static final String PACK = "Portium - Minecraft 1.16.5";
    /** Raised whenever what's imported changes, so packs made before are made again. */
    private static final String STAMP = "Imported by Portium from minecraft-1.16.5-client.jar, format 1";
    private static final String STAMP_FILE = "portium-import.txt";

    /** Same path, another layout: 1.15's chests, 1.14's villagers, and beds, which 1.12 draws with its own model. */
    private static final String[] LEFT_OUT = {"assets/minecraft/textures/entity/chest/", "assets/minecraft/textures/entity/villager/",
            "assets/minecraft/textures/entity/zombie_villager/", "assets/minecraft/textures/entity/bed/"};

    /** In construction, before the resource load after it: makes the pack if needed and switches it on once. */
    public static void run(Minecraft minecraft) {
        if (!PortiumConfig.compatibility.importTextures116) return;
        ResourcePackRepository repository = minecraft.getResourcePackRepository();
        File pack = new File(repository.getDirResourcepacks(), PACK);
        boolean existed = pack.isDirectory();
        try {
            if (!isCurrent(pack)) build(pack);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Couldn't import Minecraft 1.16.5's textures into resourcepacks/{}; the game keeps 1.12's look", PACK, e);
            return;
        }
        if (!existed) enable(minecraft, repository);
    }

    private static boolean isCurrent(File pack) {
        File stamp = new File(pack, STAMP_FILE);
        try {
            return stamp.isFile() && new String(Files.readAllBytes(stamp.toPath()), StandardCharsets.UTF_8).trim().equals(STAMP);
        } catch (IOException e) {
            return false;
        }
    }

    private static void build(File pack) throws IOException {
        boolean download = !new File(Mods116.gameDir(), "portium/forge-1.16.5/minecraft-1.16.5-client.jar").isFile();
        if (download) LOGGER.info("Downloading Minecraft 1.16.5's client jar from Mojang to import its textures; this happens once");
        File jar = Mods116.minecraftJar();
        File partial = new File(pack.getPath() + ".part");
        delete(partial.toPath());
        try (ZipFile zip = new ZipFile(jar)) {
            List<ZipEntry> wanted = new ArrayList<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && wanted(entry.getName())) wanted.add(entry);
            }
            LOGGER.info("Importing {} textures and models from Minecraft 1.16.5 into resourcepacks/{}", wanted.size(), PACK);
            try (Progress progress = Progress.start("Portium: importing Minecraft 1.16.5's textures (first launch only)", 20)) {
                int done = 0;
                for (ZipEntry entry : wanted) {
                    Path target = partial.toPath().resolve(entry.getName());
                    Files.createDirectories(target.getParent());
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    done++;
                    progress.reach((double) done / wanted.size(), done + " of " + wanted.size() + " files");
                }
            }
            ZipEntry icon = zip.getEntry("pack.png");
            if (icon != null) {
                try (InputStream in = zip.getInputStream(icon)) {
                    Files.copy(in, partial.toPath().resolve("pack.png"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        String meta = "{\"pack\":{\"pack_format\":3,\"description\":\"Minecraft 1.16.5's textures and models, imported by Portium\"}}";
        Files.write(partial.toPath().resolve("pack.mcmeta"), meta.getBytes(StandardCharsets.UTF_8));
        Files.write(partial.toPath().resolve(STAMP_FILE), STAMP.getBytes(StandardCharsets.UTF_8));
        delete(pack.toPath());
        Files.move(partial.toPath(), pack.toPath());
    }

    private static boolean wanted(String name) {
        if (!name.startsWith("assets/minecraft/textures/") && !name.startsWith("assets/minecraft/models/")) return false;
        for (String out : LEFT_OUT) {
            if (name.startsWith(out)) return false;
        }
        return true;
    }

    /** The pack at the bottom of the enabled packs, so the player's own packs stay above it; saved to options.txt. */
    private static void enable(Minecraft minecraft, ResourcePackRepository repository) {
        repository.updateRepositoryEntriesAll();
        ResourcePackRepository.Entry entry = null;
        for (ResourcePackRepository.Entry candidate : repository.getRepositoryEntriesAll()) {
            if (PACK.equals(candidate.getResourcePackName())) entry = candidate;
        }
        if (entry == null) return;
        List<ResourcePackRepository.Entry> enabled = new ArrayList<>(repository.getRepositoryEntries());
        if (enabled.contains(entry)) return;
        enabled.add(0, entry);
        repository.setRepositories(enabled);
        minecraft.gameSettings.resourcePacks.remove(PACK);
        minecraft.gameSettings.resourcePacks.add(0, PACK);
        // Forge saves no options while the game loads; saved once the main menu opens.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new SaveOptions());
        LOGGER.info("Switched on resource pack {} (below any others); it can be switched off in Resource Packs", PACK);
    }

    /** Saves options.txt (with the pack switched on) when the main menu first opens, then stops listening. */
    public static final class SaveOptions {

        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void opened(net.minecraftforge.client.event.GuiOpenEvent event) {
            if (!(event.getGui() instanceof net.minecraft.client.gui.GuiMainMenu)) return;
            Minecraft.getMinecraft().gameSettings.saveOptions();
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
        }
    }

    private static void delete(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path each : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator) Files.delete(each);
        }
    }

    private ModernTextureImport() {}
}
