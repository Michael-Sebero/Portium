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

    /** Mods' infos as soon as they're read: a mod's class may ask for its own file while its container is being made. */
    private final java.util.Map<String, net.minecraftforge.forgespi.language.IModInfo> infos = new java.util.HashMap<>();

    public static synchronized void portium$announce(net.minecraftforge.forgespi.language.IModInfo info) {
        INSTANCE.infos.put(info.getModId(), info);
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

    private List<ModInfo> modInfos;

    /**
     * Forge 1.16's launcher mod infos, made from Portium's (mods walk their own files through them: Patchouli finds
     * books in each mod's jar so). Made once the mods are known.
     */
    public synchronized List<ModInfo> getMods() {
        if (this.modInfos != null) return this.modInfos;
        if (this.scanData.isEmpty()) return Collections.emptyList();
        List<ModInfo> made = new java.util.ArrayList<>();
        for (ModContainer container : this.mods) {
            net.minecraftforge.forgespi.language.IModInfo source = container.getModInfo();
            try {
                java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                ModInfo info = (ModInfo) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(ModInfo.class);
                setIn(ModInfo.class, info, "owningFile", getModFileById(source.getModId()));
                setIn(ModInfo.class, info, "modId", source.getModId());
                setIn(ModInfo.class, info, "namespace", source.getNamespace());
                setIn(ModInfo.class, info, "version", source.getVersion());
                setIn(ModInfo.class, info, "displayName", source.getDisplayName());
                setIn(ModInfo.class, info, "description", source.getDescription());
                setIn(ModInfo.class, info, "logoFile", Optional.empty());
                setIn(ModInfo.class, info, "dependencies", Collections.emptyList());
                setIn(ModInfo.class, info, "properties", source.getModProperties());
                made.add(info);
            } catch (ReflectiveOperationException | RuntimeException e) {
                org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").warn("Couldn't describe mod {}", source.getModId(), e);
            }
        }
        this.modInfos = Collections.unmodifiableList(made);
        return this.modInfos;
    }

    public List<ModFileInfo> getModFiles() {
        List<ModFileInfo> files = new java.util.ArrayList<>();
        for (ModContainer container : this.mods) {
            ModFileInfo file = getModFileById(container.getModId());
            if (file != null && !files.contains(file)) files.add(file);
        }
        return files;
    }

    private final java.util.Map<String, ModFileInfo> files = new java.util.HashMap<>();

    /**
     * Forge 1.16's mod file info of a mod (mods read their own version through it). Forge's class is made without
     * its constructor, which needs Forge's own mod file: what it answers comes from Portium's description of the file.
     */
    public synchronized ModFileInfo getModFileById(String modId) {
        if (this.files.containsKey(modId)) return this.files.get(modId);
        ModFileInfo info = null;
        Optional<? extends ModContainer> container = getModContainerById(modId);
        net.minecraftforge.forgespi.language.IModInfo modInfo = container.isPresent() ? container.get().getModInfo() : this.infos.get(modId);
        if (modInfo != null) {
            try {
                net.minecraftforge.forgespi.language.IModFileInfo owner = modInfo.getOwningFile();
                java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                info = (ModFileInfo) ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(ModFileInfo.class);
                set(info, "mods", owner != null ? owner.getMods() : Collections.singletonList(modInfo));
                set(info, "properties", owner != null ? owner.getFileProperties() : Collections.emptyMap());
                set(info, "license", owner != null ? owner.getLicense() : "");
                set(info, "modLoader", owner != null ? owner.getModLoader() : "javafml");
                set(info, "showAsResourcePack", false);
                set(info, "manifest", Optional.empty());
                set(info, "signers", Optional.empty());
                // The file itself, as far as mods read it: its scan of annotations (Quark finds its modules so).
                net.minecraftforge.fml.loading.moddiscovery.ModFile file = (net.minecraftforge.fml.loading.moddiscovery.ModFile)
                        ((sun.misc.Unsafe) unsafeField.get(null)).allocateInstance(net.minecraftforge.fml.loading.moddiscovery.ModFile.class);
                setIn(net.minecraftforge.fml.loading.moddiscovery.ModFile.class, file, "fileModFileScanData", scanOf(modId));
                setIn(net.minecraftforge.fml.loading.moddiscovery.ModFile.class, file, "modFileInfo", info);
                // Its jar (mods open it to walk their own files: Patchouli's books).
                java.io.File jar = com.michaelsebero.portium.mods116.host.ModernHost.jarOf(owner);
                if (jar != null) setIn(net.minecraftforge.fml.loading.moddiscovery.ModFile.class, file, "filePath", jar.toPath());
                set(info, "modFile", file);
            } catch (ReflectiveOperationException | RuntimeException e) {
                org.apache.logging.log4j.LogManager.getLogger("Portium/1.16.5").warn("Couldn't describe the mod file of {}", modId, e);
                info = null;
            }
        }
        // Kept once the scans are in (asked for earlier, from a mod's static initializer, it's made again later).
        if (!this.scanData.isEmpty()) this.files.put(modId, info);
        return info;
    }

    /** The scan of the mod file that declares the mod (its {@code @Mod} annotation names the id). */
    @javax.annotation.Nullable
    private ModFileScanData scanOf(String modId) {
        org.objectweb.asm.Type mod = org.objectweb.asm.Type.getType(net.minecraftforge.fml.common.Mod.class);
        for (ModFileScanData scan : this.scanData) {
            for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
                if (mod.equals(annotation.getAnnotationType()) && modId.equals(annotation.getAnnotationData().get("value"))) return scan;
            }
        }
        return null;
    }

    private static void setIn(Class<?> owner, Object target, String name, Object value) throws ReflectiveOperationException {
        java.lang.reflect.Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void set(Object target, String name, Object value) throws ReflectiveOperationException {
        java.lang.reflect.Field field = ModFileInfo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    public List<ModFileScanData> getAllScanData() {
        return this.scanData;
    }
}
