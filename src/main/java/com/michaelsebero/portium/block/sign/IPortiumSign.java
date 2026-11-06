package com.michaelsebero.portium.block.sign;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/** A sign of a newer wood (1.14's and 1.16's per-wood signs): its texture as drawn, and the item it drops. */
public interface IPortiumSign {

    ResourceLocation getSignTexture();

    Item getSignItem();
}
