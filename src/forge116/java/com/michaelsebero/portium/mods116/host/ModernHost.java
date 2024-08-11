package com.michaelsebero.portium.mods116.host;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.michaelsebero.portium.mods116.IModernHost;
import com.michaelsebero.portium.mods116.ModernCapabilities;
import com.michaelsebero.portium.mods116.ModernFailures;
import com.michaelsebero.portium.mods116.ModernHooks;
import com.michaelsebero.portium.mods116.Mods116;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.ForgeI18n;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingException;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingStage;
import net.minecraftforge.fml.ModernLifecycle;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import java.util.function.Function;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLPaths;
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

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.16.5");
    private static final Type MOD = Type.getType(Mod.class);
    private static final Type CAPABILITY_INJECT = Type.getType(CapabilityInject.class);

    private final List<ModContainer> containers = new ArrayList<>();
    private final ModernObjectHolders holders = new ModernObjectHolders();
    private final List<ModFileScanData> scans = new ArrayList<>();

    public ModernHost() {
        loadForgeMessages();
        if (Mods116.isClient()) {
            LogicalSidedProvider.setClient(Minecraft::func_71410_x);
        } else {
            LogicalSidedProvider.setServer(Mods116::dedicatedServer);
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
        // 1.16's vanilla fluids, which its bootstrap makes before mods load.
        com.michaelsebero.portium.mods116.ModernFluids.setVanilla(new net.minecraft.fluid.EmptyFluid(), new net.minecraft.fluid.WaterFluid.Flowing(),
                new net.minecraft.fluid.WaterFluid.Source(), new net.minecraft.fluid.LavaFluid.Flowing(), new net.minecraft.fluid.LavaFluid.Source());
        // Forge's recipe conditions, which ForgeMod registers before mods load.
        registerConditions();
        List<String> ids = new ArrayList<>();
        for (File jar : jars) {
            ModFileScanData scan = new ModFileScanData();
            ModernModFile file = read(jar, scan);
            if (file == null) continue;
            scan.addModFileInfo(file);
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
        each(ModLoadingStage.CONSTRUCT, container -> {
            ModernLifecycle.construct(container);
            return new FMLConstructModEvent(container);
        });
        for (ModFileScanData scan : this.scans) {
            this.holders.scan(scan, getClass().getClassLoader());
            injectCapabilities(scan);
        }
        // Common and client configs load once the mods have registered them, as in Forge 1.16.
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.COMMON, FMLPaths.CONFIGDIR.get());
        if (Mods116.isClient()) ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.CLIENT, FMLPaths.CONFIGDIR.get());
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
                LOGGER.warn("Skipping {}: it has no META-INF/mods.toml, so it isn't a Forge 1.16 mod", jar.getName());
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
                AnnotationScanner scanner = new AnnotationScanner();
                try (InputStream in = zip.getInputStream(entry)) {
                    new ClassReader(ModernHooks.translate(readAll(in))).accept(scanner, ClassReader.SKIP_CODE);
                }
                scanner.addTo(scan);
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
            ModernModInfo mod116 = (ModernModInfo) info;
            ModernModFile file = (ModernModFile) info.getOwningFile();
            Object authors = mod116.getModConfig().getOrElse("authors", file.getConfig().getOrElse("authors", ""));
            mod.put("authors", String.valueOf(authors));
            Object url = mod116.getModConfig().getOrElse("displayURL", file.getConfig().getOrElse("displayURL", ""));
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
        // 1.16's event takes the mod container; it's 1.12's here, which doesn't.
        each(ModLoadingStage.CREATE_REGISTRIES, container -> newInstance(RegistryEvent.NewRegistry.class));
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void register(ResourceLocation name, IForgeRegistry<?> registry) {
        if ("minecraft:fluid".equals(name.toString())) com.michaelsebero.portium.mods116.ModernFluids.registerVanilla(registry);
        each(ModLoadingStage.LOAD_REGISTRIES, container -> new RegistryEvent.Register(name, registry));
        this.holders.apply(registry);
        com.michaelsebero.portium.mods116.shim.RegistryShims116.refresh(name);
    }

    @Override
    public void setup() {
        each(ModLoadingStage.COMMON_SETUP, FMLCommonSetupEvent::new);
        each(ModLoadingStage.SIDED_SETUP, Mods116.isClient() ? FMLClientSetupEvent::new : FMLDedicatedServerSetupEvent::new);
    }

    @Override
    public void interModComms() {
        each(ModLoadingStage.ENQUEUE_IMC, InterModEnqueueEvent::new);
        each(ModLoadingStage.PROCESS_IMC, InterModProcessEvent::new);
    }

    @Override
    public void complete() {
        each(ModLoadingStage.COMPLETE, FMLLoadCompleteEvent::new);
    }

    @Override
    public void serverAboutToStart(MinecraftServer server) {
        ConfigTracker.INSTANCE.loadConfigs(ModConfig.Type.SERVER, Mods116.serverConfigDir(server));
        MinecraftForge.EVENT_BUS.post(new FMLServerAboutToStartEvent(server));
    }

    @Override
    public void serverStarting(MinecraftServer server) {
        ModernCommands.reset();
        // Forge 1.16 registers commands on this event, before the server starts.
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.RegisterCommandsEvent(ModernCommands.dispatcher(),
                server.func_71262_S() ? net.minecraft.command.Commands.EnvironmentType.DEDICATED : net.minecraft.command.Commands.EnvironmentType.INTEGRATED));
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

    @Override
    public void generateFeatures(Object world, String biome, int chunkX, int chunkZ, long seed) {
        WorldGenGlue.generate(world, biome, chunkX, chunkZ, seed);
    }

    @Override
    public void resetFeatures() {
        WorldGenGlue.reset();
    }

    @Override
    public void beginFrame() {
        FlywheelGlue.beginFrame();
    }

    @Override
    public void renderLayer(int layer, double cameraX, double cameraY, double cameraZ) {
        FlywheelGlue.renderLayer(layer, cameraX, cameraY, cameraZ);
    }

    @Override
    public void reloadRenderers() {
        FlywheelGlue.reloadRenderers();
    }

    @Override
    public void dataReloaded() {
        com.michaelsebero.portium.mods116.shim.IngredientShims116.reloaded();
        RecipeManager116.invalidate();
    }

    private static void registerConditions() {
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.AndCondition.Serializer.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.FalseCondition.Serializer.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.ItemExistsCondition.Serializer.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.ModLoadedCondition.Serializer.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.NotCondition.Serializer.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.OrCondition.Serializer.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.TrueCondition.Serializer.INSTANCE);
        net.minecraftforge.common.crafting.CraftingHelper.register(net.minecraftforge.common.crafting.conditions.TagEmptyCondition.Serializer.INSTANCE);
    }

    /** Straight to each mod's bus: Forge 1.16's own posting asks for its mod bus event marker, which 1.12's events lack. */
    @Override
    public void postToMods(Object event) {
        net.minecraftforge.fml.ModList.get().forEachModContainer((id, container) -> {
            if (container instanceof net.minecraftforge.fml.javafmlmod.FMLModContainer) {
                ((net.minecraftforge.fml.javafmlmod.FMLModContainer) container).getEventBus().post((net.minecraftforge.eventbus.api.Event) event);
            }
        });
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Can't make " + type.getName(), e);
        }
    }

    /**
     * Moves each mod through a stage, posting it the event the stage makes for it. A mod that fails is reported (see
     * {@link ModernFailures}) and left out of the stages after, so the rest still load.
     */
    private void each(ModLoadingStage stage, Function<ModContainer, ? extends Event> event) {
        List<ModContainer> failed = new ArrayList<>();
        for (ModContainer container : new ArrayList<>(this.containers)) {
            try {
                ModernLifecycle.post(container, event.apply(container), stage);
            } catch (ModLoadingException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                ModernFailures.add(container.getModInfo().getDisplayName() + " (" + container.getModId() + ") failed during " + stage, cause);
                failed.add(container);
            } catch (RuntimeException | LinkageError e) {
                ModernFailures.add(container.getModInfo().getDisplayName() + " (" + container.getModId() + ") failed during " + stage, e);
                failed.add(container);
            }
        }
        this.containers.removeAll(failed);
        try {
            ModernLifecycle.finish(stage);
        } catch (RuntimeException | LinkageError e) {
            ModernFailures.add("Work Forge 1.16 mods queued during " + stage + " failed", e);
        }
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
