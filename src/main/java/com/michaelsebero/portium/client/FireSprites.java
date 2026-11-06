package com.michaelsebero.portium.client;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The fire drawn over a burning player's view and around burning entities. 1.12 asks the block atlas for
 * {@code blocks/fire_layer_0/1} by name, sprites that are only stitched because 1.12's fire models use them; a modern
 * pack's fire models (1.14 on, the 1.16.5 textures Portium imports) use {@code block/fire_0/1} instead, which left the
 * overlays drawing the missing texture (a magenta screen while burning or in lava). Portium stitches 1.12's always,
 * and the overlays take the modern sprites when a pack's fire models brought them, so they match the fire blocks.
 */
@SideOnly(Side.CLIENT)
public final class FireSprites {

    private static final String LEGACY = "minecraft:blocks/fire_layer_";
    private static final String MODERN = "minecraft:block/fire_";

    /** For TextureStitchEvent.Pre: 1.12's two fire sprites, whatever the fire models name. */
    public static void register(TextureMap map) {
        map.registerSprite(new ResourceLocation("minecraft", "blocks/fire_layer_0"));
        map.registerSprite(new ResourceLocation("minecraft", "blocks/fire_layer_1"));
    }

    /** What the overlays draw for a sprite 1.12 names: a fire layer's modern sprite if stitched, else as asked. */
    public static TextureAtlasSprite sprite(TextureMap map, String name) {
        if (name.startsWith(LEGACY)) {
            TextureAtlasSprite modern = map.getAtlasSprite(MODERN + name.substring(LEGACY.length()));
            if (modern != map.getMissingSprite()) return modern;
        }
        return map.getAtlasSprite(name);
    }

    private FireSprites() {}
}
