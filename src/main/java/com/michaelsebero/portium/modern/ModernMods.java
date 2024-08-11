package com.michaelsebero.portium.modern;

import com.michaelsebero.portium.Portium;
import com.michaelsebero.portium.PortiumConfig;
import com.michaelsebero.portium.modern.flat.Flattening;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs mods made for Minecraft Forge 1.13.2, installed in {@code mods/1.13.2}, on 1.12.2. They run on Forge 1.13.2's
 * own mod loader, event bus and APIs, loaded beside Forge 1.12.2 by {@link ModernClassLoader}, with Minecraft 1.13.2's
 * own classes for those 1.12 lacks; Portium's glue ({@code modern.host}) drives Forge 1.13's loading stages from
 * 1.12's and passes 1.12's events on.
 */
public final class ModernMods {

    public static final String FOLDER = "1.13.2";
    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");
    private static final String GLUE = "META-INF/portium/portium-modern.jar";

    @Nullable
    private static IModernHost host;
    private static final Map<String, File> dataPacks = new LinkedHashMap<>();
    private static final List<Map<String, String>> descriptions = new ArrayList<>();
    private static boolean registriesCreated;

    /** Finds and constructs the 1.13 mods; Portium's construction stage. */
    public static void construct() {
        if (!PortiumConfig.compatibility.modernMods) {
            LOGGER.info("Loading 1.13.2 mods is switched off in Portium's config; ignoring mods/{}", FOLDER);
            return;
        }
        File gameDir = gameDir();
        File dir = new File(new File(gameDir, "mods"), FOLDER);
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        File[] found = dir.listFiles((parent, name) -> name.endsWith(".jar"));
        if (found == null || found.length == 0) return;
        List<File> jars = new ArrayList<>(Arrays.asList(found));
        Collections.sort(jars);
        List<File> runtime = new ArrayList<>();
        File minecraftJar;
        try {
            runtime.addAll(glue(gameDir));
            runtime.addAll(ModernRuntime.resolve(gameDir));
            File[] minecraft = ModernRuntime.resolveMinecraft(gameDir);
            minecraftJar = minecraft[0];
            ModernNatives natives = new ModernNatives(minecraft[0], minecraft[1]);
            ModernClassLoader loader = new ModernClassLoader(ModernMods.class.getClassLoader(), runtime, jars, natives, new ModernMappings(srgMcp()));
            ModernHooks.loader = loader;
            ModernHooks.translator = loader.translator();
            Flattening.setNames(loader.translator().registeredNames("net/minecraft/init/Blocks"), loader.translator().registeredNames("net/minecraft/init/Items"));
            if (Boolean.getBoolean("portium.modern.audit")) ModernAudit.run(loader, jars);
            host = (IModernHost) loader.loadClass("com.michaelsebero.portium.modern.host.ModernHost").newInstance();
        } catch (IOException | ReflectiveOperationException e) {
            throw new LoaderException("Couldn't set up Forge " + ModernRuntime.FORGE + " and Minecraft 1.13.2 for the mods in mods/" + FOLDER
                    + ". Their files are downloaded the first time; check the connection, or remove those mods.", e);
        }
        for (File file : runtime) {
            if (file.getName().startsWith("forge-") && file.getName().endsWith("-universal.jar")) dataPacks.put("mod/forge", file);
        }
        for (File jar : jars) dataPacks.put("mod/" + jar.getName().replaceFirst("\\.jar$", ""), jar);
        Portium.proxy.addModernResources(jars, minecraftJar);
        List<String> mods = run(() -> host.construct(jars));
        LOGGER.info("Loaded {} Forge {} mod(s): {}", mods.size(), ModernRuntime.FORGE, String.join(", ", mods));
        descriptions.addAll(host.describeMods());
        MinecraftForge.EVENT_BUS.register(new ModernEvents(host));
    }

    public static void preInit() {
        createRegistries();
    }

    /** Forge 1.12 creates its registries just before pre-initialisation; 1.13 mods may add theirs then. */
    public static void createRegistries() {
        if (host == null || registriesCreated) return;
        registriesCreated = true;
        run(host::createRegistries);
    }

    public static void init() {
        if (host != null) run(host::setup);
    }

    public static void postInit() {
        if (host != null) run(host::interModComms);
    }

    public static void loadComplete() {
        if (host != null) run(host::complete);
    }

    /** A 1.12 server is about to start: 1.13 mods' server configs load from its world. */
    public static void serverAboutToStart(MinecraftServer server) {
        if (host != null) run(() -> host.serverAboutToStart(server));
    }

