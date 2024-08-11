package net.minecraftforge.fml.loading;

import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Portium's replacement for Forge 1.16's, which is built by its launcher's mod discovery: Portium reads the 1.16 mods
 * itself (see {@code ModernHost}), so there are no launcher mod files to list.
 */
public class LoadingModList {

    private static final LoadingModList INSTANCE = new LoadingModList();

    public static LoadingModList get() {
        return INSTANCE;
    }

    public List<ModFileInfo> getModFiles() {
        return Collections.emptyList();
    }

    public List<ModInfo> getMods() {
        return Collections.emptyList();
    }

    public ModFileInfo getModFileById(String modId) {
        return null;
    }

    public Path findResource(String className) {
        return null;
    }

    public List<EarlyLoadingException> getErrors() {
        return Collections.emptyList();
    }
}
