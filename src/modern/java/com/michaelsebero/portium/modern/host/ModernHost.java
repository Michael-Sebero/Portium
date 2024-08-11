package com.michaelsebero.portium.modern.host;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.michaelsebero.portium.modern.IModernHost;
import com.michaelsebero.portium.modern.ModernCapabilities;
import com.michaelsebero.portium.modern.ModernFailures;
import com.michaelsebero.portium.modern.ModernHooks;
import com.michaelsebero.portium.modern.ModernMods;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.LifecycleEventProvider;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModernLifecycle;
import net.minecraftforge.fml.SidedProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.moddiscovery.ModClassVisitor;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Forge 1.13.2's mod loading, run on 1.12.2: reads 1.13 mod jars as Forge 1.13's loader would, makes Forge 1.13's own
 * mod containers for them, and moves those through Forge 1.13's loading stages as 1.12's go by.
 */
public final class ModernHost implements IModernHost {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");
    private static final Type MOD = Type.getType(Mod.class);
    private static final Type CAPABILITY_INJECT = Type.getType(CapabilityInject.class);

    private final List<ModContainer> containers = new ArrayList<>();
    private final ModernObjectHolders holders = new ModernObjectHolders();
    private final List<ModFileScanData> scans = new ArrayList<>();

    public ModernHost() {
        loadForgeMessages();
        ModernBootstrap.run();
        com.michaelsebero.portium.modern.ModernGuis.init(ModernMods.isClient() ? ModernClient::openContainer : (id, windowId, extra) -> {});
        if (ModernMods.isClient()) {
            SidedProvider.setClient(Minecraft::func_71410_x);
        } else {
            SidedProvider.setServer(ModernMods::dedicatedServer);
        }
    }

