package com.michaelsebero.portium.block;

import net.minecraft.block.properties.PropertyInteger;

/**
 * A block that holds several copies of its item, like sea pickles and turtle eggs. Using the item on it adds one.
 */
public interface IStackableBlock {

    PropertyInteger getCountProperty();

    default int getMaxCount() {
        int max = 0;
        for (Integer value : this.getCountProperty().getAllowedValues()) max = Math.max(max, value);
        return max;
    }
}
