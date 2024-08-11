package com.michaelsebero.portium.mods116.host;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.forgespi.language.IConfigurable;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** One {@code [[mods]]} entry of a 1.16 mod's {@code mods.toml}. */
final class ModernModInfo implements IModInfo, IConfigurable {

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
        for (UnmodifiableConfig dependency : dependencies) this.dependencies.add(new Dependency(this, dependency));
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

    /** The {@code [[mods]]} entry itself, for Portium's mod list. */
    UnmodifiableConfig getModConfig() {
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

    @Override
    public <T> Optional<T> getConfigElement(String... key) {
        return element(this.config, key);
    }

    @Override
    public List<? extends IConfigurable> getConfigList(String... key) {
        return list(this.config, key);
    }

    static <T> Optional<T> element(UnmodifiableConfig config, String... key) {
        return Optional.ofNullable(config.<T>get(Arrays.asList(key)));
    }

    static List<? extends IConfigurable> list(UnmodifiableConfig config, String... key) {
        List<UnmodifiableConfig> entries = config.getOrElse(Arrays.asList(key), Collections.emptyList());
        List<IConfigurable> out = new ArrayList<>();
        for (UnmodifiableConfig entry : entries) {
            out.add(new IConfigurable() {
                @Override
                public <T> Optional<T> getConfigElement(String... inner) {
                    return element(entry, inner);
                }

                @Override
                public List<? extends IConfigurable> getConfigList(String... inner) {
                    return list(entry, inner);
                }
            });
        }
        return out;
    }

    /** One {@code [[dependencies.<mod>]]} entry. */
    private static final class Dependency implements ModVersion {

        private IModInfo owner;
        private final String modId;
        private final VersionRange range;
        private final boolean mandatory;
        private final Ordering ordering;
        private final DependencySide side;

        Dependency(IModInfo owner, UnmodifiableConfig config) {
            this.owner = owner;
            this.modId = config.get("modId");
            VersionRange range;
            try {
                range = VersionRange.createFromVersionSpec(config.getOrElse("versionRange", ""));
            } catch (InvalidVersionSpecificationException e) {
                range = UNBOUNDED;
            }
            this.range = range;
            this.mandatory = config.getOrElse("mandatory", false);
            this.ordering = Ordering.valueOf(config.<String>getOrElse("ordering", "NONE").toUpperCase(Locale.ROOT));
            this.side = DependencySide.valueOf(config.<String>getOrElse("side", "BOTH").toUpperCase(Locale.ROOT));
        }

        @Override
        public String getModId() {
            return this.modId;
        }

        @Override
        public VersionRange getVersionRange() {
            return this.range;
        }

        @Override
        public boolean isMandatory() {
            return this.mandatory;
        }

        @Override
        public Ordering getOrdering() {
            return this.ordering;
        }

        @Override
        public DependencySide getSide() {
            return this.side;
        }

        @Override
        public void setOwner(IModInfo owner) {
            this.owner = owner;
        }

        @Override
        public IModInfo getOwner() {
            return this.owner;
        }
    }
}
