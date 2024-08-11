package net.minecraftforge.fml.loading;

import com.michaelsebero.portium.mods116.Mods116;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Portium's replacement for Forge 1.13's, which asks its launcher: the folders, under Forge 1.12's game directory. */
public enum FMLPaths {
    GAMEDIR(),
    MODSDIR("mods"),
    CONFIGDIR("config"),
    FMLCONFIG(false, CONFIGDIR, "fml.toml");

    private final Path relativePath;
    private final boolean isDirectory;

    FMLPaths(String... path) {
        this(true, null, path);
    }

    FMLPaths(boolean isDirectory, FMLPaths parent, String... path) {
        Path relative = Paths.get("", path);
        this.relativePath = parent == null ? relative : parent.relativePath.resolve(relative);
        this.isDirectory = isDirectory;
    }

    public Path relative() {
        return this.relativePath;
    }

    public Path get() {
        Path path = Mods116.gameDir().toPath().resolve(this.relativePath).toAbsolutePath().normalize();
        if (this.isDirectory) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Couldn't make " + path, e);
            }
        }
        return path;
    }
}
