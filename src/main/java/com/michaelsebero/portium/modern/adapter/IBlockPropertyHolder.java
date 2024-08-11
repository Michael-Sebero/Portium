package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.mc.block.Block$Properties;

/** Implemented on 1.12's blocks by mixin: takes the properties a 1.13 block was made with. */
public interface IBlockPropertyHolder {

    void portium$applyBlockProperties(Block$Properties properties);
}
