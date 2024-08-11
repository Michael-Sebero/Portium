package com.michaelsebero.portium.client;

import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blocks' render layers as 1.16 mods set them ({@code RenderTypeLookup.setRenderLayer}); 1.12 asks the block, so a
 * mixin on {@code Block.getRenderLayer} answers from here (see {@code MixinBlockRenderLayer}).
 */
public final class RenderLayers116 {

    private static final Map<Block, BlockRenderLayer> LAYERS = new ConcurrentHashMap<>();

    /** Typed loosely for the glue, which compiles against 1.16: the block, and the layer's name. */
    public static void set(Object block, String layer) {
        LAYERS.put((Block) block, BlockRenderLayer.valueOf(layer));
    }

    public static BlockRenderLayer get(Block block) {
        return LAYERS.get(block);
    }

    /** The name of the one layer 1.12 draws a block in: a 1.16 mod's choice, or the block's own. Loosely typed, for the glue. */
    public static String layerName(Object block) {
        BlockRenderLayer layer = LAYERS.get((Block) block);
        return (layer != null ? layer : ((Block) block).getRenderLayer()).name();
    }

    private RenderLayers116() {}
}
