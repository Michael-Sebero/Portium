package com.michaelsebero.portium.mods116.host;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/** A 1.16 mod jar's {@code META-INF/mods.toml}, as Forge 1.16 presents it to mods. */
final class ModernModFile implements IModFileInfo, IConfigurable {

    final File jar;
    private final UnmodifiableConfig config;
    @Nullable
    private final Manifest manifest;
    private final List<IModInfo> mods = new ArrayList<>();
    /** The jar's version, which {@code ${file.jarVersion}} in a mod's version stands for. */
    final String jarVersion;

    ModernModFile(File jar, UnmodifiableConfig config, @Nullable Manifest manifest) {
        this.jar = jar;
        this.config = config;
        this.manifest = manifest;
        String version = manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        this.jarVersion = version == null ? "0.0NONE" : version;
        List<UnmodifiableConfig> mods = config.getOrElse("mods", Collections.emptyList());
        for (UnmodifiableConfig mod : mods) this.mods.add(new ModernModInfo(this, mod));
    }

    UnmodifiableConfig getConfig() {
        return this.config;
    }

    Optional<Manifest> getManifest() {
        return Optional.ofNullable(this.manifest);
    }

    @Override
    public List<IModInfo> getMods() {
        return this.mods;
    }

    @Override
    public String getModLoader() {
        return this.config.getOrElse("modLoader", "javafml");
    }

    @Override
    public VersionRange getModLoaderVersion() {
        try {
            return VersionRange.createFromVersionSpec(this.config.getOrElse("loaderVersion", "[0,)"));
        } catch (InvalidVersionSpecificationException e) {
            return IModInfo.UNBOUNDED;
        }
    }

    @Override
    public boolean showAsResourcePack() {
        return this.config.getOrElse("showAsResourcePack", false);
    }

    @Override
    public Map<String, Object> getFileProperties() {
        return Collections.emptyMap();
    }

    @Override
    public String getLicense() {
        return this.config.getOrElse("license", "");
    }

    @Override
    public <T> Optional<T> getConfigElement(String... key) {
        return ModernModInfo.element(this.config, key);
    }

    @Override
    public List<? extends IConfigurable> getConfigList(String... key) {
        return ModernModInfo.list(this.config, key);
    }
}
