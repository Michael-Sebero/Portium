package net.minecraftforge.fml.loading.moddiscovery;

import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Portium's replacement for Forge 1.13's: a 1.13 mod's jar, for mods that look at their own file. */
public class ModFile {

    public enum Type { MOD, LIBRARY, LANGPROVIDER }

    private final Path path;
    @Nullable
    private IModFileInfo info;
    @Nullable
    private ModFileScanData scan;
    @Nullable
    private FileSystem files;

    public ModFile(Path path, IModLocator locator) {
        this.path = path;
    }

    public ModFile(Path path, IModFileInfo info, @Nullable ModFileScanData scan) {
        this.path = path;
        this.info = info;
        this.scan = scan;
    }

    public Path getFilePath() {
        return this.path;
    }

    public String getFileName() {
        return this.path.getFileName().toString();
    }

    public Type getType() {
        return Type.MOD;
    }

    /** A path inside the jar. */
    public synchronized Path findResource(String name) {
        try {
            if (this.files == null) this.files = FileSystems.newFileSystem(URI.create("jar:" + this.path.toUri()), Collections.emptyMap());
            return this.files.getPath(name);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't open " + this.path, e);
        } catch (java.nio.file.FileSystemAlreadyExistsException e) {
            this.files = FileSystems.getFileSystem(URI.create("jar:" + this.path.toUri()));
            return this.files.getPath(name);
        }
    }

    @Nullable
    public IModFileInfo getModFileInfo() {
        return this.info;
    }

    public List<IModInfo> getModInfos() {
        return this.info == null ? Collections.emptyList() : this.info.getMods();
    }

    @Nullable
    public ModFileScanData getScanResult() {
        return this.scan;
    }

    public void setFileProperties(Map<String, Object> properties) {}

    @Override
    public String toString() {
        return "Mod File: " + this.path;
    }
}
