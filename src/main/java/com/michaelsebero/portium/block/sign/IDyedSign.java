package com.michaelsebero.portium.block.sign;

import net.minecraft.item.EnumDyeColor;

/** A sign's text colour (1.14: dyes colour a sign's text), on 1.12's {@code TileEntitySign}. */
public interface IDyedSign {

    /** The 1.14 text colour of each dye, by 1.12's colour index ({@code DyeColor.textColor}). */
    int[] TEXT_COLORS = {16777215, 16738335, 16711935, 10141901, 16776960, 12582656, 16738740, 8421504, 13882323, 65535, 10494192, 255, 10506797,
            65280, 16711680, 0};

    EnumDyeColor portium$getColor();

    /** Sets the colour; false if it already was. */
    boolean portium$setColor(EnumDyeColor color);

    /** The colour 1.14 draws the text in: the dye's text colour at 40%. */
    static int renderColor(EnumDyeColor color) {
        int rgb = TEXT_COLORS[color.getMetadata()];
        int red = (int) ((rgb >> 16 & 255) * 0.4D);
        int green = (int) ((rgb >> 8 & 255) * 0.4D);
        int blue = (int) ((rgb & 255) * 0.4D);
        return red << 16 | green << 8 | blue;
    }
}
