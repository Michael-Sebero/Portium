package com.michaelsebero.portium.client;

import net.minecraft.item.EnumDyeColor;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;

/**
 * Banner and shield textures painted from their pattern layers, for 1.12's {@code LayeredColorMaskTexture}, reading
 * patterns drawn either way:
 * <ul>
 * <li>1.14+'s (and so 1.16.5's, and modern packs'): the layer's own colour tinted by the dye, its alpha the pattern's
 * shape, as 1.16 tints its pattern sprites;</li>
 * <li>1.12's: opaque over the whole cloth, the red channel the pattern's shape, the dye tinting the cloth beneath.</li>
 * </ul>
 * 1.12 itself read only the second kind, and only from 32-bit RGBA files the size of the cloth, leaving any other layer
 * out. A layer of another size is scaled to the cloth here, as 1.16's atlas sprites stretch over the model.
 */
@SideOnly(Side.CLIENT)
public final class BannerLayers {

    /** As many layers as 1.12 and 1.16 draw: the base colour and 16 patterns. */
    public static final int MAX_LAYERS = 17;

    /** The cloth with each layer painted on in turn, as ARGB. */
    public static BufferedImage paint(BufferedImage cloth, LayerSource layers, int count) throws java.io.IOException {
        int width = cloth.getWidth();
        int height = cloth.getHeight();
        int[] clothPixels = cloth.getRGB(0, 0, width, height, null, 0, width);
        int[] out = clothPixels.clone();
        for (int i = 0; i < count && i < MAX_LAYERS; i++) {
            BufferedImage layer = layers.layer(i);
            if (layer == null) continue;
            int tint = layers.colour(i).getColorValue();
            int[] pixels = scaled(layer, width, height);
            if (masksWithAlpha(pixels)) {
                for (int p = 0; p < out.length; p++) {
                    int src = pixels[p];
                    if (src >>> 24 != 0) out[p] = over(src & 0xFF000000 | multiply(src, tint), out[p]);
                }
            } else {
                for (int p = 0; p < out.length; p++) {
                    int src = pixels[p];
                    if (src >>> 24 != 0) out[p] = over((src & 0xFF0000) << 8 | multiply(clothPixels[p], tint), out[p]);
                }
            }
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, out, 0, width);
        return image;
    }

    /**
     * Whether a layer is drawn the 1.14 way, its shape in its alpha: it fades in places, or it's shaded cloth that's
     * neither the full white nor the black of a 1.12 mask anywhere.
     */
    static boolean masksWithAlpha(int[] pixels) {
        boolean anyOpaque = false;
        boolean maskWhite = false;
        boolean maskBlack = false;
        for (int pixel : pixels) {
            int alpha = pixel >>> 24;
            if (alpha == 0) continue;
            if (alpha != 255) return true;
            anyOpaque = true;
            int red = pixel >> 16 & 255;
            if (red == 255) maskWhite = true;
            else if (red == 0) maskBlack = true;
        }
        return anyOpaque && !maskWhite && !maskBlack;
    }

    private static int[] scaled(BufferedImage layer, int width, int height) {
        int layerWidth = layer.getWidth();
        int layerHeight = layer.getHeight();
        int[] source = layer.getRGB(0, 0, layerWidth, layerHeight, null, 0, layerWidth);
        if (layerWidth == width && layerHeight == height) return source;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int row = y * layerHeight / height * layerWidth;
            for (int x = 0; x < width; x++) pixels[y * width + x] = source[row + x * layerWidth / width];
        }
        return pixels;
    }

    /** The RGB of two colours multiplied, channel by channel. */
    private static int multiply(int colour, int tint) {
        int red = (colour >> 16 & 255) * (tint >> 16 & 255) / 255;
        int green = (colour >> 8 & 255) * (tint >> 8 & 255) / 255;
        int blue = (colour & 255) * (tint & 255) / 255;
        return red << 16 | green << 8 | blue;
    }

    /** {@code src} blended over {@code dst} as GL's usual alpha blending does, both unpremultiplied ARGB. */
    private static int over(int src, int dst) {
        int srcAlpha = src >>> 24;
        if (srcAlpha == 255) return src;
        int dstAlpha = dst >>> 24;
        int outAlpha = srcAlpha + dstAlpha * (255 - srcAlpha) / 255;
        if (outAlpha == 0) return 0;
        int dstWeight = dstAlpha * (255 - srcAlpha) / 255;
        int red = ((src >> 16 & 255) * srcAlpha + (dst >> 16 & 255) * dstWeight) / outAlpha;
        int green = ((src >> 8 & 255) * srcAlpha + (dst >> 8 & 255) * dstWeight) / outAlpha;
        int blue = ((src & 255) * srcAlpha + (dst & 255) * dstWeight) / outAlpha;
        return outAlpha << 24 | red << 16 | green << 8 | blue;
    }

    /** Where {@link #paint} gets each layer's image and dye. */
    public interface LayerSource {
        BufferedImage layer(int index) throws java.io.IOException;

        EnumDyeColor colour(int index);
    }

    private BannerLayers() {}
}
