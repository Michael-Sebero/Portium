package com.michaelsebero.portium.modern.mc.util;

import com.michaelsebero.portium.modern.Srg;
import net.minecraft.item.Item;

/** 1.13's {@code net.minecraft.util.IItemProvider}: anything with an item form. 1.12's items and blocks implement it. */
public interface IItemProvider {

    @Srg("func_199767_j")
    Item asItem();
}
