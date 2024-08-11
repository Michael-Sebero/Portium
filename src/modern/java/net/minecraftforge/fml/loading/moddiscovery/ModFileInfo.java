package net.minecraftforge.fml.loading.moddiscovery;

import net.minecraftforge.forgespi.language.IModFileInfo;

import java.util.Optional;
import java.util.jar.Manifest;

/** Portium's replacement for Forge 1.13's: a 1.13 mod jar's mods.toml and the jar (see {@code ModernModFile}). */
public abstract class ModFileInfo implements IModFileInfo {

    public abstract ModFile getFile();

    public abstract Optional<Manifest> getManifest();
}
