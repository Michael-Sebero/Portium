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
    @Nullable
    private static ModernPackSupport instance;

    /** Whether an enabled pack has 1.13+ block models, which give some of 1.12's model names other meanings. */
    public static boolean modernModels() {
        return instance != null && instance.active();
    }

    public static void install(Minecraft minecraft) {
        instance = new ModernPackSupport();
        ((MinecraftAccessor) minecraft).portium$getDefaultResourcePacks().add(instance);
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

    /**
     * Models that 1.17 to 1.19 renamed, which ResourceFix (made for 1.19.2's assets) names, by the 1.16.5 model each
     * is the same as: served as a model with that parent when a modern pack is on, so the 1.16.5 assets Portium
     * imports stand in for 1.19.2's (doors, paths and cauldrons otherwise had no model). A pack's own copy wins.
     */
    private static final java.util.Map<String, String> RENAMED_MODELS = new java.util.HashMap<>();

    static {
        for (String door : new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "iron"}) {
            RENAMED_MODELS.put("block/" + door + "_door_bottom_left", "block/" + door + "_door_bottom");
            RENAMED_MODELS.put("block/" + door + "_door_bottom_right", "block/" + door + "_door_bottom_hinge");
            RENAMED_MODELS.put("block/" + door + "_door_top_left", "block/" + door + "_door_top");
            RENAMED_MODELS.put("block/" + door + "_door_top_right", "block/" + door + "_door_top_hinge");
        }
        RENAMED_MODELS.put("block/dirt_path", "block/grass_path");
        RENAMED_MODELS.put("block/water_cauldron_level1", "block/cauldron_level1");
        RENAMED_MODELS.put("block/water_cauldron_level2", "block/cauldron_level2");
        RENAMED_MODELS.put("block/water_cauldron_full", "block/cauldron_level3");
    }

    /**
     * 1.12 texture names Portium's own models use (its stairs, slabs, walls, nylium...), by the 1.13+ name of the same
     * texture. While a modern pack is on, 1.12's blocks show its textures (through its models), so these are served
     * from it too, or Portium's blocks would keep 1.12's art beside them. Water is left out: 1.13's is grey and tinted.
     */
    private static final java.util.Map<String, String> RENAMED_TEXTURES = new java.util.HashMap<>();

    static {
        String[] pairs = {"brick", "bricks", "cobblestone", "cobblestone", "cobblestone_mossy", "mossy_cobblestone", "dirt", "dirt",
                "end_bricks", "end_stone_bricks", "flower_pot", "flower_pot", "log_acacia", "acacia_log", "log_big_oak", "dark_oak_log",
                "log_birch", "birch_log", "log_jungle", "jungle_log", "log_oak", "oak_log", "log_spruce", "spruce_log",
                "log_acacia_top", "acacia_log_top", "log_big_oak_top", "dark_oak_log_top", "log_birch_top", "birch_log_top",
                "log_jungle_top", "jungle_log_top", "log_oak_top", "oak_log_top", "log_spruce_top", "spruce_log_top",
                "mushroom_block_inside", "mushroom_block_inside", "mushroom_block_skin_brown", "brown_mushroom_block",
                "mushroom_block_skin_red", "red_mushroom_block", "mushroom_block_skin_stem", "mushroom_stem", "nether_brick", "nether_bricks",
                "netherrack", "netherrack", "planks_acacia", "acacia_planks", "planks_big_oak", "dark_oak_planks", "planks_birch", "birch_planks",
                "planks_jungle", "jungle_planks", "planks_oak", "oak_planks", "planks_spruce", "spruce_planks", "prismarine_bricks",
                "prismarine_bricks", "prismarine_dark", "dark_prismarine", "prismarine_rough", "prismarine", "pumpkin_side", "pumpkin_side",
                "pumpkin_top", "pumpkin_top", "quartz_block_bottom", "quartz_block_bottom", "red_nether_brick", "red_nether_bricks",
                "red_sandstone_normal", "red_sandstone", "red_sandstone_smooth", "cut_red_sandstone", "red_sandstone_top", "red_sandstone_top",
                "sandstone_normal", "sandstone", "sandstone_smooth", "cut_sandstone", "sandstone_top", "sandstone_top", "stone", "stone",
                "stone_andesite", "andesite", "stone_andesite_smooth", "polished_andesite", "stonebrick", "stone_bricks", "stonebrick_mossy",
                "mossy_stone_bricks", "stone_diorite", "diorite", "stone_diorite_smooth", "polished_diorite", "stone_granite", "granite",
                "stone_granite_smooth", "polished_granite", "stone_slab_top", "smooth_stone"};
        for (int i = 0; i < pairs.length; i += 2) RENAMED_TEXTURES.put(pairs[i], pairs[i + 1]);
    }

    /** "textures/block/granite.png" for "textures/blocks/stone_granite.png" (or its .mcmeta), or null. */
    @Nullable
    private static String modernTexture(String path) {
        if (!path.startsWith("textures/blocks/")) return null;
        String rest = path.substring("textures/blocks/".length());
        String suffix = rest.endsWith(".png.mcmeta") ? ".png.mcmeta" : rest.endsWith(".png") ? ".png" : null;
        if (suffix == null) return null;
        String modern = RENAMED_TEXTURES.get(rest.substring(0, rest.length() - suffix.length()));
        return modern == null ? null : "textures/block/" + modern + suffix;
    }

    /** The topmost enabled pack with a 1.13+ texture (not this one), or null. */
    @Nullable
    private IResourcePack texturePack(String modernPath) {
        ResourceLocation location = new ResourceLocation("minecraft", modernPath);
        List<ResourcePackRepository.Entry> enabled = Minecraft.getMinecraft().getResourcePackRepository().getRepositoryEntries();
        for (int i = enabled.size() - 1; i >= 0; i--) {
            IResourcePack pack = enabled.get(i).getResourcePack();
            if (pack != this && pack.resourceExists(location)) return pack;
        }
        return null;
    }

    /** The 1.16.5 model a renamed model stands for, if the path is one ("models/block/dirt_path.json"). */
    @Nullable
    private static String renamedModel(String path) {
        if (!path.startsWith("models/") || !path.endsWith(".json")) return null;
        return RENAMED_MODELS.get(path.substring("models/".length(), path.length() - ".json".length()));
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        if (!resourceExists(location)) throw new FileNotFoundException(location.toString());
        String texture = modernTexture(location.getPath());
        if (texture != null) {
            IResourcePack pack = this.texturePack(texture);
            if (pack == null) throw new FileNotFoundException(location.toString());
            return pack.getInputStream(new ResourceLocation("minecraft", texture));
        }
        String renamed = renamedModel(location.getPath());
        if (renamed != null) {
            return new java.io.ByteArrayInputStream(("{\"parent\": \"minecraft:" + renamed + "\"}").getBytes(StandardCharsets.UTF_8));
        }
        InputStream in = ModernPackSupport.class.getClassLoader().getResourceAsStream(ROOT + "assets/minecraft/" + location.getPath());
        if (in == null) throw new FileNotFoundException(location.toString());
        return in;
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!"minecraft".equals(location.getNamespace())) return false;
        if (renamedModel(location.getPath()) != null) return active();
        String texture = modernTexture(location.getPath());
        if (texture != null) return active() && this.texturePack(texture) != null;
        if (!this.files.contains(location.getPath()) || !active()) return false;
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
            String path = "models/" + (model.contains("/") ? model : "block/" + model) + ".json";
            if (this.files.contains(path)) continue;
            // A renamed model is there when the model it stands for is.
            String renamed = renamedModel(path);
            if (renamed != null) path = "models/" + renamed + ".json";
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