    /** Forge 1.13's own messages, which its mod loading errors are worded from. */
    private static void loadForgeMessages() {
        Map<String, String> messages = new HashMap<>();
        try (InputStream in = ModernHost.class.getClassLoader().getResourceAsStream("assets/forge/lang/en_us.json")) {
            if (in != null) {
                messages = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), new TypeToken<Map<String, String>>() {}.getType());
            }
        } catch (IOException e) {
            LOGGER.warn("Couldn't read Forge 1.13's messages", e);
        }
        ForgeI18n.loadLanguageData(messages);
    }

    @Override
    public List<String> construct(List<File> jars) {
        List<String> ids = new ArrayList<>();
        List<net.minecraftforge.fml.loading.moddiscovery.ModFileInfo> files = new ArrayList<>();
        for (File jar : jars) {
            ModFileScanData scan = new ModFileScanData();
            ModernModFile file = read(jar, scan);
            if (file == null) continue;
            scan.addModFileInfo(file);
            file.setScan(scan);
            files.add(file);
            this.scans.add(scan);
            Map<String, String> modClasses = new HashMap<>();
            for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
                if (MOD.equals(annotation.getAnnotationType())) {
                    modClasses.put((String) annotation.getAnnotationData().get("value"), annotation.getClassType().getClassName());
                }
            }
            for (IModInfo info : file.getMods()) {
                String modClass = modClasses.get(info.getModId());
                if (modClass == null) throw new IllegalStateException(jar.getName() + " lists mod " + info.getModId() + " but has no @Mod class for it");
                this.containers.add(new FMLModContainer(info, modClass, getClass().getClassLoader(), scan));
                ids.add(info.getModId());
            }
        }
        ModList.portium$set(this.containers, this.scans);
        net.minecraftforge.fml.loading.LoadingModList.portium$set(files);
        transition(ModLoadingStage.CONSTRUCT, null);
        for (ModFileScanData scan : this.scans) {
            this.holders.scan(scan, getClass().getClassLoader());
            injectCapabilities(scan);
        }
        // Common and client configs load once the mods have registered them, as in Forge 1.13.
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, FMLPaths.CONFIGDIR.get());
        if (ModernMods.isClient()) ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, FMLPaths.CONFIGDIR.get());
        return ids;
    }

    /**
     * Reads a jar's mods.toml and scans its classes for annotations; null if it isn't a 1.13 mod. Classes are scanned
     * as they'll load, so annotations name Forge 1.13's classes by their loaded names.
     */
    @Nullable
    private static ModernModFile read(File jar, ModFileScanData scan) {
        try (JarFile zip = new JarFile(jar)) {
            JarEntry toml = zip.getJarEntry("META-INF/mods.toml");
            if (toml == null) {
                LOGGER.warn("Skipping {}: it has no META-INF/mods.toml, so it isn't a Forge 1.13 mod", jar.getName());
                return null;
            }
            CommentedConfig config;
            try (Reader reader = new InputStreamReader(zip.getInputStream(toml), StandardCharsets.UTF_8)) {
                config = TomlFormat.instance().createParser().parse(reader);
            }
            Enumeration<JarEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ModClassVisitor visitor = new ModClassVisitor();
                try (InputStream in = zip.getInputStream(entry)) {
                    new ClassReader(ModernHooks.translate(readAll(in))).accept(visitor, 0);
                }
                visitor.buildData(scan.getClasses(), scan.getAnnotations());
            }
            return new ModernModFile(jar, config, zip.getManifest());
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't read " + jar.getName(), e);
        }
    }

    @Override
    public List<Map<String, String>> describeMods() {
        List<Map<String, String>> mods = new ArrayList<>();
        for (ModContainer container : this.containers) {
            IModInfo info = container.getModInfo();
            Map<String, String> mod = new HashMap<>();
            mod.put("id", info.getModId());
            mod.put("name", info.getDisplayName());
            mod.put("version", info.getVersion().toString());
            mod.put("description", info.getDescription().trim());
            Object authors = info.getModConfig().getOrElse("authors", info.getOwningFile().getConfig().getOrElse("authors", ""));
            mod.put("authors", String.valueOf(authors));
            Object url = info.getModConfig().getOrElse("displayURL", info.getOwningFile().getConfig().getOrElse("displayURL", ""));
            mod.put("url", String.valueOf(url));
            mods.add(mod);
        }
        return mods;
    }

    /** {@code @CapabilityInject} fields and methods, filled as Forge 1.12 registers their capabilities. */
    private void injectCapabilities(ModFileScanData scan) {
        for (ModFileScanData.AnnotationData annotation : scan.getAnnotations()) {
            if (!CAPABILITY_INJECT.equals(annotation.getAnnotationType())) continue;
            Object value = annotation.getAnnotationData().get("value");
            if (!(value instanceof Type)) continue;
            try {
                Class<?> owner = Class.forName(annotation.getClassType().getClassName(), false, getClass().getClassLoader());
                ModernCapabilities.inject(((Type) value).getClassName(), owner, annotation.getMemberName());
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Couldn't find {} for its @CapabilityInject", annotation.getClassType().getClassName(), e);
            }
        }
    }

    @Override
    public void createRegistries() {
        transition(ModLoadingStage.CREATE_REGISTRIES, RegistryEvent.NewRegistry::new);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register(ResourceLocation name, IForgeRegistry<?> registry) {
        // One event per registry; mods stay in the registry stage until setup.
        transition(ModLoadingStage.LOAD_REGISTRIES, () -> new RegistryEvent.Register(name, registry), false);
        this.holders.apply(registry);
    }

    @Override
    public void setup() {
        transition(ModLoadingStage.LOAD_REGISTRIES, Event::new);
        transition(ModLoadingStage.COMMON_SETUP, null);
        transition(ModLoadingStage.SIDED_SETUP, null);
    }

    @Override
    public void interModComms() {
        transition(ModLoadingStage.ENQUEUE_IMC, null);
        transition(ModLoadingStage.PROCESS_IMC, null);
    }

    @Override
    public void complete() {
        transition(ModLoadingStage.COMPLETE, null);
    }

    @Override
    public void serverAboutToStart(MinecraftServer server) {
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, ModernMods.serverConfigDir(server));
        MinecraftForge.EVENT_BUS.post(new FMLServerAboutToStartEvent(server));
    }

    @Override
    public void serverStarting(MinecraftServer server) {
        ModernCommands.reset();
        MinecraftForge.EVENT_BUS.post(new FMLServerStartingEvent(server));
        ModernCommands.forward();
    }

    @Override
    public void serverStarted(MinecraftServer server) {
        MinecraftForge.EVENT_BUS.post(new FMLServerStartedEvent(server));
    }

    @Override
    public void serverStopping(MinecraftServer server) {
        MinecraftForge.EVENT_BUS.post(new FMLServerStoppingEvent(server));
    }

    @Override
    public void serverStopped(MinecraftServer server) {
        MinecraftForge.EVENT_BUS.post(new FMLServerStoppedEvent(server));
    }

    private void transition(ModLoadingStage stage, @Nullable Supplier<Event> event) {
        transition(stage, event, true);
    }

    /**
     * Moves the mods through a stage; {@code advance} false keeps them in it, for a stage with several events. A mod
     * that fails is reported (see {@link ModernFailures}) and left out of the stages after, so the rest still load.
     */
    private void transition(ModLoadingStage stage, @Nullable Supplier<Event> event, boolean advance) {
        LifecycleEventProvider.LifecycleEvent lifecycle = ModernLifecycle.event(stage, event, advance);
        List<ModContainer> failed = new ArrayList<>();
        for (ModContainer container : new ArrayList<>(this.containers)) {
            List<ModLoadingException> errors = new ArrayList<>();
            try {
                container.transitionState(lifecycle, errors::addAll);
            } catch (RuntimeException | LinkageError e) {
                ModernFailures.add(container.getModInfo().getDisplayName() + " (" + container.getModId() + ") failed during " + stage, e);
                failed.add(container);
                continue;
            }
            for (ModLoadingException error : errors) {
                Throwable cause = error.getCause() != null ? error.getCause() : error;
                ModernFailures.add(container.getModInfo().getDisplayName() + " (" + container.getModId() + ") failed during " + stage, cause);
            }
            if (!errors.isEmpty()) failed.add(container);
        }
        this.containers.removeAll(failed);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        return out.toByteArray();
    }

    @Override
    public boolean conditionsMet(com.google.gson.JsonObject recipe) {
        return net.minecraftforge.common.crafting.CraftingHelper.processConditions(recipe, "conditions");
    }
}
