package net.minecraftforge.fml.loading;

import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Portium's replacement for Forge 1.13's: the 1.13 mods Portium loaded, and their files. */
public class LoadingModList {

    private static LoadingModList instance = new LoadingModList(Collections.emptyList());
    private final List<ModFileInfo> files;

    private LoadingModList(List<ModFileInfo> files) {
        this.files = files;
    }

    public static LoadingModList get() {
        return instance;
    }

    /** For Portium's glue, once it has read the mods. */
    public static void portium$set(List<ModFileInfo> files) {
        instance = new LoadingModList(Collections.unmodifiableList(new ArrayList<>(files)));
    }

    public List<ModFileInfo> getModFiles() {
        return this.files;
    }

    @Nullable
    public ModFileInfo getModFileById(String id) {
        for (ModFileInfo file : this.files) {
            for (IModInfo mod : file.getMods()) {
                if (mod.getModId().equals(id)) return file;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List getMods() {
        List mods = new ArrayList<>();
        for (ModFileInfo file : this.files) mods.addAll(file.getMods());
        return mods;
    }

    @Nullable
    public Path findResource(String name) {
        for (ModFileInfo file : this.files) {
            Path path = file.getFile().findResource(name);
            if (Files.exists(path)) return path;
        }
        return null;
    }

    @Nullable
    public URL findURLForResource(String name) {
        Path path = findResource(name);
        try {
            return path == null ? null : path.toUri().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public List<EarlyLoadingException> getErrors() {
        return Collections.emptyList();
    }

    public List<ModFile> getBrokenFiles() {
        return Collections.emptyList();
    }

    public void setBrokenFiles(List<ModFile> files) {}

    public void addCoreMods() {}

    public void addAccessTransformers() {}
}
