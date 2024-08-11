package com.michaelsebero.portium.modern.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.michaelsebero.portium.mixin.client.I18nAccessor;
import com.michaelsebero.portium.mixin.client.LocaleAccessor;
import com.michaelsebero.portium.mixin.client.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.LanguageMap;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 1.13 mods' resources on the client, as Forge 1.13 has them: each mod jar a built-in resource pack, its language
 * files (1.13's JSON) read into 1.12's language tables, and an inventory model for each of its items named after the
 * item, which 1.12 leaves to mods to ask for.
 */
public final class ModernClientResources {

    private static final Logger LOGGER = LogManager.getLogger("Portium/1.13.2");
    private static final Type STRINGS = new TypeToken<Map<String, String>>() {}.getType();

    private static final Set<String> namespaces = new LinkedHashSet<>();

    /** Before the first resource load: the jars become packs, and their text and item models follow every reload. */
    public static void install(List<File> jars, @javax.annotation.Nullable File minecraftJar) {
        Minecraft minecraft = Minecraft.getMinecraft();
        List<IResourcePack> packs = ((MinecraftAccessor) minecraft).portium$getDefaultResourcePacks();
        // Below everything else, the game's own 1.12 resources included.
        if (minecraftJar != null) packs.add(0, new ModernVanillaPack(minecraftJar));
        for (File jar : jars) {
            packs.add(new ModernResourcePack(jar));
            namespaces.addAll(assetNamespaces(jar));
        }
        MinecraftForge.EVENT_BUS.register(new ModernClientResources());
    }

    /**
     * In pre-initialisation, after the game's language manager, which replaces both language tables on every reload,
     * so the text read here lands on top.
     */
    public static void registerLanguageReload() {
        if (namespaces.isEmpty()) return;
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(ModernClientResources::loadLanguage);
    }

    private static Set<String> assetNamespaces(File jar) {
        Set<String> found = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(jar)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith("assets/")) continue;
                int slash = name.indexOf('/', "assets/".length());
                if (slash > 0) found.add(name.substring("assets/".length(), slash));
            }
        } catch (IOException e) {
            LOGGER.warn("Couldn't read {}", jar.getName(), e);
        }
        return found;
    }

    /** English, then the current language over it, from every pack, into both of 1.12's tables. */
    private static void loadLanguage(IResourceManager manager) {
        String code = Minecraft.getMinecraft().gameSettings.language.toLowerCase(Locale.ROOT);
        Map<String, String> text = new LinkedHashMap<>();
        for (String namespace : namespaces) {
            read(manager, new ResourceLocation(namespace, "lang/en_us.json"), text);
            if (!code.equals("en_us")) read(manager, new ResourceLocation(namespace, "lang/" + code + ".json"), text);
        }
        if (text.isEmpty()) return;
        StringBuilder lang = new StringBuilder();
        for (Map.Entry<String, String> entry : text.entrySet()) lang.append(entry.getKey()).append('=').append(entry.getValue().replace('\n', ' ')).append('\n');
        LanguageMap.inject(new ByteArrayInputStream(lang.toString().getBytes(StandardCharsets.UTF_8)));
        ((LocaleAccessor) I18nAccessor.portium$getLocale()).portium$getProperties().putAll(text);
    }

    private static void read(IResourceManager manager, ResourceLocation location, Map<String, String> text) {
        try {
            for (IResource resource : manager.getAllResources(location)) {
                try (InputStream in = resource.getInputStream()) {
                    // As Minecraft reads them: a key given twice takes its last value (mods repeat "_" for comments).
                    com.google.gson.JsonElement json = new com.google.gson.JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
                    if (json == null || !json.isJsonObject()) continue;
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : json.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) text.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        } catch (IOException e) {
            // No such language file.
        }
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation name = item.getRegistryName();
            if (name != null && namespaces.contains(name.getNamespace())) {
                ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(name, "inventory"));
            }
        }
    }
}
