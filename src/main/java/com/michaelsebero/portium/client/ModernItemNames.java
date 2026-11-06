package com.michaelsebero.portium.client;

import com.google.common.collect.ImmutableSet;
import com.michaelsebero.portium.mixin.client.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Item models that 1.13 named after something else: 1.12's grass block item is {@code item/grass}, which 1.13 on is
 * the grass plant (so with a modern pack, 1.16.5's textures included, the grass block showed as grass). Above every
 * pack, this answers for those few names, as the right modern item, while the pack that would answer them is a modern
 * one; a 1.12 pack's own models are left alone.
 */
@SideOnly(Side.CLIENT)
public final class ModernItemNames implements IResourcePack {

    public static final ModernItemNames INSTANCE = new ModernItemNames();
    /** A model 1.13 on have and 1.12 doesn't, marking a pack as a modern one. */
    private static final ResourceLocation MODERN_MODEL = new ResourceLocation("minecraft", "models/block/grass_block.json");

    /** 1.12's item model by the 1.13 item model it is: by name under models/item. */
    private static final Map<String, String> RENAMED = new HashMap<>();
    /** 1.12 items whose 1.13 model's name is one of the above: their own model, written out. */
    private static final Map<String, String> WRITTEN = new HashMap<>();

    static {
        RENAMED.put("grass", "grass_block");
        RENAMED.put("melon", "melon_slice");
        RENAMED.put("nether_brick", "nether_bricks");
        RENAMED.put("snow", "snow_block");
        RENAMED.put("smooth_sandstone", "cut_sandstone");
        RENAMED.put("smooth_red_sandstone", "cut_red_sandstone");
        RENAMED.put("stone_slab", "smooth_stone_slab");
        RENAMED.put("pumpkin", "carved_pumpkin");
        RENAMED.put("stone_stairs", "cobblestone_stairs");
        // 1.12's tall grass is 1.13's grass, whose name the grass block's model takes over above.
        WRITTEN.put("tall_grass", "{\"parent\": \"minecraft:item/generated\", \"textures\": {\"layer0\": \"minecraft:block/grass\"}}");
    }

    private ModernItemNames() {}

    /** The model a name stands for here ("models/item/grass.json"), or null if it isn't one of these. */
    @Nullable
    private static String answer(String path) {
        if (!path.startsWith("models/item/") || !path.endsWith(".json")) return null;
        String name = path.substring("models/item/".length(), path.length() - ".json".length());
        String renamed = RENAMED.get(name);
        if (renamed != null) return "{\"parent\": \"minecraft:item/" + renamed + "\"}";
        return WRITTEN.get(name);
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (!"minecraft".equals(location.getNamespace()) || answer(location.getPath()) == null) return false;
        IResourcePack provider = topProvider(location);
        if (provider == null || !provider.resourceExists(MODERN_MODEL)) return false;
        String name = location.getPath().substring("models/item/".length(), location.getPath().length() - ".json".length());
        String renamed = RENAMED.get(name);
        // Only where a pack has the modern model the answer names.
        return renamed == null || topProvider(new ResourceLocation("minecraft", "models/item/" + renamed + ".json")) != null;
    }

    /** The highest pack below this one with the file: the player's packs from the top, then the game's own. */
    @Nullable
    private static IResourcePack topProvider(ResourceLocation location) {
        Minecraft minecraft = Minecraft.getMinecraft();
        List<IResourcePack> packs = new ArrayList<>(((MinecraftAccessor) minecraft).portium$getDefaultResourcePacks());
        for (ResourcePackRepository.Entry entry : minecraft.getResourcePackRepository().getRepositoryEntries()) packs.add(entry.getResourcePack());
        for (int i = packs.size() - 1; i >= 0; i--) {
            IResourcePack pack = packs.get(i);
            if (pack != INSTANCE && pack.resourceExists(location)) return pack;
        }
        return null;
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        String answer = resourceExists(location) ? answer(location.getPath()) : null;
        if (answer == null) throw new FileNotFoundException(location.toString());
        return new ByteArrayInputStream(answer.getBytes(StandardCharsets.UTF_8));
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
        return "Portium: modern item names";
    }
}