    /**
     * Whether a data pack recipe's Forge 1.13 {@code conditions} hold (as Forge 1.13 checks before loading a recipe);
     * true without 1.13 mods, whose Forge evaluates them.
     */
    public static boolean conditionsMet(com.google.gson.JsonObject recipe) {
        if (host == null || !recipe.has("conditions")) return true;
        try {
            return host.conditionsMet(recipe);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("Couldn't check a recipe's conditions", e);
            return true;
        }
    }

    public static void serverStarting(MinecraftServer server) {
        if (host != null) run(() -> host.serverStarting(server));
    }

    public static void serverStarted(MinecraftServer server) {
        if (host != null) run(() -> host.serverStarted(server));
    }

    public static void serverStopping(MinecraftServer server) {
        if (host != null) run(() -> host.serverStopping(server));
    }

    public static void serverStopped(MinecraftServer server) {
        if (host != null) run(() -> host.serverStopped(server));
    }

    /** For the glue: whether Forge 1.12 has loaded a mod. */
    public static boolean isLegacyModLoaded(String modId) {
        return Loader.isModLoaded(modId);
    }

    /** For the glue: the running server, if any. */
    @Nullable
    public static MinecraftServer currentServer() {
        return FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    /** The game directory; a dedicated server may not name one, and runs in it. */
    public static File gameDir() {
        return Launch.minecraftHome != null ? Launch.minecraftHome : new File(".");
    }

    /** For the glue: where a server keeps its worlds' 1.13 server configs, {@code serverconfig} in its world folder. */
    public static Path serverConfigDir(MinecraftServer server) {
        File folder = server.getActiveAnvilConverter().getFile(server.getFolderName(), "serverconfig");
        if (!folder.isDirectory() && !folder.mkdirs()) LOGGER.warn("Couldn't make {}", folder);
        return folder.toPath();
    }

    /**
     * The jars whose {@code data} folders are built-in data packs, as Forge 1.13 makes every mod a data pack: the
     * 1.13 mods, and Forge 1.13 itself for its {@code forge} tags. Keyed by pack id.
     */
    public static Map<String, File> dataPackJars() {
        return Collections.unmodifiableMap(dataPacks);
    }

    /** The 1.13 mods, for 1.12's mod list (see {@link IModernHost#describeMods}). */
    public static List<Map<String, String>> descriptions() {
        return Collections.unmodifiableList(descriptions);
    }

    /** For the glue: whether this is the physical client. */
    public static boolean isClient() {
        return FMLCommonHandler.instance().getSide().isClient();
    }

    /** For the glue: whether Minecraft runs with MCP names, as in a development environment. */
    public static boolean isDevelopment() {
        Object deobfuscated = Launch.blackboard.get("fml.deobfuscatedEnvironment");
        return deobfuscated instanceof Boolean && (Boolean) deobfuscated;
    }

    /** For the glue: the dedicated server, if this is one. */
    @Nullable
    public static DedicatedServer dedicatedServer() {
        return isClient() ? null : (DedicatedServer) FMLCommonHandler.instance().getMinecraftServerInstance();
    }

    private static void run(Runnable stage) {
        run(() -> {
            stage.run();
            return null;
        });
    }

    private static <T> T run(java.util.function.Supplier<T> stage) {
        try {
            return stage.get();
        } catch (RuntimeException | LinkageError e) {
            throw new LoaderException("A Forge " + ModernRuntime.FORGE + " mod failed to load: " + e.getMessage(), e);
        }
    }

    /** Portium's glue: classes directories in a development environment, else a jar inside Portium's. */
    private static List<File> glue(File gameDir) throws IOException {
        String classes = System.getProperty("portium.modern.classes");
        List<File> files = new ArrayList<>();
        if (classes != null) {
            for (String path : classes.split(File.pathSeparator)) files.add(new File(path));
            return files;
        }
        File jar = new File(gameDir, "portium/forge-1.13.2/portium-modern.jar");
        Files.createDirectories(jar.getParentFile().toPath());
        try (InputStream in = ModernMods.class.getClassLoader().getResourceAsStream(GLUE)) {
            if (in == null) throw new IOException(GLUE + " is missing from Portium's jar");
            Files.copy(in, jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        files.add(jar);
        return files;
    }

    @Nullable
    private static File srgMcp() throws IOException {
        if (!isDevelopment()) return null;
        String path = System.getProperty("portium.modern.srgMcp");
        if (path == null || !new File(path).isFile()) throw new IOException("Set portium.modern.srgMcp to Forge's srg-mcp.srg to run 1.13 mods in a development environment");
        return new File(path);
    }

    private ModernMods() {}
}
