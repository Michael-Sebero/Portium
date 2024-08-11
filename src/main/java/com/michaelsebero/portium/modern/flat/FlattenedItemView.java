package com.michaelsebero.portium.modern.flat;

import net.minecraft.item.Item;

/** A flattened item (see {@link Flattening}): the 1.12 item and metadata it stands for. */
public interface FlattenedItemView {

    Item base();

    int meta();
}
