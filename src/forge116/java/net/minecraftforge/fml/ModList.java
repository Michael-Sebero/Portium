package net.minecraftforge.fml;

import com.michaelsebero.portium.mods116.Mods116;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Portium's replacement for Forge 1.13's, which is built from its launcher's mod files: the 1.13 mods Portium loads,
 * and, for {@link #isLoaded}, Forge 1.12's mods as well, so 1.13 mods find their 1.12 companions.
 */
public class ModList {

    private static final ModList INSTANCE = new ModList();

    private final List<ModContainer> mods = new ArrayList<>();
    private final List<ModFileScanData> scanData = new ArrayList<>();

    public static ModList get() {
        return INSTANCE;
    }

    /** For Portium's glue: the mods it has loaded. */
    public static void portium$set(List<ModContainer> mods, List<ModFileScanData> scanData) {
        INSTANCE.mods.clear();
        INSTANCE.mods.addAll(mods);
        INSTANCE.scanData.clear();
        INSTANCE.scanData.addAll(scanData);
    }

    /** Each mod container, by mod id. */
    public void forEachModContainer(java.util.function.BiConsumer<String, ModContainer> action) {
        for (ModContainer container : this.mods) action.accept(container.getModId(), container);
    }

    public boolean isLoaded(String modId) {
        return getModContainerById(modId).isPresent() || Mods116.isLegacyModLoaded(modId);
    }

    public Optional<? extends ModContainer> getModContainerById(String modId) {
        for (ModContainer container : this.mods) {
            if (container.getModId().equals(modId)) return Optional.of(container);
        }
        return Optional.empty();
    }

    public Optional<? extends ModContainer> getModContainerByObject(Object mod) {
        for (ModContainer container : this.mods) {
            if (container.matches(mod)) return Optional.of(container);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getModObjectById(String modId) {
        return getModContainerById(modId).map(container -> (T) container.getMod());
    }

    public int size() {
        return this.mods.size();
    }

    /** Forge 1.13's launcher mod infos, which Portium's mods don't have. */
    public List<ModInfo> getMods() {
        return Collections.emptyList();
    }

    public List<ModFileInfo> getModFiles() {
        return Collections.emptyList();
    }

    public ModFileInfo getModFileById(String modId) {
        return null;
    }

    public List<ModFileScanData> getAllScanData() {
        return this.scanData;
    }
}
