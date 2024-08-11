package com.michaelsebero.portium.modern.adapter;

import com.michaelsebero.portium.modern.mc.item.Item$Properties;

/** Implemented on 1.12's items by mixin: takes the properties a 1.13 item was made with. */
public interface IItemPropertyHolder {

    void portium$applyItemProperties(Item$Properties properties);
}
