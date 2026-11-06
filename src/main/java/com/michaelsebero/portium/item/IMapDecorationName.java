package com.michaelsebero.portium.item;

import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/** A map marker's name, drawn under it (1.13: named banners), on {@link net.minecraft.world.storage.MapDecoration}. */
public interface IMapDecorationName {

    @Nullable
    ITextComponent portium$getName();

    void portium$setName(@Nullable ITextComponent name);
}
