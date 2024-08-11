package com.michaelsebero.portium.modern.host;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** One {@code [[mods]]} entry of a 1.13 mod's {@code mods.toml}. */
final class ModernModInfo implements IModInfo {

    private final ModernModFile file;
    private final UnmodifiableConfig config;
    private final String id;
    private final ArtifactVersion version;
    private final List<ModVersion> dependencies = new ArrayList<>();

    ModernModInfo(ModernModFile file, UnmodifiableConfig config) {
        this.file = file;
        this.config = config;
        this.id = config.get("modId");
        this.version = new DefaultArtifactVersion(config.<String>getOrElse("version", "1").replace("${file.jarVersion}", file.jarVersion));
        List<UnmodifiableConfig> dependencies = file.getConfig().getOrElse(Arrays.asList("dependencies", this.id), Collections.emptyList());
        for (UnmodifiableConfig dependency : dependencies) this.dependencies.add(new ModVersion(this, dependency));
    }

    @Override
    public IModFileInfo getOwningFile() {
        return this.file;
    }

    @Override
    public String getModId() {
        return this.id;
    }

    @Override
    public String getDisplayName() {
        return this.config.getOrElse("displayName", this.id);
    }

    @Override
    public String getDescription() {
        return this.config.getOrElse("description", "");
    }

    @Override
    public ArtifactVersion getVersion() {
        return this.version;
    }

    @Override
    public List<ModVersion> getDependencies() {
        return this.dependencies;
    }

    @Override
    public UnmodifiableConfig getModConfig() {
        return this.config;
    }

    @Override
    public String getNamespace() {
        return this.id;
    }

    @Override
    public Map<String, Object> getModProperties() {
        UnmodifiableConfig properties = this.file.getConfig().get(Arrays.asList("modproperties", this.id));
        return properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties.valueMap());
    }

    @Override
    @Nullable
    public URL getUpdateURL() {
        String url = this.config.get("updateJSONURL");
        try {
            return url == null ? null : new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
