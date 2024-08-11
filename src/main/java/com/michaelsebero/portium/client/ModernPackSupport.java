package com.michaelsebero.portium.client;

import com.michaelsebero.portium.mixin.client.MinecraftAccessor;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets resource packs made for Minecraft 1.13 to 1.19 work: 1.12's block states pointed at the models those versions
 * name differently ({@code acacia_log_side} became a rotated {@code acacia_log}), from the resourcefix 1.1 pack built
 * into Portium (see {@code portium/resourcefix/CREDITS.txt}).
 * <p>
 * Below the player's packs and above the game's own. It answers only while an enabled pack has 1.13's block models
 * (Portium's imported 1.16.5 pack, or a modern pack with models); without them 1.12's block states are left as they
 * are, so 1.12's look is never broken. A block state whose models no pack has (the pack follows 1.19, which renamed
 * door models after 1.16) is left out too, and that block keeps 1.12's.
 */
@SideOnly(Side.CLIENT)
public final class ModernPackSupport implements IResourcePack {

    private static final Logger LOGGER = LogManager.getLogger("Portium");
    private static final String ROOT = "portium/resourcefix/";
    /** A model 1.13 on have and 1.12 doesn't (1.12's grass block is {@code grass_normal}). */
    private static final ResourceLocation MODERN_MODEL = new ResourceLocation("minecraft", "models/block/grass_block.json");

    private final Set<String> files = new HashSet<>();
    /** Per block state served, whether every model it names is found; worked out again when the enabled packs change. */
    private final java.util.Map<String, Boolean> resolvable = new java.util.HashMap<>();
    @Nullable
    private List<ResourcePackRepository.Entry> checked;
    private boolean active;

    private ModernPackSupport() {
        try (InputStream in = ModernPackSupport.class.getClassLoader().getResourceAsStream(ROOT + "files.txt")) {
            if (in == null) throw new FileNotFoundException(ROOT + "files.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                // assets/minecraft/blockstates/acacia_log.json -> blockstates/acacia_log.json
                if (line.startsWith("assets/minecraft/")) this.files.add(line.substring("assets/minecraft/".length()).trim());
            }
        } catch (IOException e) {
            LOGGER.error("Couldn't read Portium's modern resource pack support; modern packs may look wrong", e);
        }
    }

    /** Before the first resource load: above the game's and mods' resources, below the player's packs. */
    public static void install(Minecraft minecraft) {
        ((MinecraftAccessor) minecraft).portium$getDefaultResourcePacks().add(new ModernPackSupport());
    }

    /** Whether an enabled pack has 1.13's models, checked again whenever the enabled packs change. */
    private boolean active() {
        List<ResourcePackRepository.Entry> enabled = Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries();
        if (!enabled.equals(this.checked)) {
            this.checked = new ArrayList<>(enabled);
            boolean modern = false;
            for (ResourcePackRepository.Entry entry : enabled) {
                if (entry.getResourcePack().resourceExists(MODERN_MODEL)) modern = true;
            }
            this.resolvable.clear();
            if (modern != this.active) LOGGER.info(modern ? "An enabled resource pack has 1.13+ models: pointing 1.12's blocks at them"
                    : "No enabled resource pack has 1.13+ models: 1.12's blocks keep their own");
            this.active = modern;
        }
        return this.active;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!resourceExists(location)) throw new FileNotFoundException(location.toString());
        InputStream in = ModernPackSupport.class.getClassLoader().getResourceAsStream(ROOT + "assets/minecraft/" + location.getPath());
        if (in == null) throw new FileNotFoundException(location.toString());
        return in;
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!"minecraft".equals(location.getNamespace()) || !this.files.contains(location.getPath()) || !active()) return false;
        if (!location.getPath().startsWith("blockstates/")) return true;
        return this.resolvable.computeIfAbsent(location.getPath(), this::modelsFound);
    }

    /** Whether every model a block state of this pack names is in this pack or another the game has. */
    private boolean modelsFound(String blockstate) {
        Set<String> models = new HashSet<>();
        try (InputStream in = ModernPackSupport.class.getClassLoader().getResourceAsStream(ROOT + "assets/minecraft/" + blockstate)) {
            if (in == null) return false;
            collectModels(new com.google.gson.JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)), models);
        } catch (IOException | RuntimeException e) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        List<IResourcePack> packs = new ArrayList<>(((MinecraftAccessor) minecraft).portium$getDefaultResourcePacks());
        for (ResourcePackRepository.Entry entry : minecraft.getResourcePackRepository().getRepositoryEntries()) packs.add(entry.getResourcePack());
        packs.remove(this);
        for (String model : models) {
            String path = "models/block/" + model + ".json";
            if (this.files.contains(path)) continue;
            ResourceLocation location = new ResourceLocation("minecraft", path);
            boolean found = false;
            for (IResourcePack pack : packs) {
                if (pack.resourceExists(location)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /** Every {@code "model"} a vanilla-format block state names, in its variants or multipart cases. */
    private static void collectModels(com.google.gson.JsonElement json, Set<String> models) {
        if (json.isJsonObject()) {
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : json.getAsJsonObject().entrySet()) {
                if ("model".equals(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
                    String name = entry.getValue().getAsString();
                    models.add(name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name);
                } else {
                    collectModels(entry.getValue(), models);
                }
            }
        } else if (json.isJsonArray()) {
            for (com.google.gson.JsonElement element : json.getAsJsonArray()) collectModels(element, models);
        }
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of("minecraft");
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return "Portium: modern resource pack support";
    }
}
